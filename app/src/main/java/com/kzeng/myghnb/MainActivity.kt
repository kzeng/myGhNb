package com.kzeng.myghnb

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
                title = { Text(if (screen == "editor") "编辑文章" else if (screen == "about") "About" else "My GH Notebook", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { if (screen != "home") IconButton(onClick = { screen = "home" }) { Icon(Icons.Default.ArrowBack, "返回") } },
                actions = { if (screen == "home") {
                    IconButton(onClick = { darkTheme = !darkTheme; settings.edit().putBoolean("dark_theme", darkTheme).apply() }) { Icon(if (darkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, "切换主题") }
                    IconButton(onClick = { screen = "about" }) { Icon(Icons.Default.Info, "About") }
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = { if (screen == "home") FloatingActionButton(onClick = ::newNote) { Icon(Icons.Default.Add, "新建") } }
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
            "reader" -> ReaderScreenV2(selected ?: Note("", "", ""), padding)
            "editor" -> EditorScreenV2(selected ?: Note("", "", ""), padding, onSave = {
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
                Card(Modifier.fillMaxWidth().clickable { onOpen(note) }) {
                    Column(Modifier.padding(16.dp)) {
                        if (section == "archive") Text(note.date.take(7), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        Text(note.title.ifBlank { "未命名" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${note.date} · ${note.fileName}", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { IconButton(onClick = { onEdit(note) }) { Icon(Icons.Default.Edit, "编辑文章") } }
                    }
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorScreenV2(initial: Note, padding: androidx.compose.foundation.layout.PaddingValues, onSave: (Note) -> Unit, onPublish: (Note) -> Unit) {
    var title by remember(initial.fileName) { mutableStateOf(initial.title) }
    var body by remember(initial.fileName) { mutableStateOf(initial.body) }
    var mode by remember(initial.fileName) { mutableStateOf("code") }
    var visualEditor by remember { mutableStateOf<WebView?>(null) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("标题") }, singleLine = true)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://imgchr.com/"))) }) { Icon(Icons.Default.Image, null); Spacer(Modifier.size(6.dp)); Text("打开图床") }
            OutlinedButton(onClick = { val clip = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager; clip.primaryClip?.getItemAt(0)?.text?.toString()?.let { body += "\n\n$it\n" } }) { Text("粘贴图片链接") }
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedButton(onClick = { mode = "code" }) { Text("Code") }
            OutlinedButton(onClick = { mode = "visual" }) { Text("WYSIWYG") }
            OutlinedButton(onClick = { mode = "preview" }) { Text("预览文章") }
        }
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
                        loadDataWithBaseURL("https://kzeng.github.io/", markdownToHtml(body, editable = true), "text/html", "UTF-8", null)
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
            Button(onClick = { saveFromEditor(initial, title, body, mode, visualEditor, onSave) }, Modifier.weight(1f)) { Icon(Icons.Default.Save, null); Spacer(Modifier.size(6.dp)); Text("保存草稿") }
            Button(onClick = { saveFromEditor(initial, title, body, mode, visualEditor, onPublish) }, Modifier.weight(1f)) { Icon(Icons.Default.Send, null); Spacer(Modifier.size(6.dp)); Text("发布") }
        }
    }
}

private fun saveFromEditor(initial: Note, title: String, body: String, mode: String, visualEditor: WebView?, callback: (Note) -> Unit) {
    if (mode != "visual" || visualEditor == null) {
        callback(initial.copy(title = title, body = body))
        return
    }
    visualEditor.evaluateJavascript("document.body.innerHTML") { rawHtml ->
        val html = runCatching { org.json.JSONTokener(rawHtml).nextValue() as String }.getOrDefault(rawHtml)
        callback(initial.copy(title = title, body = htmlToMarkdown(html)))
    }
}

private fun markdownToHtml(markdown: String, editable: Boolean = false): String {
    val imageHtml = mutableListOf<String>()
    val imagePattern = Regex("""!\[([^]]*)\]\(\s*(?:<([^>]+)>|([^\s)]+))(?:\s+[\"'][^)]*[\"'])?\s*\)""")
    val markdownWithImageTokens = imagePattern.replace(markdown) {
        val imageUrl = (it.groupValues[2].ifBlank { it.groupValues[3] }).trim().replace("http://", "https://")
        val alt = it.groupValues[1]
        val token = "MYGHNB_IMAGE_TOKEN_${imageHtml.size}"
        imageHtml += "<img src=\"$imageUrl\" alt=\"$alt\" loading=\"lazy\" style=\"display:block;max-width:100%;height:auto\"/>"
        token
    }
    var html = markdownWithImageTokens.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
    imageHtml.forEachIndexed { index, image -> html = html.replace("MYGHNB_IMAGE_TOKEN_$index", image) }
    html = Regex("(?m)^### (.+)$").replace(html, "<h3>$1</h3>")
    html = Regex("(?m)^## (.+)$").replace(html, "<h2>$1</h2>")
    html = Regex("(?m)^# (.+)$").replace(html, "<h1>$1</h1>")
    html = Regex("\\*\\*(.+?)\\*\\*").replace(html, "<strong>$1</strong>")
    html = Regex("(?<!\\*)\\*([^*]+)\\*(?!\\*)").replace(html, "<em>$1</em>")
    html = html.replace("\n\n", "</p><p>").replace("\n", "<br/>")
    val edit = if (editable) " contenteditable=\"true\" spellcheck=\"true\"" else ""
    return "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width, initial-scale=1\"><style>body{font-family:sans-serif;padding:16px;line-height:1.65;color:#202124;background:#fff}img{display:block;margin:12px 0}h1,h2,h3{line-height:1.25}</style></head><body$edit><p>$html</p></body></html>"
}

private fun htmlToMarkdown(html: String): String = html
    .replace(Regex("<img[^>]*alt=\\\"([^\\\"]*)\\\"[^>]*src=\\\"([^\\\"]+)\\\"[^>]*/?>"), "![\$1](\$2)")
    .replace(Regex("<br\\s*/?>"), "\n")
    .replace(Regex("</(p|div|h1|h2|h3)>"), "\n\n")
    .replace(Regex("<h1>(.*?)</h1>"), "# \$1")
    .replace(Regex("<h2>(.*?)</h2>"), "## \$1")
    .replace(Regex("<h3>(.*?)</h3>"), "### \$1")
    .replace(Regex("<strong>(.*?)</strong>"), "**\$1**")
    .replace(Regex("<em>(.*?)</em>"), "*\$1*")
    .replace(Regex("<[^>]+>"), "")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .trim()

@Composable
private fun ReaderScreenV2(note: Note, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(note.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(note.date, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AndroidView(
            modifier = Modifier.fillMaxWidth().weight(1f),
            factory = { context ->
                WebView(context).apply {
                    configureArticleWebView()
                    loadDataWithBaseURL("https://kzeng.github.io/", markdownToHtml(note.body), "text/html", "UTF-8", null)
                }
            }
        )
    }
}

private fun WebView.configureArticleWebView() {
    webViewClient = ArticleWebViewClient()
    settings.javaScriptEnabled = false
    settings.domStorageEnabled = true
    settings.loadsImagesAutomatically = true
    settings.blockNetworkImage = false
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
}

private class ArticleWebViewClient : WebViewClient() {
    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (request.isForMainFrame) return null
        val scheme = request.url.scheme ?: return null
        if (scheme != "https" && scheme != "http") return null

        return runCatching {
            val connection = java.net.URL(request.url.toString()).openConnection() as java.net.HttpURLConnection
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("User-Agent", view.settings.userAgentString)
            connection.setRequestProperty("Referer", "https://kzeng.github.io/")
            connection.setRequestProperty("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8")
            connection.setRequestProperty("Accept-Encoding", "identity")
            connection.connect()
            if (connection.responseCode !in 200..299) return@runCatching null

            val mimeType = connection.contentType?.substringBefore(';')?.trim().orEmpty().ifBlank { "image/*" }
            WebResourceResponse(mimeType, null, connection.inputStream)
        }.getOrNull()
    }
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
    Column(Modifier.fillMaxSize().padding(padding).padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Image(painterResource(com.kzeng.myghnb.R.drawable.my_gh_nb_logo), "My GH Notebook", Modifier.size(112.dp))
        Spacer(Modifier.height(18.dp))
        Text("My GH Notebook", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("GitHub + Notebook", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))
        Text("Author: Zengkai001@gmail.com")
        Text("Version: 0.0.1")
        Spacer(Modifier.height(36.dp))
        Text("GitHub 发布配置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(token, { token = it; saved = false }, Modifier.fillMaxWidth(), label = { Text("Fine-grained Token") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
        Spacer(Modifier.height(8.dp))
        Button(onClick = { prefs.edit().putString("github_token", token.trim()).apply(); saved = true }) { Text(if (saved) "已保存" else "保存 Token") }
        Text("Token 仅保存在本机，不会写入项目文件。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun loadDrafts(context: Context): List<Note> = runCatching {
    val array = JSONArray(context.getSharedPreferences("drafts", 0).getString("items", "[]"))
    List(array.length()) { i -> val o = array.getJSONObject(i); Note(o.getString("file"), o.getString("title"), o.getString("date"), o.optString("body")) }
}.getOrDefault(emptyList())

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
    val byFile = (remote + drafts).associateBy { it.fileName }
    return byFile.values.toList()
}

private fun loadNotes(context: Context): List<Note> = mergeNotes(loadDrafts(context), loadCachedNotes(context))

private fun parseTags(raw: String): List<String> = Regex("(?m)^tags:\\s*\\[([^]]*)]").find(raw)?.groupValues?.get(1).orEmpty()
    .split(',', '、')
    .map { it.trim().trim('"', '\'') }
    .filter { it.isNotBlank() }

private fun saveDrafts(context: Context, notes: List<Note>) {
    val array = JSONArray(); notes.forEach { array.put(JSONObject().apply { put("file", it.fileName); put("title", it.title); put("date", it.date); put("body", it.body) }) }
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
