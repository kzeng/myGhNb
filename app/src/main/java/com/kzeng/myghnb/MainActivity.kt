package com.kzeng.myghnb

import android.Manifest
import android.content.Context
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.io.IOException
import java.net.URL
import java.time.LocalDate
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.SecretKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job

data class Note(val fileName: String, val title: String, val date: String, val body: String = "", val sha: String? = null, val tags: List<String> = emptyList())

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MyGhNbApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyGhNbApp() {
    val context = LocalContext.current
    val settings = remember { context.getSharedPreferences("settings", 0) }
    val systemDarkTheme = isSystemInDarkTheme()
    var darkTheme by remember { mutableStateOf(settings.getBoolean("dark_theme", systemDarkTheme)) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var screen by remember { mutableStateOf("home") }
    var notes by remember { mutableStateOf(loadNotes(context)) }
    var selected by remember { mutableStateOf<Note?>(null) }
    var loading by remember { mutableStateOf(false) }

    fun newNote() {
        val date = LocalDate.now().toString()
        val number = (notes.count { it.fileName.startsWith("p$date-") } + 1).toString().padStart(3, '0')
        selected = Note("p$date-$number.md", "未命名", date)
        screen = "editor"
    }

    MyGhNbTheme(darkTheme) {
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (screen == "editor") "编辑文章" else if (screen == "about") "About" else "My GH Notebook",
                        modifier = Modifier.padding(start = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = { if (screen != "home") IconButton(onClick = { screen = "home" }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { if (screen == "home") {
                    IconButton(onClick = { darkTheme = !darkTheme; settings.edit().putBoolean("dark_theme", darkTheme).apply() }) { Icon(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, "切换主题") }
                    IconButton(onClick = { screen = "about" }) { Icon(Icons.Default.Info, "About") }
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            if (screen == "home") {
                FloatingActionButton(
                    onClick = ::newNote,
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) { Icon(Icons.Default.Add, "新建") }
            }
        }
    ) { padding ->
        when (screen) {
            "home" -> HomeScreenV3(notes, loading, padding, onRefresh = {
                loading = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { GitHubClient.fetchNotes(context) }
                    if (result.isNotEmpty()) {
                        saveCachedNotes(context, result)
                        notes = mergeNotes(loadDrafts(context), result)
                    }
                    loading = false
                    snackbar.showSnackbar(if (result.isEmpty()) "同步失败或暂无文章" else "已同步 ${result.size} 篇文章")
                }
            }, onOpen = { selected = it; screen = "reader" }, onEdit = { selected = it; screen = "editor" })
            "reader" -> ReaderScreenV2(selected ?: Note("", "", ""), darkTheme, padding)
            "editor" -> EditorScreenV2(selected ?: Note("", "", ""), darkTheme, padding, onSave = {
                notes = notes.filterNot { n -> n.fileName == it.fileName } + it
                saveDrafts(context, notes)
                selected = it
                scope.launch { snackbar.showSnackbar("草稿已保存") }
            }, onPublish = { note ->
                loading = true
                scope.launch {
                    val ok = GitHubClient.publish(context, note)
                    loading = false
                    snackbar.showSnackbar(if (ok) "已提交到 clash 分支" else "提交失败，请检查 GitHub Token")
                }
            })
            "about" -> AboutScreen(context, padding)
        }
    }
    }
}

@Composable
private fun MyGhNbTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HomeScreenV3(notes: List<Note>, loading: Boolean, padding: androidx.compose.foundation.layout.PaddingValues, onRefresh: () -> Unit, onOpen: (Note) -> Unit, onEdit: (Note) -> Unit) {
    var section by remember { mutableStateOf("all") }
    var query by remember { mutableStateOf("") }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var page by remember(section, query, selectedTag, notes.size) { mutableStateOf(0) }
    val sorted = notes.sortedWith(compareByDescending<Note> { it.date }.thenByDescending { it.fileName })
    val allTags = sorted.flatMap { it.tags }.distinct().sorted()
    val filtered = when (section) {
        "search" -> sorted.filter { query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true) || it.fileName.contains(query, true) }
        "tags" -> sorted.filter { selectedTag == null || selectedTag in it.tags }
        else -> sorted
    }
    val pageSize = 10
    val pageCount = maxOf(1, (filtered.size + pageSize - 1) / pageSize)
    if (page >= pageCount) page = pageCount - 1
    val pageItems = filtered.drop(page * pageSize).take(pageSize)
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { section = "all"; selectedTag = null }) { Icon(Icons.Default.MenuBook, contentDescription = "全部文章") }
            IconButton(onClick = { section = "tags" }) { Icon(Icons.Default.Label, contentDescription = "标签") }
            IconButton(onClick = { section = "archive"; selectedTag = null }) { Icon(Icons.Default.Archive, contentDescription = "归档") }
            IconButton(onClick = { section = "search" }) { Icon(Icons.Default.Search, contentDescription = "查找") }
            if (loading) {
                CircularProgressIndicator(Modifier.padding(12.dp).size(22.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, contentDescription = "同步刷新") }
            }
        }
        if (section == "search") {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("查找文章") }, singleLine = true)
        }
        if (section == "tags") {
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                allTags.forEach { tag -> OutlinedButton(onClick = { selectedTag = if (selectedTag == tag) null else tag }) { Text(tag) } }
            }
        }
        Spacer(Modifier.height(10.dp))
        if (pageItems.isEmpty()) Text("暂无匹配文章", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pageItems) { note ->
                ArticleCard(note, section == "archive", onOpen, onEdit)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(enabled = page > 0, onClick = { page-- }) { Text("上一页") }
            Text(" ${page + 1} / $pageCount ", modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedButton(enabled = page + 1 < pageCount, onClick = { page++ }) { Text("下一页") }
        }
    }
}

