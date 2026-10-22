package com.rakshyaa.rakshyaa.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** Shown before location consent, including when Android permission already exists. */
@Composable
fun LocationDisclosureDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(com.rakshyaa.rakshyaa.R.string.location_disclosure_title)) },
        text = { Text(stringResource(com.rakshyaa.rakshyaa.R.string.location_disclosure_message)) },
        confirmButton = {
            TextButton(onClick = onAgree) {
                Text(stringResource(com.rakshyaa.rakshyaa.R.string.location_disclosure_agree))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(com.rakshyaa.rakshyaa.R.string.location_disclosure_decline))
            }
        }
    )
}
