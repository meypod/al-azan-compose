package com.github.meypod.al_azan.core.domain.model.notification

import androidx.compose.runtime.Immutable
import com.github.meypod.al_azan.core.domain.model.TextResource

@Immutable
data class NotificationChannelConfig(
    val id: String,
    /**
     * The user visible name of this channel
     */
    val name: TextResource,
    /**
     * The user visible description of this channel.
     * The recommended maximum length is 300 characters; the value may be truncated if it is too long
     */
    val description: TextResource,
    val importanceLevel: AndroidNotificationImportance = AndroidNotificationImportance.IMPORTANCE_DEFAULT,
    val showBadge: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val vibrationPattern: List<Long>? = null,
    /**
     * set `null` for default ringtone
     */
    val soundUri: String? = null,
    /** When true the channel is allowed to interrupt Do Not Disturb. */
    val canBypassDnd: Boolean = false,
    /**
     * When true the channel itself stays audibly silent because the audio is produced elsewhere (the
     * foreground playback service), e.g. for the adhan. Importance is still kept high for heads-up.
     */
    val soundHandledExternally: Boolean = false,
)