@Composable
private fun ArticleCard(note: Note, showMonth: Boolean, onOpen: (Note) -> Unit, onEdit: (Note) -> Unit) {
    /* val excerpt = note.body
        .lineSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("!") && !it.contains("![") }
        .joinToString(" ")
        .replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1")
        .replace(Regex("[*_`>]"), "")
        .trim()
        .let { if (it.length > 110) it.take(110).trimEnd() + "…" else it }
    val displayExcerpt = excerpt.ifBlank {
        if (note.body.contains("![")) "包含图片的文章" else "暂无摘要"
    }
    */
    val readingMinutes = maxOf(1, note.body.length / 450 + 1)

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(note) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.padding(start = 16.dp, top = 15.dp, end = 10.dp, bottom = 12.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (showMonth) note.date.take(7) else note.date,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelMedium
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(note.title.ifBlank { "未命名" }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                note.tags.take(2).forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag, maxLines = 1) }, modifier = Modifier.padding(end = 4.dp))
                }
                if (note.tags.isEmpty()) Text("无标签", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.weight(1f))
                Text("约 $readingMinutes 分钟", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
                IconButton(onClick = { onEdit(note) }) { Icon(Icons.Default.Edit, contentDescription = "编辑文章") }
            }
        }
    }
}

@Composable
private fun HomeScreenV2(notes: List<Note>, loading: Boolean, padding: androidx.compose.foundation.layout.PaddingValues, onRefresh: () -> Unit, onOpen: (Note) -> Unit, onEdit: (Note) -> Unit) {
    var page by remember(notes.size) { mutableStateOf(0) }
    val pageSize = 10
    val sorted = notes.sortedWith(compareByDescending<Note> { it.date }.thenByDescending { it.fileName })
    val pageCount = maxOf(1, (sorted.size + pageSize - 1) / pageSize)
    if (page >= pageCount) page = pageCount - 1
    val pageItems = sorted.drop(page * pageSize).take(pageSize)
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("你的文章", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("GitHub · clash", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (loading) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) else IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "同步") }
        }
        Spacer(Modifier.height(14.dp))
        if (pageItems.isEmpty()) Text("还没有文章，点击右下角开始写作。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(pageItems) { note ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(note) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(note.title.ifBlank { "未命名" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${note.date} · ${note.fileName}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = { onEdit(note) }) { Icon(Icons.Default.Edit, "编辑文章") } }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(enabled = page > 0, onClick = { page-- }) { Text("上一页") }
            Text("  ${page + 1} / $pageCount  ", modifier = Modifier.padding(horizontal = 8.dp))
            OutlinedButton(enabled = page + 1 < pageCount, onClick = { page++ }) { Text("下一页") }
        }
    }
}

