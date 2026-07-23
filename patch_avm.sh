sed -i '56,66c\
                ScreenState.Success(\
                    AccountUiState(\
                        user = currentUser,\
                        groupCount = groupCount,\
                        friendCount = friendCount,\
                        netBalance = netBalance,\
                        defaultCurrency = currency,\
                        pendingInvites = pendingInvites\
                    )\
                ) as ScreenState<AccountUiState>' app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt
