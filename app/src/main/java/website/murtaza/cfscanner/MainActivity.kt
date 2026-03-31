package website.murtaza.cfscanner

import android.os.Bundle
import android.content.ClipData
import android.content.ClipboardManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import coil.compose.AsyncImage
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Locale
import kotlin.random.Random
import website.murtaza.cfscanner.ui.theme.CFScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CFScannerTheme(darkTheme = true, dynamicColor = false) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ScannerApp(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

private enum class AppScreen {
    START,
    SCANNING,
    RESULTS
}

private fun detectInitialLanguage(): AppLanguage {
    return when (Locale.getDefault().language.lowercase(Locale.ROOT)) {
        "fa" -> AppLanguage.FA
        "ru" -> AppLanguage.RU
        "zh" -> AppLanguage.ZH
        else -> AppLanguage.EN
    }
}

data class ScanResult(
    val ip: String,
    val latencyMs: Double
)

data class RegionItem(
    val id: String,
    val label: String,
    val flagUrl: String
)

enum class AppLanguage {
    EN, FA, RU, ZH
}

private fun tr(lang: AppLanguage, key: String): String {
    return when (lang) {
        AppLanguage.EN -> when (key) {
            "app_title" -> "Cloudflare Advanced Scanner"
            "app_subtitle" -> "Choose language, region, and scan count to begin"
            "language" -> "Language"
            "region" -> "Region"
            "scan_count" -> "Number of scans (100-10000)"
            "start" -> "Start"
            "made_with_love" -> "Made with love"
            "website" -> "Website"
            "github" -> "GitHub"
            "scanning_progress" -> "Scanning in Progress"
            "checked" -> "Checked"
            "of" -> "of"
            "ips" -> "IPs"
            "live_matrix_log" -> "Live Matrix Log"
            "top_clean" -> "Top Clean IPs"
            "port" -> "Port"
            "no_results" -> "No clean IPs found."
            "latency" -> "Latency"
            "status" -> "Status"
            "copy" -> "Copy"
            "scan_again" -> "Scan Again"
            "timeout_closed" -> "timeout/closed"
            else -> key
        }
        AppLanguage.FA -> when (key) {
            "app_title" -> "اسکنر پیشرفته کلادفلر"
            "app_subtitle" -> "زبان، منطقه و تعداد اسکن را انتخاب کنید"
            "language" -> "زبان"
            "region" -> "منطقه"
            "scan_count" -> "تعداد اسکن (100 تا 10000)"
            "start" -> "شروع"
            "made_with_love" -> "ساخته شده با عشق"
            "website" -> "وبسایت"
            "github" -> "گیت‌هاب"
            "scanning_progress" -> "اسکن در حال انجام"
            "checked" -> "بررسی شده"
            "of" -> "از"
            "ips" -> "آی‌پی"
            "live_matrix_log" -> "لاگ زنده ماتریکس"
            "top_clean" -> "بهترین آی‌پی‌های سالم"
            "port" -> "پورت"
            "no_results" -> "آی‌پی سالم پیدا نشد."
            "latency" -> "تاخیر"
            "status" -> "وضعیت"
            "copy" -> "کپی"
            "scan_again" -> "اسکن دوباره"
            "timeout_closed" -> "تایم‌اوت/بسته"
            else -> key
        }
        AppLanguage.RU -> when (key) {
            "app_title" -> "Продвинутый сканер Cloudflare"
            "app_subtitle" -> "Выберите язык, регион и количество сканов"
            "language" -> "Язык"
            "region" -> "Регион"
            "scan_count" -> "Количество сканов (100-10000)"
            "start" -> "Старт"
            "made_with_love" -> "Сделано с любовью"
            "website" -> "Сайт"
            "github" -> "GitHub"
            "scanning_progress" -> "Сканирование выполняется"
            "checked" -> "Проверено"
            "of" -> "из"
            "ips" -> "IP"
            "live_matrix_log" -> "Живой Matrix лог"
            "top_clean" -> "Лучшие чистые IP"
            "port" -> "Порт"
            "no_results" -> "Чистые IP не найдены."
            "latency" -> "Задержка"
            "status" -> "Статус"
            "copy" -> "Копировать"
            "scan_again" -> "Сканировать снова"
            "timeout_closed" -> "тайм-аут/закрыт"
            else -> key
        }
        AppLanguage.ZH -> when (key) {
            "app_title" -> "Cloudflare 高级扫描器"
            "app_subtitle" -> "请选择语言、地区和扫描数量"
            "language" -> "语言"
            "region" -> "地区"
            "scan_count" -> "扫描数量 (100-10000)"
            "start" -> "开始"
            "made_with_love" -> "用爱制作"
            "website" -> "网站"
            "github" -> "GitHub"
            "scanning_progress" -> "正在扫描"
            "checked" -> "已检查"
            "of" -> "/"
            "ips" -> "IP"
            "live_matrix_log" -> "实时矩阵日志"
            "top_clean" -> "最佳可用 IP"
            "port" -> "端口"
            "no_results" -> "未找到可用 IP。"
            "latency" -> "延迟"
            "status" -> "状态"
            "copy" -> "复制"
            "scan_again" -> "重新扫描"
            "timeout_closed" -> "超时/关闭"
            else -> key
        }
    }
}

private fun localizedRegions(lang: AppLanguage): List<RegionItem> {
    return when (lang) {
        AppLanguage.EN -> listOf(
            RegionItem("china", "China (Optimized)", "https://flagcdn.com/w80/cn.png"),
            RegionItem("global", "Global", "https://flagcdn.com/w80/un.png"),
            RegionItem("russia", "Russia", "https://flagcdn.com/w80/ru.png")
        )
        AppLanguage.FA -> listOf(
            RegionItem("china", "چین (بهینه)", "https://flagcdn.com/w80/cn.png"),
            RegionItem("global", "جهانی", "https://flagcdn.com/w80/un.png"),
            RegionItem("russia", "روسیه", "https://flagcdn.com/w80/ru.png")
        )
        AppLanguage.RU -> listOf(
            RegionItem("china", "Китай (оптим.)", "https://flagcdn.com/w80/cn.png"),
            RegionItem("global", "Глобальный", "https://flagcdn.com/w80/un.png"),
            RegionItem("russia", "Россия", "https://flagcdn.com/w80/ru.png")
        )
        AppLanguage.ZH -> listOf(
            RegionItem("china", "中国（优化）", "https://flagcdn.com/w80/cn.png"),
            RegionItem("global", "全球", "https://flagcdn.com/w80/un.png"),
            RegionItem("russia", "俄罗斯", "https://flagcdn.com/w80/ru.png")
        )
    }
}

private fun getFlagUrlForRegion(region: String): String {
    return when (region) {
        "china" -> "https://flagcdn.com/w80/cn.png"
        "russia" -> "https://flagcdn.com/w80/ru.png"
        else -> "https://flagcdn.com/w80/un.png"
    }
}

@Composable
fun ScannerApp(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(AppScreen.START) }
    var selectedRegion by remember { mutableStateOf("china") }
    var appLanguage by remember { mutableStateOf(detectInitialLanguage()) }
    var totalScans by remember { mutableStateOf(500) }
    var port by remember { mutableStateOf(443) }
    var sessionKey by remember { mutableStateOf(0) }
    var currentScan by remember { mutableStateOf(0) }
    var results by remember { mutableStateOf(emptyList<ScanResult>()) }
    var scanLogs by remember { mutableStateOf(emptyList<String>()) }
    val layoutDirection = if (appLanguage == AppLanguage.FA) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        when (screen) {
            AppScreen.START -> ScannerStartScreen(
                modifier = modifier,
                language = appLanguage,
                onLanguageChange = { appLanguage = it },
                onStart = { region, count ->
                    selectedRegion = region
                    totalScans = count
                    port = 443
                    results = emptyList()
                    scanLogs = emptyList()
                    currentScan = 0
                    sessionKey++
                    screen = AppScreen.SCANNING
                }
            )

            AppScreen.SCANNING -> {
                ScanningProgressScreen(
                    modifier = modifier,
                    language = appLanguage,
                    region = selectedRegion,
                    current = currentScan,
                    total = totalScans,
                    logs = scanLogs
                )
                LaunchedEffect(sessionKey) {
                    val found = scanRegion(
                        region = selectedRegion,
                        count = totalScans,
                        port = port,
                        language = appLanguage,
                        onProgress = { currentScan = it },
                        onLog = { line ->
                            scanLogs = (scanLogs + line).takeLast(220)
                        }
                    )
                    results = found.sortedBy { it.latencyMs }.take(10)
                    screen = AppScreen.RESULTS
                }
            }

            AppScreen.RESULTS -> ScanResultsScreen(
                modifier = modifier,
                language = appLanguage,
                region = selectedRegion,
                port = port,
                results = results,
                onScanAgain = { screen = AppScreen.START }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerStartScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onStart: (region: String, count: Int) -> Unit
) {
    val topOrange = Color(0xFFF68A1E)
    val deepOrange = Color(0xFFD8591E)
    val charcoal = Color(0xFF2C3338)
    val green = Color(0xFF3BB155)
    val warmWhite = Color(0xFFFFF4E8)
    val uriHandler = LocalUriHandler.current

    val regions = localizedRegions(language)
    val languages = listOf(
        AppLanguage.EN to "English",
        AppLanguage.FA to "فارسی",
        AppLanguage.RU to "Русский",
        AppLanguage.ZH to "中文"
    )
    var languageExpanded by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var selectedRegion by remember { mutableStateOf(regions.first()) }
    var countText by remember { mutableStateOf("500") }
    LaunchedEffect(language) {
        selectedRegion = regions.firstOrNull { it.id == selectedRegion.id } ?: regions.first()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF8F2), warmWhite, Color(0xFFFFEEDC))
                )
            )
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .zIndex(10f)
        ) {
            Surface(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .clickable { languageExpanded = true },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.96f),
                shadowElevation = 10.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = tr(language, "language"),
                        tint = deepOrange
                    )
                }
            }
            DropdownMenu(
                expanded = languageExpanded,
                onDismissRequest = { languageExpanded = false }
            ) {
                languages.forEach { (langKey, langLabel) ->
                    DropdownMenuItem(
                        text = { Text(langLabel) },
                        onClick = {
                            onLanguageChange(langKey)
                            languageExpanded = false
                        }
                    )
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(34.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(topOrange, deepOrange)
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(34.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(130.dp)
                        .background(Color.White.copy(alpha = 0.15f), CircleShape)
                        .border(2.dp, Color.White.copy(alpha = 0.30f), CircleShape)
                        .padding(18.dp)
                ) {
                    val lensRadius = size.minDimension * 0.30f
                    val lensCenter = Offset(size.width * 0.45f, size.height * 0.40f)
                    val strokeWidth = 9f

                    drawCircle(
                        color = charcoal,
                        radius = lensRadius,
                        center = lensCenter,
                        style = Stroke(width = strokeWidth)
                    )

                    drawLine(
                        color = charcoal,
                        start = Offset(
                            x = lensCenter.x + lensRadius * 0.65f,
                            y = lensCenter.y + lensRadius * 0.65f
                        ),
                        end = Offset(
                            x = size.width * 0.88f,
                            y = size.height * 0.90f
                        ),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )

                    drawLine(
                        color = green,
                        start = Offset(size.width * 0.32f, size.height * 0.44f),
                        end = Offset(size.width * 0.44f, size.height * 0.58f),
                        strokeWidth = 11f,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = green,
                        start = Offset(size.width * 0.44f, size.height * 0.58f),
                        end = Offset(size.width * 0.69f, size.height * 0.30f),
                        strokeWidth = 11f,
                        cap = StrokeCap.Round
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                Text(
                    text = tr(language, "app_title"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = charcoal
                )
                Text(
                    text = tr(language, "app_subtitle"),
                    fontSize = 13.sp,
                    color = charcoal.copy(alpha = 0.75f)
                )

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedRegion.label,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        label = { Text(tr(language, "region")) },
                        prefix = {
                            Icon(
                                imageVector = Icons.Rounded.Public,
                                contentDescription = null,
                                tint = deepOrange
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        },
                        leadingIcon = {
                            AsyncImage(
                                model = selectedRegion.flagUrl,
                                contentDescription = selectedRegion.label,
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = topOrange,
                            unfocusedBorderColor = charcoal.copy(alpha = 0.45f),
                            focusedLabelColor = deepOrange,
                            unfocusedLabelColor = charcoal,
                            focusedTextColor = charcoal,
                            unfocusedTextColor = charcoal,
                            focusedTrailingIconColor = topOrange,
                            unfocusedTrailingIconColor = charcoal
                        ),
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        }
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        regions.forEach { region ->
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        AsyncImage(
                                            model = region.flagUrl,
                                            contentDescription = region.label,
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(RoundedCornerShape(5.dp))
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(region.label)
                                    }
                                },
                                onClick = {
                                    selectedRegion = region
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = countText,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() } || input.isEmpty()) {
                            countText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(tr(language, "scan_count")) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = deepOrange
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = topOrange,
                        unfocusedBorderColor = charcoal.copy(alpha = 0.45f),
                        focusedLabelColor = deepOrange,
                        unfocusedLabelColor = charcoal,
                        focusedTextColor = charcoal,
                        unfocusedTextColor = charcoal,
                        cursorColor = deepOrange
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    singleLine = true
                )

                Button(
                    onClick = {
                        val count = countText.toIntOrNull()?.coerceIn(100, 10000) ?: 500
                        onStart(selectedRegion.label, count)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = topOrange,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = tr(language, "start"),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${tr(language, "made_with_love")} ",
                        color = charcoal,
                        fontSize = 13.sp
                    )
                    AsyncImage(
                        model = "https://cdn.jsdelivr.net/gh/twitter/twemoji@14.0.2/assets/72x72/2764.png",
                        contentDescription = "heart",
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = " Murtaza Akbari",
                        color = charcoal,
                        fontSize = 13.sp
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { uriHandler.openUri("https://murtaza.website/") }
                    ) {
                        Text(tr(language, "website"), color = deepOrange, fontWeight = FontWeight.SemiBold)
                    }
                    Text("•", color = charcoal.copy(alpha = 0.6f))
                    TextButton(
                        onClick = { uriHandler.openUri("https://github.com/Murtaza-codes") }
                    ) {
                        Text(tr(language, "github"), color = deepOrange, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun ScanningProgressScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    region: String,
    current: Int,
    total: Int,
    logs: List<String>
) {
    val topOrange = Color(0xFFF68A1E)
    val deepOrange = Color(0xFFD8591E)
    val charcoal = Color(0xFF2C3338)
    val warmWhite = Color(0xFFFFF4E8)
    val green = Color(0xFF3BB155)
    val progress = if (total == 0) 0f else current.toFloat() / total.toFloat()
    val percent = (progress * 100).toInt()
    val flagUrl = getFlagUrlForRegion(region)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(warmWhite)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp)),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(topOrange.copy(alpha = 0.12f), Color.White)
                        )
                    )
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = tr(language, "scanning_progress"),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = charcoal
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AsyncImage(
                        model = flagUrl,
                        contentDescription = region,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                    )
                    Text(
                        text = "${tr(language, "region")}: ${localizedRegions(language).firstOrNull { it.id == region }?.label ?: region}",
                        fontSize = 16.sp,
                        color = charcoal
                    )
                }
                AndroidView(
                    modifier = Modifier.size(160.dp),
                    factory = { ctx ->
                        CircularProgressBar(ctx).apply {
                            val density = ctx.resources.displayMetrics.density
                            progressMax = 100f
                            progressBarWidth = 14f * density
                            backgroundProgressBarWidth = 14f * density
                            roundBorder = true
                            progressBarColor = android.graphics.Color.parseColor("#3BB155")
                            backgroundProgressBarColor = android.graphics.Color.parseColor("#F6D1B0")
                            setProgressWithAnimation(percent.toFloat(), 250)
                        }
                    },
                    update = { view ->
                        view.setProgressWithAnimation(percent.toFloat(), 150)
                    }
                )
                Text(
                    text = "$percent%",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = deepOrange
                )
                Text(
                    text = "${tr(language, "checked")} $current ${tr(language, "of")} $total ${tr(language, "ips")}",
                    fontSize = 16.sp,
                    color = charcoal
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = green,
                    trackColor = topOrange.copy(alpha = 0.25f)
                )

                Text(
                    text = tr(language, "live_matrix_log"),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = charcoal
                )
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF101414)
                    )
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(logs) { line ->
                            val lineColor = when {
                                line.startsWith("[OK]") -> Color(0xFF63E07C)
                                line.startsWith("[FAIL]") -> Color(0xFFFF7E67)
                                else -> Color(0xFFB8FFC5)
                            }
                            Text(
                                text = line,
                                color = lineColor,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ScanResultsScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    region: String,
    port: Int,
    results: List<ScanResult>,
    onScanAgain: () -> Unit
) {
    val topOrange = Color(0xFFF68A1E)
    val deepOrange = Color(0xFFD8591E)
    val charcoal = Color(0xFF2C3338)
    val warmWhite = Color(0xFFFFF4E8)
    val green = Color(0xFF3BB155)
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(warmWhite)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = tr(language, "top_clean"),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = charcoal
        )
        Text(
            text = "${tr(language, "region")}: ${localizedRegions(language).firstOrNull { it.id == region }?.label ?: region} | ${tr(language, "port")}: $port",
            fontSize = 15.sp,
            color = charcoal
        )

        if (results.isEmpty()) {
            Text(
                text = tr(language, "no_results"),
                fontSize = 16.sp,
                color = charcoal
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(results) { result ->
                    val latencyColor = when {
                        result.latencyMs < 150.0 -> green
                        result.latencyMs < 300.0 -> topOrange
                        else -> deepOrange
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.White
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = result.ip,
                                    fontWeight = FontWeight.Bold,
                                    color = charcoal
                                )
                                Text(
                                    text = "${tr(language, "latency")}: ${result.latencyMs} ms",
                                    color = latencyColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${tr(language, "status")}: ${tr(language, "port")} $port",
                                    color = charcoal
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    val clipboard =
                                        context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("ip_port", "${result.ip}:$port")
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = topOrange,
                                    contentColor = Color.Black
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(tr(language, "copy"))
                            }
                        }
                    }
                }
            }
        }

        Button(
            onClick = onScanAgain,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = topOrange,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = tr(language, "scan_again"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private val regionSubnets = mapOf(
    "china" to listOf("104.16", "104.17", "104.18", "104.19", "172.64", "172.67", "162.159", "104.20"),
    "global" to listOf("173.245", "103.21", "103.22", "103.31", "104.15", "141.101", "108.162", "190.93", "188.114", "197.234", "198.41", "162.158"),
    "russia" to listOf("104.21", "172.67", "188.114", "104.16", "162.159")
)

private fun generateIps(region: String, count: Int): List<String> {
    val subnets = regionSubnets[region] ?: regionSubnets.getValue("global")
    val ips = mutableSetOf<String>()
    while (ips.size < count) {
        val subnet = subnets.random()
        val ip = "$subnet.${Random.nextInt(0, 256)}.${Random.nextInt(1, 255)}"
        ips.add(ip)
    }
    return ips.toList()
}

private suspend fun scanRegion(
    region: String,
    count: Int,
    port: Int,
    language: AppLanguage,
    onProgress: (Int) -> Unit,
    onLog: (String) -> Unit
): List<ScanResult> = coroutineScope {
    val ips = generateIps(region, count)
    var processed = 0
    val deferred = ips.map { ip ->
        async(Dispatchers.IO) {
            val result = scanIp(ip, port)
            withContext(Dispatchers.Main) {
                synchronized(this@coroutineScope) {
                    processed++
                    onProgress(processed)
                    if (result != null) {
                        onLog("[OK]  ${result.ip}:$port  ${result.latencyMs} ms")
                    } else {
                        onLog("[FAIL] $ip:$port  ${tr(language, "timeout_closed")}")
                    }
                }
            }
            result
        }
    }
    deferred.awaitAll().filterNotNull()
}

private fun scanIp(ip: String, port: Int): ScanResult? {
    val start = System.nanoTime()
    return try {
        Socket().use { socket ->
            socket.soTimeout = 1200
            socket.connect(InetSocketAddress(ip, port), 1200)
        }
        val latency = ((System.nanoTime() - start) / 1_000_000.0)
        ScanResult(ip = ip, latencyMs = String.format("%.2f", latency).toDouble())
    } catch (_: Exception) {
        null
    }
}

@Preview(showBackground = true)
@Composable
fun ScannerStartScreenPreview() {
    CFScannerTheme(darkTheme = true, dynamicColor = false) {
        ScannerApp()
    }
}