@Composable
private fun HomeScreen(notes: List<Note>, loading: Boolean, padding: androidx.compose.foundation.layout.PaddingValues, onRefresh: () -> Unit, onOpen: (Note) -> Unit, onEdit: (Note) -> Unit) {
    Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column { Text("你的文章", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("GitHub · clash", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (loading) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "同步") }
            }
        }
        Spacer(Modifier.height(14.dp))
        if (notes.isEmpty()) Text("还没有本地文章，点击右下角开始写作。", color = MaterialTheme.colorScheme.onSurfaceVariant)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(notes.sortedByDescending { it.date }) { note ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(note) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(note.title.ifBlank { "未命名" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${note.date} · ${note.fileName}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = { onEdit(note) }) { Icon(Icons.Default.Edit, contentDescription = "编辑文章") } }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreen(initial: Note, padding: androidx.compose.foundation.layout.PaddingValues, onSave: (Note) -> Unit, onPublish: (Note) -> Unit) {
    var title by remember(initial.fileName) { mutableStateOf(initial.title) }
    var body by remember(initial.fileName) { mutableStateOf(initial.body) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("标题") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://imgchr.com/"))) }) { Icon(Icons.Default.Image, null); Spacer(Modifier.size(6.dp)); Text("打开图床") }
            OutlinedButton(onClick = { val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clip.primaryClip?.getItemAt(0)?.text?.toString()?.let { body += "\n\n$it\n" } }) { Text("粘贴图片链接") }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("Markdown") }, placeholder = { Text("开始写作…") })
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { onSave(initial.copy(title = title, body = body)) }, Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.size(6.dp)); Text("保存草稿") }
            Button(onClick = { onPublish(initial.copy(title = title, body = body)) }, Modifier.weight(1f)) { Icon(Icons.Default.Send, null); Spacer(Modifier.size(6.dp)); Text("发布") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditorScreenV2(initial: Note, darkTheme: Boolean, padding: androidx.compose.foundation.layout.PaddingValues, onSave: (Note) -> Unit, onPublish: (Note) -> Unit) {
    val context = LocalContext.current
    var title by remember(initial.fileName) { mutableStateOf(initial.title) }
    var body by remember(initial.fileName) { mutableStateOf(initial.body) }
    var tags by remember(initial.fileName) { mutableStateOf(initial.tags) }
    var tagOptions by remember(initial.fileName) { mutableStateOf((loadKnownTags(context) + initial.tags).distinct()) }
    var showTags by remember(initial.fileName) { mutableStateOf(false) }
    var newTag by remember(initial.fileName) { mutableStateOf("") }
    var mode by remember(initial.fileName) { mutableStateOf("visual") }
    var visualEditor by remember { mutableStateOf<WebView?>(null) }
    var showAiPanel by remember { mutableStateOf(false) }
    var aiPrompt by remember { mutableStateOf("") }
    var aiOutput by remember { mutableStateOf("") }
    var aiError by remember { mutableStateOf<String?>(null) }
    var aiLoading by remember { mutableStateOf(false) }
    var aiJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()?.let { recognized ->
            aiPrompt = if (aiPrompt.isBlank()) recognized else "$aiPrompt $recognized"
        }
    }
    val audioPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceLauncher.launch(createVoiceIntent()) else aiError = "需要录音权限才能使用语音输入"
    }

    fun startVoiceInput() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            aiError = "当前设备没有可用的语音识别服务"
        } else if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            voiceLauncher.launch(createVoiceIntent())
        } else {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun startAiGeneration(contextBody: String) {
        val prompt = aiPrompt.trim()
        if (prompt.isBlank()) {
            aiError = "请先输入想让 AI 完成的内容"
            return
        }
        aiJob?.cancel()
        aiOutput = ""
        aiError = null
        aiLoading = true
        aiJob = scope.launch {
            val result = DeepSeekClient.generate(context, prompt, title, tags, contextBody) { chunk ->
                aiOutput += chunk
            }
            result.exceptionOrNull()?.let { aiError = it.message ?: "AI 生成失败" }
            aiLoading = false
            aiJob = null
        }
    }

    fun generateAi() {
        if (mode == "visual" && visualEditor != null) {
            syncVisualToMarkdown(visualEditor!!) { latest ->
                body = latest
                startAiGeneration(latest)
            }
        } else {
            startAiGeneration(body)
        }
    }

    fun appendAiOutput() {
        if (aiOutput.isBlank()) return
        val append: (String) -> Unit = { latest ->
            val merged = if (latest.isBlank()) aiOutput.trim() else latest.trimEnd() + "\n\n" + aiOutput.trim()
            body = merged
            if (mode == "visual") {
                visualEditor?.loadDataWithBaseURL("https://kzeng.github.io/", markdownToHtml(merged, editable = true, dark = darkTheme), "text/html", "UTF-8", null)
            }
            aiOutput = ""
            aiPrompt = ""
            showAiPanel = false
        }
        if (mode == "visual" && visualEditor != null) syncVisualToMarkdown(visualEditor!!, append) else append(body)
    }

    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("标题") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://imgchr.com/"))) }) { Icon(Icons.Default.Image, null); Spacer(Modifier.size(6.dp)); Text("打开图床") }
            OutlinedButton(onClick = { val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clip.primaryClip?.getItemAt(0)?.text?.toString()?.let { body += "\n\n$it\n" } }) { Text("粘贴图片链接") }
            OutlinedButton(onClick = { showTags = !showTags }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text("标签")
            }
            IconButton(onClick = { aiError = null; showAiPanel = true }) {
                Icon(Icons.Default.AutoAwesome, contentDescription = "AI 写作")
            }
        }
        if (showTags) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("选择标签", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        tagOptions.forEach { tag ->
                            FilterChip(
                                selected = tag in tags,
                                onClick = {
                                    tags = if (tag in tags) tags - tag else (tags + tag).distinct()
                                },
                                label = { Text(tag) }
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(newTag, { newTag = it }, Modifier.weight(1f), label = { Text("新增标签") }, singleLine = true)
                        Button(onClick = {
                            val tag = newTag.trim()
                            if (tag.isNotBlank()) {
                                tagOptions = (tagOptions + tag).distinct()
                                tags = (tags + tag).distinct()
                                saveKnownTags(context, tagOptions)
                                newTag = ""
                            }
                        }, enabled = newTag.trim().isNotBlank()) { Text("新增") }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        MarkdownToolbar(
            mode = mode,
            visualEditor = visualEditor,
            onModeChange = { nextMode ->
                if (nextMode == "code" && mode == "visual" && visualEditor != null) {
                    syncVisualToMarkdown(visualEditor!!) { body = it; mode = "code" }
                } else {
                    mode = nextMode
                }
            },
            onBodyChange = { body = it }
        )
        Spacer(Modifier.height(8.dp))
        when (mode) {
            "code" -> OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth().weight(1f), label = { Text("Markdown") }, placeholder = { Text("开始写作…") })
            "visual" -> AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = {
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        visualEditor = this
                        loadDataWithBaseURL("https://kzeng.github.io/", markdownToHtml(body, editable = true, dark = darkTheme), "text/html", "UTF-8", null)
                    }
                },
                update = { visualEditor = it }
            )
            else -> AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = {
                    WebView(context).apply {
                        webViewClient = WebViewClient()
                        settings.javaScriptEnabled = false
                        loadDataWithBaseURL("https://kzeng.github.io/", markdownToHtml(body), "text/html", "UTF-8", null)
                    }
                }
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = { saveFromEditor(initial, title, body, tags, mode, visualEditor, onSave) }, Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.size(6.dp)); Text("保存草稿") }
            Button(onClick = { saveFromEditor(initial, title, body, tags, mode, visualEditor, onPublish) }, Modifier.weight(1f)) { Icon(Icons.Default.Send, null); Spacer(Modifier.size(6.dp)); Text("发布") }
        }
    }
    if (showAiPanel) {
        ModalBottomSheet(onDismissRequest = { if (!aiLoading) showAiPanel = false }) {
            AiPromptSheet(
                prompt = aiPrompt,
                output = aiOutput,
                error = aiError,
                loading = aiLoading,
                onPromptChange = { aiPrompt = it; aiError = null },
                onVoice = ::startVoiceInput,
                onGenerate = ::generateAi,
                onCancel = { aiJob?.cancel(); aiLoading = false },
                onAppend = ::appendAiOutput
            )
        }
    }
}

