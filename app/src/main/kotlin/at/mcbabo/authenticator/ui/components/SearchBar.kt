package at.mcbabo.authenticator.ui.components

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import at.mcbabo.authenticator.R
import at.mcbabo.authenticator.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchBarInput(
    searchBarState: SearchBarState,
    textFieldState: TextFieldState,
    onBack: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    SearchBarDefaults.InputField(
        modifier = Modifier,
        searchBarState = searchBarState,
        textFieldState = textFieldState,
        onSearch = { keyboardController?.hide() },
        placeholder = {
            if (searchBarState.currentValue == SearchBarValue.Collapsed) {
                Text(text = stringResource(R.string.search))
            }
        },
        leadingIcon = {
            if (searchBarState.currentValue == SearchBarValue.Expanded) {
                IconButton(
                    onClick = {
                        onBack()
                    }
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                    )
                }
            } else Icon(
                Icons.Outlined.Search,
                contentDescription = stringResource(R.string.search),
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun SearchBarInputPreview() {
    val searchBarState = rememberSearchBarState()
    val textFieldState = rememberTextFieldState()

    AppTheme {
        SearchBarInput(
            searchBarState = searchBarState,
            textFieldState = textFieldState,
            onBack = {}
        )
    }
}
