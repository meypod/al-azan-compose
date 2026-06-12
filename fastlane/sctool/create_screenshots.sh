#!/usr/bin/env bash

# Creates fastlane screenshots for all supported locales.
# Requires "pngquant" and "oxipng" to be installed.
#
# App setup (intro skip, Mecca location, Muslim World League method, prayer
# schedule, example reminder, notification widget) plus permissions and
# date/time are applied automatically — requires a DEBUG build (the setup
# broadcast receiver and the screen deep links only exist there) and an
# emulator (adb root for setting the date).
#
# Remaining one-time manual prep:
#   - Place the Al-Azan widget on the launcher home screen (phone runs only).
#
# Usage (output goes to the fastlane metadata images dir for each locale):
#   ./create_screenshots.sh           # phone, all screens   -> images/phoneScreenshots
#   ./create_screenshots.sh tablet7   # main screen only     -> images/sevenInchScreenshots
#   ./create_screenshots.sh tablet10  # main screen only     -> images/tenInchScreenshots
#   APP_ID=com.github.meypod.al_azan.debug ./create_screenshots.sh   # debug build
#
# Pin a device with ANDROID_SERIAL so multiple emulators can run in parallel:
#   ANDROID_SERIAL=emulator-5554 ./create_screenshots.sh &
#   ANDROID_SERIAL=emulator-5556 ./create_screenshots.sh tablet7 &

export MSYS_NO_PATHCONV=1

APP_ID="${APP_ID:-com.github.meypod.al_azan}"

function start_clean_status_bar {
    adb shell settings put global sysui_demo_allowed 1

    # Display time 11:00
    adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 1100 >/dev/null
    # Full wifi, no mobile clutter
    adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true >/dev/null
    adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide >/dev/null
    # Hide notification icons
    adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false >/dev/null
    # Full battery, not charging
    adb shell am broadcast -a com.android.systemui.demo -e command battery -e plugged false -e level 100 >/dev/null
    # Hide the "alarm set" status icon
    adb shell am broadcast -a com.android.systemui.demo -e command status -e alarm hide >/dev/null
}

# One-shot app configuration through the debug-only ScreenshotSetupReceiver,
# plus the permissions and clock state the app cannot set for itself.
function setup_app {
    # runtime permissions the intro would normally request
    adb shell pm grant "$APP_ID" android.permission.POST_NOTIFICATIONS 2>/dev/null
    adb shell pm grant "$APP_ID" android.permission.READ_PHONE_STATE 2>/dev/null
    adb shell appops set "$APP_ID" SCHEDULE_EXACT_ALARM allow 2>/dev/null

    # fixed clock: Mecca timezone, June 12 2026 11:00 (requires adb root → emulator)
    adb root >/dev/null 2>&1
    sleep 2
    adb wait-for-device
    adb shell service call alarm 3 s16 Asia/Riyadh >/dev/null
    adb shell settings put global auto_time 0
    adb shell date 061211002026 >/dev/null

    # apply the screenshot preset inside the app
    adb shell am broadcast -a com.github.meypod.al_azan.action.SETUP_SCREENSHOTS \
        -n "$APP_ID/com.github.meypod.al_azan.ScreenshotSetupReceiver" >/dev/null
    sleep 2
}

# Snooze everything that isn't ours (e.g. "Android System" debug notices) so the
# notification shade screenshot stays clean.
function snooze_system_notifications {
    adb shell cmd notification list | tr -d '\r' | grep -v "|$APP_ID|" | while read -r key; do
        [ -n "$key" ] && adb shell "cmd notification snooze --for 86400000 '$key'" >/dev/null 2>&1
    done
}

function stop_clean_status_bar {
    adb shell am broadcast -a com.android.systemui.demo -e command exit >/dev/null
}

function navigate {
    adb shell am start -n "$APP_ID/com.github.meypod.al_azan.MainActivity" \
        -a android.intent.action.VIEW -d "al-azan://$1" \
        --activity-single-top --activity-clear-top >/dev/null 2>&1
}

function change_app_lang {
    adb shell cmd locale set-app-locales "$APP_ID" --user current --locales "$1"
}

function reset_app_lang {
    adb shell cmd locale set-app-locales "$APP_ID" --user current --locales ""
}

function expand_status_bar {
    adb shell service call statusbar 1 >/dev/null
}

function collapse_status_bar {
    adb shell service call statusbar 2 >/dev/null
}

# A HOME keyevent switches the launcher into key-navigation mode, which draws a focus
# ring around the widget; launch the home intent instead and tap an empty wallpaper
# spot to make sure the launcher is in touch mode with nothing focused.
function goto_home {
    adb shell am start -a android.intent.action.MAIN -c android.intent.category.HOME >/dev/null 2>&1
    sleep 1
    adb shell input tap "$DEFOCUS_X" "$DEFOCUS_Y"
}

