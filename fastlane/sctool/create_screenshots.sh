#!/usr/bin/env bash

# Creates fastlane screenshots for all supported locales.
# Requires "pngquant" and "oxipng" to be installed.
#
# App setup (intro skip, Mecca location, Umm Al-Qura method, prayer
# schedule, notification widget) plus permissions and date/time are applied
# automatically — requires a DEBUG build (the setup broadcast receiver and the
# screen deep links only exist there) and an emulator (adb root for setting the
# date). No manual prep is required.
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
#
# Regenerate a subset with LOCALES / SCREENS (space-separated):
#   LOCALES="id fa" ./create_screenshots.sh
#   SCREENS="main-light main-dark" ./create_screenshots.sh   # main screen only
#   Screen keys: intro main-light main-dark interface schedule-muezzin
#                qada-counter qibla-compass notification-widget

export MSYS_NO_PATHCONV=1

APP_ID="${APP_ID:-com.github.meypod.al_azan}"

# APK used when the app is missing from the device. Defaults to the debug build,
# which the screenshot flow needs anyway (setup receiver + deep links). Override
# with APK_PATH for a different artifact.
APK_PATH="${APK_PATH:-../../app/build/outputs/apk/debug/app-debug.apk}"

# Install the app if it isn't on the device yet. -g grants runtime permissions.
function ensure_app_installed {
    if adb shell pm path "$APP_ID" >/dev/null 2>&1; then
        return
    fi
    echo "App '$APP_ID' not installed; installing $APK_PATH"
    if [ ! -f "$APK_PATH" ]; then
        echo "APK not found at '$APK_PATH'. Build it first (./gradlew :app:assembleDebug) or set APK_PATH."
        exit 1
    fi
    adb install -r -g "$APK_PATH" || exit 1
}

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

# Clear the onboarding flags so a cold start lands back on the intro
# (language-selection) screen for the first screenshot.
function reset_intro {
    adb shell am broadcast -a com.github.meypod.al_azan.action.RESET_INTRO \
        -n "$APP_ID/com.github.meypod.al_azan.ScreenshotSetupReceiver" >/dev/null
    sleep 2
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

# Cold-start the app so it picks up the reset onboarding flags and shows the intro.
function goto_intro {
    adb shell am force-stop "$APP_ID"
    adb shell am start -n "$APP_ID/com.github.meypod.al_azan.MainActivity" >/dev/null 2>&1
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

rm -f "./screen-tmp-${ANDROID_SERIAL}.png"

# Override with e.g. LOCALES="id fa" to regenerate a subset.
if [ -n "$LOCALES" ]; then
    read -r -a locales <<< "$LOCALES"
else
    locales=('en-US' 'ar' 'bn' 'bs' 'de' 'fa' 'fr' 'hi' 'id' 'sw' 'tr' 'ur' 'vi')
fi

# Override with e.g. SCREENS="main-light main-dark" to regenerate a subset of
# screens. Keys: intro main-light main-dark interface schedule-muezzin
# qada-counter qibla-compass notification-widget.
# Empty = all screens.
if [ -n "$SCREENS" ]; then
    read -r -a screens <<< "$SCREENS"
fi

# want <screen-key> -> true when the screen should be captured this run.
function want {
    [ -z "$SCREENS" ] && return 0
    local s
    for s in "${screens[@]}"; do
        [ "$s" == "$1" ] && return 0
    done
    return 1
}

# Metadata images dir for a locale, by run target (phone / tablet7 / tablet10).
function scr_dir_for {
    if [ "$1" == 'tablet7' ]; then
        echo "../metadata/android/$2/images/sevenInchScreenshots"
    elif [ "$1" == 'tablet10' ]; then
        echo "../metadata/android/$2/images/tenInchScreenshots"
    else
        echo "../metadata/android/$2/images/phoneScreenshots"
    fi
}

ensure_app_installed
setup_app
start_clean_status_bar
dark_mode_disable

# Intro (language-selection) pass — phone only, captured before the app is
# onboarded so it shows the very first screen the user sees. Reset once: the
# flag stays cleared across locale changes until setup_app re-applies the preset.
if [ -z "$1" ] && want intro; then
    reset_intro
    for i in "${locales[@]}"
    do
        echo "=== $i (intro) ==="
        scrDir="$(scr_dir_for "$1" "$i")"
        mkdir -p "$scrDir"

        change_app_lang "$i"
        sleep 2 # app restarts after locale change
        goto_intro
        sleep 4 # wait for cold start
        save_screenshot "$scrDir/1-intro.png"
    done
    setup_app # restore the onboarded preset for the remaining screens
fi

snooze_system_notifications

for i in "${locales[@]}"
do
    echo "=== $i ==="
    scrDir="$(scr_dir_for "$1" "$i")"
    mkdir -p "$scrDir"

    change_app_lang "$i"
    sleep 2 # app restarts after locale change

    if want main-light; then
        navigate Home
        sleep 4 # wait for activity start + widget/notification re-render
        # tablets get a single main-light shot; phone numbers it after the intro.
        if [ -n "$1" ]; then
            save_screenshot "$scrDir/1-main-light.png"
        else
            save_screenshot "$scrDir/2-main-light.png"
        fi
    fi

    # tablets only get the main screen shot
    if [ -n "$1" ]; then
        continue
    fi

    if want main-dark; then
        navigate Home
        sleep 2
        dark_mode_enable
        sleep 3
        save_screenshot "$scrDir/3-main-dark.png"
        dark_mode_disable
        sleep 3
    fi

    if want interface; then
        navigate InterfaceSettings
        sleep 2
        save_screenshot "$scrDir/4-interface-light.png"
    fi

    if want schedule-muezzin; then
        navigate ScheduleAndMuezzin
        sleep 2
        save_screenshot "$scrDir/5-schedule-muezzin-light.png"
    fi

    if want qada-counter; then
        navigate Counter
        sleep 2
        save_screenshot "$scrDir/6-qada-counter-light.png"
    fi

    if want qibla-compass; then
        navigate QiblaCompass
        sleep 2.5
        save_screenshot "$scrDir/7-qibla-compass-light.png"
    fi

    if want notification-widget; then
        # Re-snooze right before capture: system notices (e.g. "Configure
        # physical keyboards") re-post after the up-front pass and would
        # otherwise land in the shade.
        snooze_system_notifications
        expand_status_bar
        sleep 1.5
        save_screenshot "$scrDir/8-notification-widget-light.png"
        collapse_status_bar
        sleep 1
    fi

    navigate Home
    sleep 1
done

reset_app_lang
stop_clean_status_bar
