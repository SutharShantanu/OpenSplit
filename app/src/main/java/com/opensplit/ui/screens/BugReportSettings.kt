@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)

package com.opensplit.ui.screens

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.opensplit.BuildConfig
import com.opensplit.ui.components.LocalSnackbarController
import com.opensplit.ui.components.SettingsGroupCard
import com.opensplit.ui.components.SymbolIcon
import com.opensplit.ui.components.responsiveContentWidth
import com.opensplit.ui.theme.OpenSplitIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val BUG_REPORT_EMAIL = "shantanusut2000@gmail.com"
private const val MAX_ATTACHMENTS = 5
private const val MAX_TITLE = 120
private val FREQUENCIES = listOf("Every time", "Sometimes", "Only once")

/* =========================================================================================
 * SUB-SCREEN: REPORT A BUG
 * A short form that fills a structured email (summary, steps, expected vs actual, frequency,
 * device) and hands it to the user's mail app with up to five screenshots or videos attached.
 * ========================================================================================= */
@Composable
fun BugReportSubScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val snackbar = LocalSnackbarController.current
    val scope = rememberCoroutineScope()

    var title by rememberSaveable { mutableStateOf("") }
    var whatHappened by rememberSaveable { mutableStateOf("") }
    var steps by rememberSaveable { mutableStateOf("") }
    var expected by rememberSaveable { mutableStateOf("") }
    var frequency by rememberSaveable { mutableStateOf(FREQUENCIES[0]) }
    var includeDevice by rememberSaveable { mutableStateOf(true) }
    var attachments by remember { mutableStateOf(listOf<Uri>()) }
    var preparing by remember { mutableStateOf(false) }
    val deviceInfo = remember { "OpenSplit ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) · Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT}) · ${Build.MANUFACTURER} ${Build.MODEL}" }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(MAX_ATTACHMENTS)) { picked ->
        val room = MAX_ATTACHMENTS - attachments.size
        if (picked.size > room) snackbar.showMessage("Up to $MAX_ATTACHMENTS attachments; kept the first $room")
        attachments = (attachments + picked.take(room)).distinct()
    }

    fun send() {
        preparing = true
        scope.launch {
            val body = bugReportBody(title, whatHappened, steps, expected, frequency, deviceInfo.takeIf { includeDevice })
            val files = runCatching { withContext(Dispatchers.IO) { copyForSharing(context, attachments) } }
                .getOrElse { snackbar.showMessage("Couldn't attach the files"); preparing = false; return@launch }
            val intent = Intent(if (files.isEmpty()) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(BUG_REPORT_EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, "[OpenSplit bug] ${title.trim()}")
                putExtra(Intent.EXTRA_TEXT, body)
                type = "message/rfc822"
                if (files.isNotEmpty()) {
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(files))
                    clipData = ClipData.newRawUri(null, files.first()).apply { files.drop(1).forEach { addItem(ClipData.Item(it)) } }
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
            try {
                context.startActivity(Intent.createChooser(intent, "Send bug report"))
            } catch (e: Exception) {
                snackbar.showMessage("No email app found. Email $BUG_REPORT_EMAIL directly.")
            }
            preparing = false
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = Modifier.fillMaxSize().responsiveContentWidth(maxWidth = 760.dp)) {
            TopAppBar(
                title = { Text("Report a bug", fontWeight = FontWeight.Bold) },
                navigationIcon = { com.opensplit.ui.components.RoundBackButton(onClick = onBack) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        "Fill in what you can. We'll open your email app with a ready-to-send report; you can review it before sending.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                item {
                    SettingsGroupCard(title = "Your Report", icon = OpenSplitIcons.Description) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = title, onValueChange = { title = it.take(MAX_TITLE) },
                                label = { Text("Summary") },
                                placeholder = { Text("e.g. Balance wrong after editing an expense") },
                                singleLine = true, modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = whatHappened, onValueChange = { whatHappened = it },
                                label = { Text("What happened?") },
                                placeholder = { Text("Describe the problem you saw") },
                                minLines = 3, modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = steps, onValueChange = { steps = it },
                                label = { Text("Steps to reproduce (optional)") },
                                placeholder = { Text("1. Open a group\n2. Edit an expense\n3. …") },
                                minLines = 3, modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = expected, onValueChange = { expected = it },
                                label = { Text("What did you expect? (optional)") },
                                minLines = 2, modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    SettingsGroupCard(title = "How Often Does It Happen?", icon = OpenSplitIcons.Schedule) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FREQUENCIES.forEach { f ->
                                FilterChip(selected = frequency == f, onClick = { frequency = f }, label = { Text(f) })
                            }
                        }
                    }
                }
                item {
                    SettingsGroupCard(title = "Screenshots or Videos (${attachments.size}/$MAX_ATTACHMENTS)", icon = OpenSplitIcons.PhotoLibrary) {
                        // contentPadding (not padding) so thumbnails scroll to the surface edge.
                        LazyRow(contentPadding = PaddingValues(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(attachments, key = { it.toString() }) { uri ->
                                Box(modifier = Modifier.size(96.dp)) {
                                    AsyncImage(
                                        model = uri, contentDescription = "Attachment", contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                                    )
                                    Surface(
                                        onClick = { attachments = attachments - uri },
                                        shape = CircleShape,
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                                        modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            SymbolIcon(OpenSplitIcons.Close, contentDescription = "Remove attachment", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                            if (attachments.size < MAX_ATTACHMENTS) item {
                                Surface(
                                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(96.dp)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                        SymbolIcon(OpenSplitIcons.PhotoLibrary, contentDescription = null)
                                        Spacer(Modifier.height(4.dp))
                                        Text("Add", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                item {
                    SettingsGroupCard(title = "Details", icon = OpenSplitIcons.Info) {
                        // Checkbox carries its own 12dp touch inset, so less start padding keeps it aligned at 16dp.
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable { includeDevice = !includeDevice }
                                .padding(start = 4.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Checkbox(checked = includeDevice, onCheckedChange = { includeDevice = it })
                            Column {
                                Text("Include app and device details", style = MaterialTheme.typography.bodyLarge)
                                Text(deviceInfo, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item {
                    Button(
                        onClick = ::send,
                        enabled = !preparing && title.isNotBlank() && whatHappened.isNotBlank(),
                        shapes = com.opensplit.ui.theme.appButtonShapes(),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        SymbolIcon(OpenSplitIcons.Send, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Continue in email")
                    }
                }
            }
        }
    }
}

/** Plain-text report with fixed sections so every email reads the same way. */
internal fun bugReportBody(
    title: String, whatHappened: String, steps: String, expected: String, frequency: String, deviceInfo: String?
): String = buildString {
    fun section(heading: String, text: String) {
        append("■ ").append(heading).append('\n')
        append(text.trim().ifBlank { "Not provided" }).append("\n\n")
    }
    append("OPENSPLIT BUG REPORT\n")
    append("====================\n\n")
    section("Summary", title)
    section("What happened", whatHappened)
    section("Steps to reproduce", steps)
    section("Expected behaviour", expected)
    section("How often", frequency)
    section("Device", deviceInfo ?: "Not shared")
    append("Reported: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm zzz", Locale.US).format(Date())).append('\n')
}

/** Copies picked media into cache so the mail app can read it through our FileProvider. */
private fun copyForSharing(context: Context, uris: List<Uri>): List<Uri> {
    val dir = File(context.cacheDir, "bug_report").apply { deleteRecursively(); mkdirs() }
    return uris.mapIndexed { i, uri ->
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(context.contentResolver.getType(uri)) ?: "jpg"
        val file = File(dir, "attachment_${i + 1}.$ext")
        context.contentResolver.openInputStream(uri)!!.use { input -> file.outputStream().use { input.copyTo(it) } }
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    }
}