@Composable
private fun AiPromptSheet(
    prompt: String,
    output: String,
    error: String?,
    loading: Boolean,
    onPromptChange: (String) -> Unit,
    onVoice: () -> Unit,
    onGenerate: () -> Unit,
    onCancel: () -> Unit,
    onAppend: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.size(8.dp))
            Text("AI 写作", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "当前文章会作为上下文，AI 生成内容不会自动覆盖原文。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = prompt,
            onValueChange = onPromptChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("告诉 AI 你想写什么") },
            placeholder = { Text("例如：续写一段总结，保持 Markdown 格式") },
            minLines = 3,
            enabled = !loading,
            trailingIcon = {
                IconButton(onClick = onVoice, enabled = !loading) {
                    Icon(Icons.Default.Mic, contentDescription = "语音输入")
                }
            }
        )
        if (loading) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (error != null) {
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
        if (output.isNotBlank()) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(Modifier.padding(12.dp)) {
                    Text("生成结果", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    Text(output, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (loading) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("停止")
                }
            } else {
                Button(onClick = onGenerate, enabled = prompt.isNotBlank(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("生成 Markdown")
                }
            }
            if (output.isNotBlank() && !loading) {
                Button(onClick = onAppend, modifier = Modifier.weight(1f)) { Text("追加到正文") }
            }
        }
    }
}

@Composable
private fun MarkdownToolbar(
    mode: String,
    visualEditor: WebView?,
    onModeChange: (String) -> Unit,
    onBodyChange: (String) -> Unit
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = { applyMarkdownTool("bold", mode, visualEditor, onBodyChange) }) {
            Icon(Icons.Default.FormatBold, contentDescription = "粗体")
        }
        IconButton(onClick = { applyMarkdownTool("italic", mode, visualEditor, onBodyChange) }) {
            Icon(Icons.Default.FormatItalic, contentDescription = "斜体")
        }
        IconButton(onClick = { applyMarkdownTool("heading", mode, visualEditor, onBodyChange) }) {
            Icon(Icons.Default.Title, contentDescription = "标题")
        }
        IconButton(onClick = { applyMarkdownTool("code", mode, visualEditor, onBodyChange) }) {
            Icon(Icons.Default.Code, contentDescription = "代码")
        }
        IconButton(onClick = { applyMarkdownTool("list", mode, visualEditor, onBodyChange) }) {
            Icon(Icons.Default.FormatListBulleted, contentDescription = "列表")
        }
        IconButton(onClick = { applyMarkdownTool("link", mode, visualEditor, onBodyChange) }) {
            Icon(Icons.Default.Link, contentDescription = "链接")
        }
        Text("源码", style = MaterialTheme.typography.labelLarge)
        Switch(
            checked = mode == "code",
            onCheckedChange = { onModeChange(if (it) "code" else "visual") }
        )
    }
}

private fun applyMarkdownTool(
    tool: String,
    mode: String,
    visualEditor: WebView?,
    onBodyChange: (String) -> Unit
) {
    if (mode == "visual" && visualEditor != null) {
        val command = when (tool) {
            "bold" -> "bold" to null
            "italic" -> "italic" to null
            "heading" -> "formatBlock" to "<h2>"
            "code" -> "formatBlock" to "<pre>"
            "list" -> "insertUnorderedList" to null
            "link" -> "createLink" to "https://"
            else -> return
        }
        val argument = command.second?.let { "'$it'" } ?: "null"
        val script = "document.execCommand('${command.first}', false, $argument);"
        visualEditor.evaluateJavascript(script, null)
        return
    }

    val snippet = when (tool) {
        "bold" -> "**粗体**"
        "italic" -> "*斜体*"
        "heading" -> "## 标题"
        "code" -> "`代码`"
        "list" -> "- 列表项"
        "link" -> "[链接文字](https://)"
        else -> return
    }
    onBodyChange(snippet)
}

private fun syncVisualToMarkdown(editor: WebView, onReady: (String) -> Unit) {
    editor.evaluateJavascript("document.body.innerHTML") { rawHtml ->
        val html = runCatching { org.json.JSONTokener(rawHtml).nextValue() as String }.getOrDefault(rawHtml)
        onReady(htmlToMarkdown(html))
    }
}

private fun saveFromEditor(initial: Note, title: String, body: String, tags: List<String>, mode: String, visualEditor: WebView?, callback: (Note) -> Unit) {
    if (mode != "visual" || visualEditor == null) {
        callback(initial.copy(title = title, body = body, tags = tags))
        return
    }
    visualEditor.evaluateJavascript("document.body.innerHTML") { rawHtml ->
        val html = runCatching { org.json.JSONTokener(rawHtml).nextValue() as String }.getOrDefault(rawHtml)
        callback(initial.copy(title = title, body = htmlToMarkdown(html), tags = tags))
    }
}

