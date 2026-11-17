@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.opensplit.ui.theme.OpenSplitIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String = "Search expenses, groups, friends...",
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    SearchBarDefaults.InputField(
        query = query,
        onQueryChange = onQueryChange,
        onSearch = { },
        expanded = false,
        onExpandedChange = { },
        placeholder = { Text(placeholderText) },
        leadingIcon = {
            SymbolIcon(OpenSplitIcons.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (isLoading) {
                AppLoadingIndicator(size = 20.dp)
            } else if (query.isNotEmpty()) {
                IconButton(shapes = com.opensplit.ui.theme.appIconButtonShapes(), onClick = { onQueryChange("") }) {
                    SymbolIcon(OpenSplitIcons.Close, contentDescription = "Clear")
                }
            }
        },
        modifier = modifier.fillMaxWidth()
    )
}
