package at.mcbabo.authenticator.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.data.store.GestureType
import at.mcbabo.authenticator.internal.crypto.AuthType
import at.mcbabo.authenticator.ui.viewmodel.OtpAccountWithCode

@Composable
fun OTPAccountItem(
    modifier: Modifier,
    account: OtpAccountWithCode,
    gestureType: GestureType,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onHOTPClick: () -> Unit = {}
) {
    val gestureModifier = when (gestureType) {
        GestureType.LONG_PRESS_TO_COPY -> Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )

        GestureType.TAP_TO_COPY -> Modifier
            .combinedClickable(
                onClick = onLongClick,
                onLongClick = onClick
            )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(gestureModifier)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row {
                Text(
                    text = "${account.account.issuer.ifEmpty { stringResource(R.string.no_issuer) }}: ",
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                )
                Text(
                    text = account.account.accountName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = account.currentCode.toString().chunked(account.currentCode?.length?.div(2) ?: 32)
                    .joinToString(" "),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }

        when (account.account.type) {
            AuthType.TOTP -> {
                CountdownPieChart(
                    current = account.remainingSeconds,
                    total = account.account.period,
                    modifier = Modifier.padding(end = 12.dp),
                    size = 24.dp
                )
            }

            AuthType.HOTP -> {
                IconButton(
                    onClick = { onHOTPClick() }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Increment HOTP",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