private fun createVoiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
    putExtra(RecognizerIntent.EXTRA_PROMPT, "请说出你想让 AI 完成的内容")
}

private object DeepSeekClient {
    private const val endpoint = "https://api.deepseek.com/chat/completions"

    suspend fun generate(
        context: Context,
        prompt: String,
        title: String,
        tags: List<String>,
        body: String,
        onChunk: suspend (String) -> Unit
    ): Result<String> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val token = loadSecret(context, "deepseek_token")
            if (token.isBlank()) throw IOException("请先在 About 页面保存 DeepSeek Token")
            connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 120_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $token")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "text/event-stream")
            }
            val contextText = buildString {
                appendLine("当前文章标题：$title")
                appendLine("当前文章标签：${tags.joinToString("、")}")
                appendLine("当前 Markdown 正文：")
                appendLine(body)
                appendLine()
                appendLine("用户要求：")
                appendLine(prompt)
                appendLine()
                appendLine("请只输出可以直接追加到文章末尾的 Markdown 内容，不要输出解释，不要使用 Markdown 代码围栏包裹结果。")
            }
            val messages = JSONArray()
                .put(JSONObject().put("role", "system").put("content", "你是一个严谨的 Markdown 写作助手。输出必须是合法 Markdown。"))
                .put(JSONObject().put("role", "user").put("content", contextText))
            val payload = JSONObject()
                .put("model", "deepseek-chat")
                .put("messages", messages)
                .put("stream", true)
                .put("temperature", 0.7)
            connection.outputStream.use { it.write(payload.toString().toByteArray(StandardCharsets.UTF_8)) }
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val error = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                throw IOException("DeepSeek API $responseCode: ${error.take(240)}")
            }
            val output = StringBuilder()
            BufferedReader(InputStreamReader(connection.inputStream, StandardCharsets.UTF_8)).useLines { lines ->
                lines.forEach { line ->
                    if (!line.startsWith("data:")) return@forEach
                    val data = line.removePrefix("data:").trim()
                    if (data.isBlank() || data == "[DONE]") return@forEach
                    val content = runCatching {
                        JSONObject(data).optJSONArray("choices")?.optJSONObject(0)
                            ?.optJSONObject("delta")?.optString("content").orEmpty()
                    }.getOrDefault("")
                    if (content.isNotEmpty()) {
                        output.append(content)
                        withContext(kotlinx.coroutines.Dispatchers.Main) { onChunk(content) }
                    }
                }
            }
            if (output.isBlank()) throw IOException("DeepSeek 没有返回 Markdown 内容")
            Result.success(output.toString())
        } catch (error: Exception) {
            if (error is CancellationException) throw error
            Result.failure(error)
        } finally {
            connection?.disconnect()
        }
    }
}

private const val secretKeyAlias = "myghnb_secret_key"

private fun getSecretKey(): SecretKey {
    val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    if (!keyStore.containsAlias(secretKeyAlias)) {
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                secretKeyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        generator.generateKey()
    }
    return keyStore.getKey(secretKeyAlias, null) as SecretKey
}

private fun saveSecret(context: Context, name: String, value: String) {
    if (value.isBlank()) {
        context.getSharedPreferences("secure_settings", 0).edit().remove(name).apply()
        return
    }
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
    val encrypted = cipher.iv + cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
    context.getSharedPreferences("secure_settings", 0).edit()
        .putString(name, Base64.encodeToString(encrypted, Base64.NO_WRAP)).apply()
}

private fun loadSecret(context: Context, name: String): String = runCatching {
    val encoded = context.getSharedPreferences("secure_settings", 0).getString(name, null) ?: return ""
    val encrypted = Base64.decode(encoded, Base64.NO_WRAP)
    val iv = encrypted.copyOfRange(0, 12)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
    String(cipher.doFinal(encrypted.copyOfRange(12, encrypted.size)), StandardCharsets.UTF_8)
}.getOrDefault("")

