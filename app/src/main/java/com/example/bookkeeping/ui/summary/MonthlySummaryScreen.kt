package com.example.bookkeeping.ui.summary

import android.content.Intent
import android.content.ClipData
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.bookkeeping.LocalAppContainer
import com.example.bookkeeping.R
import com.example.bookkeeping.data.repo.MonthlySummary
import com.example.bookkeeping.data.repo.MonthlySummaryRepository
import com.example.bookkeeping.util.AmountFormatter
import kotlinx.coroutines.*
import java.time.LocalDate
import java.time.YearMonth
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthlySummaryScreen(month: YearMonth, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var summary by remember(month) { mutableStateOf<MonthlySummary?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var hidden by rememberSaveable { mutableStateOf(false) }
    var artwork by rememberSaveable { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var busy by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val background = colors.background.toArgb(); val foreground = colors.onBackground.toArgb(); val accent = colors.primary.toArgb()
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    val currency = AmountFormatter.symbolOf(settings.currency)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(month, retry, lifecycle) {
        error = null
        try { lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { MonthlySummaryRepository(container.database).observe(month, LocalDate.now()).collect { summary = it } } }
        catch (e: CancellationException) { throw e } catch (_: Exception) { error = "小结读取失败，请重试" }
    }
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
        if (uri != null && bitmap != null) {
            val snapshot = bitmap!!
            scope.launch {
                busy = true
                try { withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { check(snapshot.compress(Bitmap.CompressFormat.PNG, 100, it)) } ?: error("无法写入") } }
                catch (e: CancellationException) { throw e } catch (_: Exception) { error = "图片保存失败，请重试" }
                finally { busy = false }
            }
        }
    }
    fun preparePoster() {
        val data = summary ?: return
        if (busy) return
        val style = settings.themeStyle
        val useSerif = settings.reading.font.value == "serif" || (settings.reading.font.value == "theme" && style.value != "classic")
        val lines = SummaryPoster.lines(data, currency, hidden)
        val includeArtwork = artwork
        busy = true
        scope.launch {
            try {
                bitmap = withContext(Dispatchers.Default) {
                    val base = if (useSerif) ResourcesCompat.getFont(context, if (settings.reading.weight.value == "medium") R.font.ledger_medium else R.font.ledger_text) ?: Typeface.DEFAULT else Typeface.DEFAULT
                    val font = if (settings.reading.weight.value == "bold") Typeface.create(base, Typeface.BOLD) else base
                    val image = if (includeArtwork) BitmapFactory.decodeResource(context.resources, R.drawable.background_abacus) else null
                    try { SummaryPoster.render(lines, background, foreground, accent, font, image) } finally { image?.recycle() }
                }
            } catch (e: CancellationException) { throw e } catch (_: Exception) { error = "图片生成失败，请重试" }
            finally { busy = false }
        }
    }
    bitmap?.let { preview ->
        Dialog(onDismissRequest = { if (!busy) bitmap = null }) {
            Surface(shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().heightIn(max = 640.dp).padding(12.dp)) {
                    Text("分享预览", style = MaterialTheme.typography.titleMedium)
                    Box(Modifier.weight(1f).verticalScroll(rememberScrollState())) { Image(preview.asImageBitmap(), "完整月度小结预览", Modifier.fillMaxWidth().aspectRatio(preview.width.toFloat() / preview.height)) }
                    Button({ saveLauncher.launch("小珠衡-${month}-月度小结.png") }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("保存图片") }
                    OutlinedButton({
                        if (!busy) scope.launch {
                            busy = true
                            try {
                                val file = withContext(Dispatchers.IO) {
                                    val dir = File(context.cacheDir, "export").apply { mkdirs() }
                                    dir.listFiles()?.filter { it.name.startsWith("summary-") && it.lastModified() < System.currentTimeMillis() - 86400000 }?.forEach { it.delete() }
                                    File(dir, "summary-${month}-${System.currentTimeMillis()}.png").also { f -> f.outputStream().use { check(preview.compress(Bitmap.CompressFormat.PNG, 100, it)) } }
                                }
                                val uri = container.exportManager.shareUriFor(file)
                                val intent = Intent(Intent.ACTION_SEND).apply { type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); clipData = ClipData.newRawUri("月度小结", uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                                context.startActivity(Intent.createChooser(intent, "分享月度小结"))
                            } catch (e: CancellationException) { throw e } catch (_: Exception) { error = "分享失败，请重试" }
                            finally { busy = false }
                        }
                    }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("分享图片") }
                    TextButton({ bitmap = null }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("关闭") }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            }
        }
    }
    Scaffold(containerColor = colors.background, topBar = { TopAppBar(title = { Text("月度小结") }, navigationIcon = { TextButton(onBack) { Text("返回") } }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            if (summary == null && error == null) CircularProgressIndicator()
            error?.let { Text(it, color = colors.error); TextButton({ retry++ }) { Text("重试") } }
            summary?.let { data ->
                SummaryContent(data, currency, hidden)
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text("隐藏金额", Modifier.weight(1f)); Switch(hidden, { hidden = it }) }
                Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) { Text("海报添加淡人物背景", Modifier.weight(1f)); Switch(artwork, { artwork = it }) }
                Button({ preparePoster() }, enabled = !busy && data.count > 0, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "正在生成…" else "生成分享图片") }
            }
        }
    }
}

@Composable
internal fun SummaryContent(data: MonthlySummary, currency: String, hidden: Boolean) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SummaryPoster.lines(data, currency, hidden).forEachIndexed { index, text ->
            if (index in 2..4) Surface(color = colors.surfaceVariant, shape = MaterialTheme.shapes.medium) { Text(text, Modifier.fillMaxWidth().padding(16.dp), style = MaterialTheme.typography.titleLarge) }
            else Text(text, style = if (index == 0) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge)
        }
    }
}
