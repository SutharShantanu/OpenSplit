sed -i '/if (showNameDialog) {/i \
    if (showCurrencyDialog) {\
        val currencies = listOf("USD", "EUR", "GBP", "INR", "JPY", "AUD", "CAD")\
        AlertDialog(\
            onDismissRequest = { showCurrencyDialog = false },\
            title = { Text("Default Currency") },\
            text = {\
                LazyColumn {\
                    items(currencies) { currency ->\
                        ListItem(\
                            headlineContent = { Text(currency) },\
                            modifier = Modifier.clickable {\
                                viewModel.updateDefaultCurrency(currency)\
                                showCurrencyDialog = false\
                            }\
                        )\
                    }\
                }\
            },\
            confirmButton = {},\
            dismissButton = {\
                TextButton(onClick = { showCurrencyDialog = false }) {\
                    Text("Cancel")\
                }\
            }\
        )\
    }' app/src/main/java/com/example/ui/screens/AccountScreen.kt
