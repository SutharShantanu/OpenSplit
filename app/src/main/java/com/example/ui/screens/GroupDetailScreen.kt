package com.example.ui.screens

import android.content.Context
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.components.StateLayout
import com.example.ui.viewmodel.GroupDetailViewModel
import com.example.ui.viewmodel.ScreenState
import kotlinx.coroutines.launch

import androidx.compose.material.icons.rounded.FileDownload
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.appHazeHeader
import com.example.ui.components.appHazeSource
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    viewModel: GroupDetailViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAddExpense: () -> Unit,
    onNavigateToExpenseDetail: (String, String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddMember by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val emailOrPhone = extractEmailOrPhoneFromContact(context, uri)
                if (emailOrPhone != null) {
                    viewModel.addMemberFromContact(emailOrPhone, context)
                } else {
                    Toast.makeText(context, "No email or phone found for contact.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
        } else {
            Toast.makeText(context, "Permission required to read contacts", Toast.LENGTH_SHORT).show()
        }
    }

    val groupName = if (uiState is ScreenState.Success) (uiState as ScreenState.Success).data.group.name else "Group Details"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(groupName, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showExportSheet = true }) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export Group Expenses")
                    }
                    IconButton(onClick = { showAddMember = true }) {
                        Icon(Icons.Filled.Person, contentDescription = "Add Member")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
                ),
                modifier = Modifier.appHazeHeader(hazeState)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAddExpense) {
                Icon(Icons.Filled.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding())
        ) {
            StateLayout(state = uiState) { data ->
                val filteredExpenses = data.expenses.filter { it.description.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .appHazeSource(hazeState)
                ) {
                    Text(
                        text = "Members",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        data.members.forEach { user ->
                            val bal = data.balances[user.uid] ?: 0.0
                            val color = if (bal > 0.01) MaterialTheme.colorScheme.primary else if (bal < -0.01) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            val currency = data.group.currency
                            val text = if (bal > 0.01) "gets back $currency ${"%.2f".format(bal)}" else if (bal < -0.01) "owes $currency ${"%.2f".format(-bal)}" else "settled up"
                            
                            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                                Icon(Icons.Filled.Person, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(user.displayName, style = MaterialTheme.typography.bodyLarge)
                                    Text(text, style = MaterialTheme.typography.bodyMedium, color = color)
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Search expenses") },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Expenses",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                    
                    if (filteredExpenses.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                            Text("No expenses yet \u2014 add the first one")
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredExpenses) { exp ->
                                Card(modifier = Modifier.fillMaxWidth().clickable { onNavigateToExpenseDetail(data.group.id, exp.id) }) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(exp.description, style = MaterialTheme.typography.titleMedium)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("${exp.amount} ${exp.currency}", style = MaterialTheme.typography.bodyMedium)
                                        Text("Paid by ${data.members.find { it.uid == exp.paidBy }?.displayName ?: "Unknown"}", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }

                if (showExportSheet) {
                    ExportBottomSheet(
                        scopeName = data.group.name,
                        expenses = data.expenses,
                        groups = listOf(data.group),
                        onDismiss = { showExportSheet = false }
                    )
                }
            }
        }
    }


    if (showAddMember) {
        var emailQuery by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddMember = false },
            title = { Text("Add Member") },
            text = {
                Column {
                    Text("Enter the email address of the user you want to add.")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = emailQuery,
                        onValueChange = { emailQuery = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Or")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(onClick = {
                        showAddMember = false
                        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Filled.Contacts, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add from Contacts")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addMemberByEmail(emailQuery)
                        showAddMember = false
                    },
                    enabled = emailQuery.isNotBlank()
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMember = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

suspend fun extractEmailOrPhoneFromContact(context: Context, contactUri: android.net.Uri): String? {
    return kotlinx.coroutines.Dispatchers.IO.let {
        kotlinx.coroutines.withContext(it) {
            var result: String? = null
            var contactId: String? = null
            context.contentResolver.query(contactUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
                }
            }

            if (contactId != null) {
                // Try to get email first
                context.contentResolver.query(
                    ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    null,
                    "${ContactsContract.CommonDataKinds.Email.CONTACT_ID} = ?",
                    arrayOf(contactId),
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        result = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Email.DATA))
                    }
                }

                // If no email, try phone
                if (result == null) {
                    context.contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(contactId),
                        null
                    )?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            result = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                        }
                    }
                }
            }
            result
        }
    }
}
