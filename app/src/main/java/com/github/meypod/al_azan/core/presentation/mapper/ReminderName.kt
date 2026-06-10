package com.github.meypod.al_azan.core.presentation.mapper

import android.content.res.Resources
import com.github.meypod.al_azan.R
import com.github.meypod.al_azan.core.domain.model.adhan.Prayer
import com.github.meypod.al_azan.core.domain.model.reminder.Reminder

/**
 * "N minutes before/after <prayer>" with locale-correct word order.
 * Single source of truth so the reminder list, notifications, toasts, and the
 * reschedule snackbar stay consistent.
 */
fun reminderDurationTitle(
    resources: Resources,
    duration: Int,
    durationModifier: Int,
    prayer: Prayer,
): String {
    val plural = if (durationModifier >= 0) R.plurals.reminder_minutes_after else R.plurals.reminder_minutes_before
    return resources.getQuantityString(plural, duration, duration, resources.getString(prayer.stringRes))
}

/** Display name for a reminder: the user's label, or [reminderDurationTitle] when blank. */
fun reminderDisplayName(
    resources: Resources,
    label: String,
    duration: Int,
    durationModifier: Int,
    prayer: Prayer,
): String = label.ifBlank { reminderDurationTitle(resources, duration, durationModifier, prayer) }

fun Reminder.displayName(resources: Resources): String = reminderDisplayName(resources, label, duration, durationModifier, prayer)