private fun markdownToHtml(markdown: String, editable: Boolean = false, dark: Boolean = false): String {
    val imageHtml = mutableListOf<String>()
    val codeHtml = mutableListOf<String>()
    fun imageToken(altText: String, source: String): String {
        val imageUrl = source.trim().let { url -> if (url.startsWith("//")) "https:$url" else url }
        val alt = altText.replace("&", "&amp;").replace("\"", "&quot;")
        val escapedUrl = imageUrl.replace("&", "&amp;").replace("\"", "&quot;")
        val token = "MYGHNB_IMAGE_TOKEN_${imageHtml.size}"
        imageHtml += "<img src=\"$escapedUrl\" alt=\"$alt\" loading=\"eager\" onerror=\"if(this.src.indexOf('https://') === 0){this.onerror=null;this.src=this.src.replace('https://','http://');}\" style=\"display:block;max-width:100%;height:auto\"/>"
        return token
    }
    val linkedImagePattern = Regex("""\[!\[([^]]*)\]\(\s*(?:<([^>]+)>|([^\s)]+))\s*\)\]\(\s*(?:<[^>]+>|[^\s)]+)\s*\)""")
    var markdownWithImageTokens = linkedImagePattern.replace(markdown) {
        imageToken(it.groupValues[1], it.groupValues[2].ifBlank { it.groupValues[3] })
    }
    val imagePattern = Regex("""!\[([^]]*)\]\(\s*(?:<([^>]+)>|([^\s)]+))(?:\s+[\"'][^)]*[\"'])?\s*\)""")
    markdownWithImageTokens = imagePattern.replace(markdownWithImageTokens) {
        imageToken(it.groupValues[1], it.groupValues[2].ifBlank { it.groupValues[3] })
    }
    val fencedCodePattern = Regex("""(?s)```([^\n`]*)\n?(.*?)```""")
    markdownWithImageTokens = fencedCodePattern.replace(markdownWithImageTokens) {
        val language = it.groupValues[1].trim()
        val source = it.groupValues[2].trim('\n')
        val token = "MYGHNB_CODE_TOKEN_${codeHtml.size}"
        val languageClass = language.takeIf { it.isNotBlank() }?.let { " class=\"language-$it\"" }.orEmpty()
        codeHtml += "<pre><code$languageClass>${highlightCode(source, language)}</code></pre>"
        token
    }
    var html = markdownWithImageTokens.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    imageHtml.forEachIndexed { index, image -> html = html.replace("MYGHNB_IMAGE_TOKEN_$index", image) }
    codeHtml.forEachIndexed { index, code -> html = html.replace("MYGHNB_CODE_TOKEN_$index", code) }
    html = Regex("(?m)^### (.+)$").replace(html, "<h3>$1</h3>")
    html = Regex("(?m)^## (.+)$").replace(html, "<h2>$1</h2>")
    html = Regex("(?m)^# (.+)$").replace(html, "<h1>$1</h1>")
    html = Regex("\\*\\*(.+?)\\*\\*").replace(html, "<strong>$1</strong>")
    html = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").replace(html, "<em>$1</em>")
    html = html.replace("\n\n", "</p><p>").replace("\n", "<br/>")
    val edit = if (editable) " contenteditable=\"true\" spellcheck=\"true\"" else ""
    val background = if (dark) "#1C1B1F" else "#FFFFFF"
    val foreground = if (dark) "#F4EFF4" else "#202124"
    val link = if (dark) "#D0BCFF" else "#6750A4"
    val selection = if (dark) "#4F378B" else "#D0BCFF"
    val codeBackground = if (dark) "#2B2930" else "#F1EFF4"
    return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>:root{color-scheme:${if (dark) "dark" else "light"}}html,body{min-height:100%;background:$background;color:$foreground}body{font-family:sans-serif;padding:16px;line-height:1.65;caret-color:$foreground}body::selection{background:$selection}a{color:$link}img{display:block;margin:12px 0;max-width:100%;height:auto}h1,h2,h3{line-height:1.25}pre{overflow-x:auto;padding:14px;border-radius:10px;background:$codeBackground;line-height:1.45}pre code{font-family:monospace;white-space:pre}.tok-keyword{color:#C586C0;font-weight:600}.tok-string{color:#CE9178}.tok-comment{color:#6A9955}.tok-number{color:#B5CEA8}</style></head><body$edit><p>$html</p></body></html>"
}

private fun highlightCode(source: String, language: String): String {
    var text = source.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    val protected = mutableListOf<String>()
    fun protect(pattern: Regex, cssClass: String) {
        text = pattern.replace(text) {
            val token = "MYGHNB_HIGHLIGHT_TOKEN_${protected.size}"
            protected += "<span class=\"$cssClass\">${it.value}</span>"
            token
        }
    }

    val lang = language.lowercase()
    if (lang in setOf("python", "py", "shell", "bash", "sh", "yaml", "yml")) {
        protect(Regex("(?m)#.*$"), "tok-comment")
    } else {
        protect(Regex("(?m)//.*$|/\\*[\\s\\S]*?\\*/"), "tok-comment")
    }
    protect(Regex("\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'"), "tok-string")

    val keywords = when (lang) {
        "python", "py" -> "and|as|assert|async|await|break|class|continue|def|elif|else|for|from|if|import|in|is|lambda|None|not|or|pass|print|raise|return|True|False|try|while|with|yield"
        "json" -> "true|false|null"
        "javascript", "js", "typescript", "ts" -> "as|async|await|break|case|class|const|continue|debugger|default|delete|else|export|extends|false|for|from|function|if|import|in|instanceof|let|new|null|return|switch|this|throw|true|try|typeof|var|while|with|yield"
        "kotlin", "java" -> "abstract|as|boolean|break|class|const|continue|data|else|false|final|for|fun|if|import|in|interface|is|new|null|object|override|package|private|public|return|static|this|throw|true|try|val|var|when|while"
        "c", "cpp", "c++" -> "auto|bool|break|case|char|class|const|continue|default|delete|do|double|else|false|float|for|if|include|int|long|namespace|new|null|private|public|return|short|signed|static|struct|switch|template|this|true|typedef|typename|using|void|while"
        else -> ""
    }
    if (keywords.isNotBlank()) {
        text = Regex("\\b($keywords)\\b").replace(text) { "<span class=\"tok-keyword\">${it.value}</span>" }
    }
    text = Regex("\\b\\d+(?:\\.\\d+)?\\b").replace(text) { "<span class=\"tok-number\">${it.value}</span>" }
    protected.forEachIndexed { index, token -> text = text.replace("MYGHNB_HIGHLIGHT_TOKEN_$index", token) }
    return text
}

private fun htmlToMarkdown(html: String): String {
    var markdown = html
        // Accept either attribute order; the visual editor emits src before alt.
        .replace(Regex("""<img\b(?=[^>]*\bsrc=\"([^\"]+)\")(?=[^>]*\balt=\"([^\"]*)\")[^>]*/?>"""), "![\$2](\$1)")
        .replace(Regex("<br\\s*/?>"), "\n")
        .replace(Regex("<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL), "# \$1\n\n")
        .replace(Regex("<h2[^>]*>(.*?)</h2>", RegexOption.DOT_MATCHES_ALL), "## \$1\n\n")
        .replace(Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL), "### \$1\n\n")
        .replace(Regex("<pre[^>]*>(.*?)</pre>", RegexOption.DOT_MATCHES_ALL), "```\n\$1\n```\n\n")
        .replace(Regex("<strong[^>]*>(.*?)</strong>", RegexOption.DOT_MATCHES_ALL), "**\$1**")
        .replace(Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL), "**\$1**")
        .replace(Regex("<em[^>]*>(.*?)</em>", RegexOption.DOT_MATCHES_ALL), "*\$1*")
        .replace(Regex("<i[^>]*>(.*?)</i>", RegexOption.DOT_MATCHES_ALL), "*\$1*")
        .replace(Regex("<a[^>]*href=\"([^\"]+)\"[^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL), "[\$2](\$1)")
        .replace(Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL), "- \$1\n")
        .replace(Regex("</(p|div|ul|ol)>"), "\n\n")
        .replace(Regex("<[^>]+>"), "")
    markdown = markdown
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&quot;", "\"")
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
    return markdown
}

@Composable
private fun ReaderScreenV2(note: Note, darkTheme: Boolean, padding: androidx.compose.foundation.layout.PaddingValues) {
    val context = LocalContext.current
    val articleUrl = "https://kzeng.github.io/posts/${note.fileName.removeSuffix(".md")}/"
    Column(Modifier.fillMaxSize().padding(padding)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(note.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("文章链接", articleUrl))
                Toast.makeText(context, "文章链接已复制", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.Share, contentDescription = "复制文章链接")
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                WebView(context).apply {
                        configureArticleWebView()
                        setBackgroundColor(android.graphics.Color.parseColor(if (darkTheme) "#1C1B1F" else "#FFFFFF"))
                        loadDataWithBaseURL("https://kzeng.github.io/", markdownToHtml(note.body, dark = darkTheme), "text/html", "UTF-8", null)
                }
            }
        )
    }
}

