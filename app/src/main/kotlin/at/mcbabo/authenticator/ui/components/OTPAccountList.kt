package at.mcbabo.authenticator.ui.components

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.mcbabo.authenticator.ui.viewmodel.OtpAccountWithCode

@Composable
fun OTPAccountList(
    accounts: List<OtpAccountWithCode>,
    listPosition: String,
    listState: LazyListState,
    onClick: (OtpAccountWithCode) -> Unit,
    onLongClick: (String?) -> Unit
) {
    LazyColumn(
        state = listState
    ) {
        items(
            items = accounts,
            key = { "$listPosition-${it.account.id}" }
        ) { accountWithCode ->
            OTPAccountItem(
                modifier = Modifier,
                account = accountWithCode,
                onClick = {
                    onClick(accountWithCode)
                },
                onLongClick = {
                    onLongClick(accountWithCode.currentCode)
                }
            )
            HorizontalDivider()
        }
    }
}
