sed -i '/val defaultCurrency: String/a \    , val allExpenses: List<com.example.domain.model.Expense>' app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt

sed -i '/flow {/i \                expenseRepository.getExpensesForUser(currentUser.uid),' app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt

sed -i 's/) { groups, friendsBalances, currency ->/) { groups, friendsBalances, expenses, currency ->/' app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt

sed -i '/val pendingInvites = emptyList<String>()/a \                val allExpenses = expenses' app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt

sed -i '/defaultCurrency = currency,/a \                        allExpenses = allExpenses,' app/src/main/java/com/example/ui/viewmodel/AccountViewModel.kt