private fun WebView.configureArticleWebView() {
    // Let Chromium load images directly. Intercepting every image with
    // HttpURLConnection breaks redirects, compressed responses, and hosts that
    // require WebView's normal request headers/cookie handling.
    webViewClient = WebViewClient()
    settings.javaScriptEnabled = false
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.blockNetworkImage = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
}

@Composable
private fun ReaderScreen(note: Note, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding).padding(20.dp)) {
        Text(note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(note.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(20.dp))
        Text(note.body.ifBlank { "暂无正文缓存，请先同步文章内容。" }, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AboutScreen(context: Context, padding: androidx.compose.foundation.layout.PaddingValues) {
    val prefs = context.getSharedPreferences("settings", 0)
    var token by remember { mutableStateOf(prefs.getString("github_token", "") ?: "") }
    var saved by remember { mutableStateOf(false) }
    var deepSeekToken by remember { mutableStateOf(loadSecret(context, "deepseek_token")) }
    var deepSeekSaved by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().padding(padding).padding(28.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(painterResource(com.kzeng.myghnb.R.drawable.my_gh_nb_logo), "My GH Notebook", Modifier.size(112.dp))
        Spacer(Modifier.height(18.dp))
        Text("My GH Notebook", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("GitHub + Notebook", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Text("Author: Zengkai001@gmail.com")
        Text("Version: ${BuildConfig.VERSION_NAME}")
        Spacer(Modifier.height(36.dp))
        Text("GitHub 发布配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(token, { token = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Fine-grained Token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { prefs.edit().putString("github_token", token.trim()).apply(); saved = true }) { Text(if (saved) "已保存" else "保存 Token") }
        Text("Token 仅保存在本机，不会写入项目文件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(28.dp))
        Text("DeepSeek AI 配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            deepSeekToken,
            { deepSeekToken = it; deepSeekSaved = false },
            Modifier.fillMaxWidth(),
            label = { Text("DeepSeek Token") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = { saveSecret(context, "deepseek_token", deepSeekToken.trim()); deepSeekSaved = true }) {
            Text(if (deepSeekSaved) "已保存" else "保存 DeepSeek Token")
        }
        Text("Token 使用 Android Keystore 加密保存在本机。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun loadDrafts(context: Context): List<Note> = runCatching {
    val array = JSONArray(context.getSharedPreferences("drafts", 0).getString("items", "[]"))
    List(array.length()) { i -> val o = array.getJSONObject(i); Note(o.getString("file"), o.getString("title"), o.getString("date"), o.optString("body"), tags = o.optJSONArray("tags")?.let { a -> List(a.length()) { n -> a.getString(n) } } ?: emptyList()) }
}.getOrDefault(emptyList())

private fun loadKnownTags(context: Context): List<String> = runCatching {
    JSONArray(context.getSharedPreferences("settings", 0).getString("known_tags", "[]"))
        .let { array -> List(array.length()) { i -> array.getString(i) } }
}.getOrDefault(emptyList())

private fun saveKnownTags(context: Context, tags: List<String>) {
    context.getSharedPreferences("settings", 0).edit().putString("known_tags", JSONArray(tags.distinct()).toString()).apply()
}

private fun loadCachedNotes(context: Context): List<Note> = runCatching {
    val array = JSONArray(context.getSharedPreferences("remote_cache", 0).getString("items", "[]"))
    List(array.length()) { i -> val o = array.getJSONObject(i); Note(o.getString("file"), o.getString("title"), o.getString("date"), o.optString("body"), o.optString("sha").ifBlank { null }, o.optJSONArray("tags")?.let { a -> List(a.length()) { n -> a.getString(n) } } ?: emptyList()) }
}.getOrDefault(emptyList())

private fun saveCachedNotes(context: Context, notes: List<Note>) {
    val array = JSONArray()
    notes.forEach { array.put(JSONObject().apply { put("file", it.fileName); put("title", it.title); put("date", it.date); put("body", it.body); put("sha", it.sha ?: ""); put("tags", JSONArray(it.tags)) }) }
    context.getSharedPreferences("remote_cache", 0).edit().putString("items", array.toString()).apply()
}

private fun mergeNotes(drafts: List<Note>, remote: List<Note>): List<Note> {
    val remoteByFile = remote.associateBy { it.fileName }
    val mergedDrafts = drafts.map { draft ->
        val remoteNote = remoteByFile[draft.fileName]
        draft.copy(
            tags = draft.tags.ifEmpty { remoteNote?.tags ?: emptyList() },
            sha = draft.sha ?: remoteNote?.sha
        )
    }
    val byFile = (remote + mergedDrafts).associateBy { it.fileName }
    return byFile.values.toList()
}

private fun loadNotes(context: Context): List<Note> = mergeNotes(loadDrafts(context), loadCachedNotes(context))

private fun parseTags(raw: String): List<String> = Regex("(?m)^tags:\\s*\\[([^]]*)]").find(raw)?.groupValues?.get(1).orEmpty()
    .split(',', '、')
    .map { it.trim().trim('"', '\'') }
    .filter { it.isNotBlank() }

private fun saveDrafts(context: Context, notes: List<Note>) {
    val array = JSONArray(); notes.forEach { array.put(JSONObject().apply { put("file", it.fileName); put("title", it.title); put("date", it.date); put("body", it.body); put("tags", JSONArray(it.tags)) }) }
    context.getSharedPreferences("drafts", 0).edit().putString("items", array.toString()).apply()
}

private object GitHubClient {
    private const val repo = "https://api.github.com/repos/kzeng/kzeng.github.io/contents/content/posts"
    private fun connection(context: Context, url: String): HttpURLConnection = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestProperty("Accept", "application/vnd.github+json")
        setRequestProperty("User-Agent", "MyGhNotebook/0.0.1")
        context.getSharedPreferences("settings", 0).getString("github_token", null)?.trim()?.takeIf { it.isNotEmpty() }?.let { setRequestProperty("Authorization", "Bearer $it") }
    }
    fun fetchNotes(context: Context): List<Note> = runBlocking(Dispatchers.IO) { runCatching {
        val c = connection(context, "$repo?ref=clash")
        val code = c.responseCode
        val text = (if (code in 200..299) c.inputStream else c.errorStream)?.bufferedReader()?.readText().orEmpty()
        if (code !in 200..299) throw IOException("GitHub API $code: ${runCatching { JSONObject(text).optString("message") }.getOrDefault(text.take(120))}")
        val array = JSONArray(text)
        List(array.length()) { i ->
            val o = array.getJSONObject(i)
            val raw = o.optString("download_url").takeIf { it.isNotBlank() }?.let { url -> URL(url).openStream().bufferedReader().use { it.readText() } } ?: ""
            val title = Regex("(?m)^title:\\s*[\\\"']?(.*?)[\\\"']?\\s*$").find(raw)?.groupValues?.get(1)?.trim().orEmpty().ifBlank { o.getString("name").removeSuffix(".md") }
            val date = Regex("(?m)^date:\\s*(\\d{4}-\\d{2}-\\d{2})").find(raw)?.groupValues?.get(1).orEmpty()
            val body = raw.substringAfter("---", "").substringAfter("---", "").trim()
            Note(o.getString("name"), title, date, body, sha = o.optString("sha"), tags = parseTags(raw))
        }.filterNot { it.fileName == "_index.md" }
    }.getOrDefault(emptyList()) }
    suspend fun publish(context: Context, note: Note): Boolean = withContext(Dispatchers.IO) { runCatching {
        val path = "$repo/${note.fileName}?ref=clash"; val check = connection(context, path); val existing = if (check.responseCode == 200) JSONObject(check.inputStream.bufferedReader().readText()).optString("sha") else null
        val body = "---\ntitle: \"${note.title.replace("\"", "\\\"")}\"\ndate: ${note.date}\ndraft: false\ntags: []\n---\n\n${note.body}"
        val put = connection(context, "$repo/${note.fileName}"); put.requestMethod = "PUT"; put.doOutput = true; put.setRequestProperty("Content-Type", "application/json")
        val payload = JSONObject().apply { put("message", "publish ${note.fileName}"); put("content", Base64.encodeToString(body.toByteArray(), Base64.NO_WRAP)); put("branch", "clash"); if (!existing.isNullOrBlank()) put("sha", existing) }
        put.outputStream.use { it.write(payload.toString().toByteArray()) }; put.responseCode in 200..201
    }.getOrDefault(false) }
}
