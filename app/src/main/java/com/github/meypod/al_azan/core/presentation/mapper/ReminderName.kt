package com.github.meypod.al_azan.core.presentation.mapper

import android.content.res.Resources
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder

/**
 * Display name for a reminder: the user's label, or "N minutes before/after <prayer>" when blank.
 * Single source of truth so notifications, toasts, and the reschedule snackbar stay consistent.
 */
fun reminderDisplayName(
    resources: Resources,
    label: String,
    duration: Int,
    durationModifier: Int,
    prayer: Prayer,
): String =
    label.ifBlank {
        val plural = if (durationModifier >= 0) R.plurals.reminder_minutes_after else R.plurals.reminder_minutes_before
        resources.getQuantityString(plural, duration, duration, resources.getString(prayer.stringRes))
    }

fun Reminder.displayName(resources: Resources): String = reminderDisplayName(resources, label, duration, durationModifier, prayer)
