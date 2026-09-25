package com.rakshyaa.rakshyaa.ui.components

import android.content.Intent
import android.net.Uri

/** Composes only. Android's messaging app requires the user to confirm sending. */
fun emergencyMessageIntent(phones: List<String>, message: String): Intent {
    val recipients = phones.map { it.replace(Regex("[^+0-9]"), "") }
        .filter { it.matches(Regex("\\+?[0-9]{7,15}")) }.distinct().take(5)
    return Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(recipients.joinToString(";"))))
        .putExtra("sms_body", message)
}

fun emergencyDialIntent(): Intent = Intent(Intent.ACTION_DIAL)
