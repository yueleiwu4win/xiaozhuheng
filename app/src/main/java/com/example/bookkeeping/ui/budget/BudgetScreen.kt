package com.example.bookkeeping.ui.budget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bookkeeping.LocalAppContainer
import com.example.bookkeeping.util.AmountFormatter
import com.example.bookkeeping.data.repo.parseBudgetAmount
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import java.time.YearMonth

@Composable
fun BudgetStrip(amount: Long?, expense: Long, currency: String, onClick: () -> Unit) {
    Surface(Modifier.fillMaxWidth().clickable(onClick = onClick), color = MaterialTheme.colorScheme.surface.copy(alpha = .94f)) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (amount == null) Text("设置本月预算 ›", style = MaterialTheme.typography.bodyMedium)
            else {
                val remaining = amount - expense
                val color = if (expense >= amount) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                Text(when { remaining < 0 -> "预算超出 ${AmountFormatter.withSymbol(-remaining, currency)}"; remaining == 0L -> "预算已用完"; else -> "预算剩余 ${AmountFormatter.withSymbol(remaining, currency)}" }, style = MaterialTheme.typography.bodyMedium, color = color)
                LinearProgressIndicator(progress = { (expense.toDouble() / amount).toFloat().coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(3.dp), color = color)
                Text("已使用 ${"%.0f".format(expense.toDouble() / amount * 100)}% · 点击调整", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(month: YearMonth, onBack: () -> Unit) {
    val container = LocalAppContainer.current
    val repo = container.budgetRepository
    val settings by container.settingsRepository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var text by rememberSaveable(month.toString()) { mutableStateOf("") }
    var loaded by rememberSaveable(month.toString()) { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var previous by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(month) {
        try {
            val current = repo.get(month)
            if (!loaded) text = current?.amountCents?.let { AmountFormatter.format(it).replace(",", "") }.orEmpty()
            previous = repo.get(month.minusMonths(1))?.amountCents
            loaded = true
        } catch (e: CancellationException) { throw e } catch (_: Exception) { error = "预算读取失败，请返回重试" }
    }
    fun save(remove: Boolean = false) {
        val cents = parseBudgetAmount(text)
        if (!remove && cents == null) { error = "请输入有效金额，最多八位整数和两位小数"; return }
        if (busy || !loaded) return
        busy = true
        scope.launch {
            try { if (remove) repo.remove(month) else repo.set(month, cents!!); onBack() }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error = "保存失败，请重试" }
            finally { busy = false }
        }
    }
    Scaffold(containerColor = MaterialTheme.colorScheme.background, topBar = {
        TopAppBar(title = { Text("${month.year}年${month.monthValue}月预算") }, navigationIcon = { TextButton(onBack, enabled = !busy) { Text("返回") } })
    }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("月度总支出预算", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(text, { if (it.length <= 11) { text = it; error = null } }, label = { Text("预算金额 · ${settings.currency}") }, enabled = loaded && !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("预算仅统计支出，收入不抵扣。每个月独立保存，补记、修改和删除账单后自动更新进度。")
            previous?.let { cents -> OutlinedButton({ text = AmountFormatter.format(cents).replace(",", "") }, enabled = !busy && loaded) { Text("沿用上月预算") } }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button({ save() }, enabled = loaded && !busy, modifier = Modifier.fillMaxWidth()) { Text(if (busy) "保存中…" else "保存预算") }
            TextButton({ save(true) }, enabled = loaded && !busy) { Text("取消本月预算") }
        }
    }
}