function dark_mode_enable {
    adb shell cmd uimode night yes
}

function dark_mode_disable {
    adb shell cmd uimode night no
}

function save_screenshot {
    local tmp="./screen-tmp-${ANDROID_SERIAL}.png"
    adb exec-out screencap -p -d "$DISPLAY_ID" > "$tmp"

    pngquant --strip --skip-if-larger --force --quality 85-99 "$tmp" -o "$tmp"
    oxipng --strip safe "$tmp" --out "$tmp"

    mv "$tmp" "$1"
}

function select_adb_device {
    if [ -n "$ANDROID_SERIAL" ]; then
        if ! adb devices | grep -q "^$ANDROID_SERIAL[[:space:]]*device$"; then
            echo "Device '$ANDROID_SERIAL' (from ANDROID_SERIAL) is not connected."
            exit 1
        fi
        echo "Using device: $ANDROID_SERIAL (from ANDROID_SERIAL)"
        return
    fi
    devices=($(adb devices | awk 'NR>1 && $2=="device" {print $1}'))
    count=${#devices[@]}
    if [ $count -eq 0 ]; then
        echo "No adb devices found."
        exit 1
    elif [ $count -eq 1 ]; then
        export ANDROID_SERIAL="${devices[0]}"
        echo "Using device: ${devices[0]}"
    else
        echo "Multiple adb devices found:"
        for i in "${!devices[@]}"; do
            echo "$((i+1)). ${devices[$i]}"
        done
        read -p "Select device [1-$count]: " idx
        idx=$((idx-1))
        if [ $idx -ge 0 ] && [ $idx -lt $count ]; then
            export ANDROID_SERIAL="${devices[$idx]}"
            echo "Using device: ${devices[$idx]}"
        else
            echo "Invalid selection."
            exit 1
        fi
    fi
}

select_adb_device

# The resizable emulator exposes two physical displays; screencap needs an explicit id.
DISPLAY_ID="$(adb shell dumpsys SurfaceFlinger --display-id | awk 'NR==1{print $2}')"

# Empty wallpaper spot used to defocus the launcher (see goto_home): mid-width,
# ~65% height lands between the widget at the top and the dock at the bottom.
read -r SCREEN_W SCREEN_H < <(adb shell wm size | awk -F'[ x]' '/Physical/{print $(NF-1), $NF}')
DEFOCUS_X=$((SCREEN_W / 2))
DEFOCUS_Y=$((SCREEN_H * 65 / 100))

rm -f "./screen-tmp-${ANDROID_SERIAL}.png"

# Override with e.g. LOCALES="id fa" to regenerate a subset.
if [ -n "$LOCALES" ]; then
    read -r -a locales <<< "$LOCALES"
else
    locales=('en-US' 'ar' 'bn' 'bs' 'de' 'fa' 'fr' 'hi' 'id' 'sw' 'tr' 'ur' 'vi')
fi

setup_app
start_clean_status_bar
snooze_system_notifications
dark_mode_disable

for i in "${locales[@]}"
do
    echo "=== $i ==="

    if [ "$1" == 'tablet7' ]; then
        scrDir="../metadata/android/$i/images/sevenInchScreenshots"
    elif [ "$1" == 'tablet10' ]; then
        scrDir="../metadata/android/$i/images/tenInchScreenshots"
    else
        scrDir="../metadata/android/$i/images/phoneScreenshots"
    fi

    mkdir -p "$scrDir"

    change_app_lang "$i"
    sleep 2 # app restarts after locale change

    navigate Home
    sleep 4 # wait for activity start + widget/notification re-render
    save_screenshot "$scrDir/1-main-light.png"

    # tablets only get the main screen shot
    if [ -n "$1" ]; then
        continue
    fi

    dark_mode_enable
    sleep 3
    save_screenshot "$scrDir/2-main-dark.png"
    dark_mode_disable
    sleep 3

    navigate InterfaceSettings
    sleep 2
    save_screenshot "$scrDir/3-interface-light.png"

    navigate ScheduleAndMuezzin
    sleep 2
    save_screenshot "$scrDir/4-schedule-muezzin-light.png"

    navigate Reminder
    sleep 2
    save_screenshot "$scrDir/5-reminders-light.png"

    navigate Counter
    sleep 2
    save_screenshot "$scrDir/6-qada-counter-light.png"

    navigate QiblaCompass
    sleep 2.5
    save_screenshot "$scrDir/7-qibla-compass-light.png"

    # widgets
    expand_status_bar
    sleep 1.5
    save_screenshot "$scrDir/8-notification-widget-light.png"
    collapse_status_bar
    sleep 1

    goto_home
    sleep 2
    save_screenshot "$scrDir/9-homescreen-widget-light.png"

    navigate Home
    sleep 1
done

reset_app_lang
stop_clean_status_bar
