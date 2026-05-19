package com.minis.glassh.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeviceThermostat
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minis.glassh.model.DiskInfo
import com.minis.glassh.model.VpsStats
import kotlin.math.max

@Composable
fun DashboardScreen(vm: GlasshViewModel) {
    val state by vm.dashboard.collectAsState()
    val host = state.host ?: return

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(top = 56.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BackPill(onClick = { vm.back() })
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    host.name.ifBlank { host.host },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "${host.user}@${host.host}:${host.port}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            RefreshPill(loading = state.loading, onClick = { vm.manualRefresh() })
        }

        AnimatedVisibility(visible = state.error != null) {
            ErrorBanner(state.error.orEmpty(), onRetry = { vm.manualRefresh() })
        }

        val stats = state.stats
        if (stats == null) {
            LoadingPlaceholder(loading = state.loading)
        } else {
            HeroCard(stats)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(Modifier.weight(1f)) { CpuCard(stats) }
                Box(Modifier.weight(1f)) { TempCard(stats) }
            }
            MemoryCard(stats)
            DiskCard(stats.disks)
            NetworkCard(stats, state.previousStats)
            FooterCard(stats)
        }
    }
}

@Composable
private fun RefreshPill(loading: Boolean, onClick: () -> Unit) {
    GlassSurface(
        shape = CircleShape,
        tintAlpha = 0.22f,
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val rotation by animateFloatAsState(
                targetValue = if (loading) 720f else 0f,
                animationSpec = tween(durationMillis = if (loading) 1200 else 0),
                label = "spin",
            )
            Icon(
                Icons.Outlined.Refresh,
                contentDescription = "Refresh",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(rotation),
            )
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onRetry: () -> Unit) {
    GlassSurface(
        shape = RoundedCornerShape(18.dp),
        tintAlpha = 0.22f,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Connection error",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFFF8B8B),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                    .clickable { onRetry() }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    "Retry",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun LoadingPlaceholder(loading: Boolean) {
    GlassSurface(modifier = Modifier.fillMaxWidth().height(220.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (loading) "Connecting & collecting metrics…" else "No data yet",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun HeroCard(stats: VpsStats) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stats.hostname ?: "—",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            stats.os?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            }
            stats.kernel?.let {
                Text(
                    it,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill(label = "Uptime", value = stats.uptime ?: "—")
                stats.loadAvg?.let { (a, b, c) ->
                    Pill(label = "Load", value = "%.2f %.2f %.2f".format(a, b, c))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                stats.publicIp?.let { Pill(label = "Public IP", value = it) }
                stats.processCount?.let { Pill(label = "Procs", value = it.toString()) }
            }
        }
    }
}

@Composable
private fun Pill(label: String, value: String) {
    GlassSurface(shape = RoundedCornerShape(14.dp), tintAlpha = 0.18f) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun CpuCard(stats: VpsStats) {
    val pct = stats.cpuUsagePercent ?: 0f
    GadgetCard(
        icon = Icons.Outlined.Speed,
        title = "CPU",
        subtitle = stats.cpuModel ?: stats.cpuCores?.let { "$it cores" } ?: "—",
    ) {
        Ring(
            percent = pct,
            colors = listOf(Color(0xFF7B5BFF), Color(0xFF5BA8FF)),
            centerLabel = "%.0f%%".format(pct),
            centerSubLabel = stats.cpuCores?.let { "$it cores" }
        )
    }
}

@Composable
private fun TempCard(stats: VpsStats) {
    val maxTemp = stats.maxTempCelsius
    val pct = if (maxTemp != null) ((maxTemp / 100f) * 100f).coerceIn(0f, 100f) else 0f
    val color = when {
        maxTemp == null -> Color(0xFF6F7AA0)
        maxTemp < 55 -> Color(0xFF06D6A0)
        maxTemp < 75 -> Color(0xFFFFC857)
        else -> Color(0xFFFF6B6B)
    }
    GadgetCard(
        icon = Icons.Outlined.DeviceThermostat,
        title = "Temperature",
        subtitle = if (stats.temperatures.isEmpty()) "No sensors detected" else "${stats.temperatures.size} sensor${if (stats.temperatures.size == 1) "" else "s"}",
    ) {
        Ring(
            percent = pct,
            colors = listOf(color, color.copy(alpha = 0.6f)),
            centerLabel = if (maxTemp != null) "%.0f°".format(maxTemp) else "—",
            centerSubLabel = "max",
        )
        if (stats.temperatures.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            stats.temperatures.entries.take(3).forEach { (name, temp) ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name.shorten(),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "%.1f°C".format(temp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }
        }
    }
}

private fun String.shorten(): String =
    substringAfterLast(':').substringAfterLast('/').take(18)

@Composable
private fun MemoryCard(stats: VpsStats) {
    val memPct = stats.memUsagePercent ?: 0f
    val swapPct = run {
        val t = stats.swapTotalKb ?: 0L
        val f = stats.swapFreeKb ?: 0L
        if (t <= 0) 0f else ((t - f).toFloat() / t.toFloat() * 100f)
    }
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Memory)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Memory",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(14.dp))
            Bar(
                label = "RAM",
                trailing = "${humanKb(stats.memTotalKb?.minus(stats.memAvailableKb ?: 0L))} / ${humanKb(stats.memTotalKb)}",
                percent = memPct,
                colors = listOf(Color(0xFF7B5BFF), Color(0xFF5BA8FF)),
            )
            if ((stats.swapTotalKb ?: 0L) > 0L) {
                Spacer(Modifier.height(12.dp))
                Bar(
                    label = "Swap",
                    trailing = "${humanKb((stats.swapTotalKb ?: 0L) - (stats.swapFreeKb ?: 0L))} / ${humanKb(stats.swapTotalKb)}",
                    percent = swapPct.coerceIn(0f, 100f),
                    colors = listOf(Color(0xFFFF7BB3), Color(0xFFFFC857)),
                )
            }
        }
    }
}

@Composable
private fun DiskCard(disks: List<DiskInfo>) {
    if (disks.isEmpty()) return
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.Storage)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Disks",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(8.dp))
            disks.take(8).forEachIndexed { i, d ->
                if (i > 0) Spacer(Modifier.height(12.dp))
                Bar(
                    label = d.mount,
                    trailing = "${d.usedHuman} / ${d.sizeHuman}",
                    percent = d.usedPercent,
                    colors = when {
                        d.usedPercent < 70 -> listOf(Color(0xFF06D6A0), Color(0xFF5BA8FF))
                        d.usedPercent < 90 -> listOf(Color(0xFFFFC857), Color(0xFFFF7BB3))
                        else -> listOf(Color(0xFFFF6B6B), Color(0xFFFF7BB3))
                    },
                )
            }
        }
    }
}

@Composable
private fun NetworkCard(stats: VpsStats, prev: VpsStats?) {
    val rx = stats.netRxBytes
    val tx = stats.netTxBytes
    if (rx == null && tx == null) return
    val dt = prev?.let { (stats.collectedAt - it.collectedAt) / 1000.0 }?.takeIf { it > 0 }
    val rxRate = if (prev?.netRxBytes != null && rx != null && dt != null)
        max(0L, rx - prev.netRxBytes) / dt else null
    val txRate = if (prev?.netTxBytes != null && tx != null && dt != null)
        max(0L, tx - prev.netTxBytes) / dt else null

    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(Icons.Outlined.SwapVert)
                Spacer(Modifier.width(10.dp))
                Text(
                    "Network",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    NetTile(
                        title = "Down",
                        rate = rxRate,
                        total = rx,
                        accent = Color(0xFF06D6A0),
                    )
                }
                Box(Modifier.weight(1f)) {
                    NetTile(
                        title = "Up",
                        rate = txRate,
                        total = tx,
                        accent = Color(0xFFFF7BB3),
                    )
                }
            }
        }
    }
}

@Composable
private fun NetTile(title: String, rate: Double?, total: Long?, accent: Color) {
    GlassSurface(shape = RoundedCornerShape(18.dp), tintAlpha = 0.16f) {
        Column(Modifier.padding(14.dp)) {
            Text(
                title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                if (rate != null) humanRate(rate) else "—",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                "Total: ${if (total != null) humanBytes(total) else "—"}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun FooterCard(stats: VpsStats) {
    GlassSurface(modifier = Modifier.fillMaxWidth(), tintAlpha = 0.14f) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF06D6A0))
            )
            Spacer(Modifier.width(8.dp))
            Text(
                "Updated ${formatRelative(stats.collectedAt)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
    }
}

private fun formatRelative(ts: Long): String {
    val d = (System.currentTimeMillis() - ts) / 1000
    return when {
        d < 5 -> "just now"
        d < 60 -> "${d}s ago"
        d < 3600 -> "${d / 60}m ago"
        else -> "${d / 3600}h ago"
    }
}

@Composable
private fun GadgetCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
) {
    GlassSurface(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Text(
                        subtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                        maxLines = 2,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun IconBadge(icon: ImageVector) {
    GlassSurface(shape = CircleShape, tintAlpha = 0.32f, modifier = Modifier.size(34.dp)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun Ring(
    percent: Float,
    colors: List<Color>,
    centerLabel: String,
    centerSubLabel: String? = null,
) {
    val animPct by animateFloatAsState(
        targetValue = percent,
        animationSpec = tween(600),
        label = "ring",
    )
    Box(
        Modifier.size(120.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 12f
            val rect = androidx.compose.ui.geometry.Rect(
                offset = Offset(stroke, stroke),
                size = Size(size.width - stroke * 2, size.height - stroke * 2),
            )
            // Background track
            drawArc(
                color = Color.White.copy(alpha = 0.18f),
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // Progress arc
            drawArc(
                brush = Brush.sweepGradient(colors),
                startAngle = 135f,
                sweepAngle = 270f * (animPct / 100f).coerceIn(0f, 1f),
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                centerLabel,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            centerSubLabel?.let {
                Text(
                    it,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun Bar(label: String, trailing: String, percent: Float, colors: List<Color>) {
    val animPct by animateFloatAsState(percent.coerceIn(0f, 100f), tween(600), label = "bar")
    Column(Modifier.fillMaxWidth()) {
        Row {
            Text(
                label,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
            )
            Text(
                "${trailing}  •  %.0f%%".format(percent),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animPct / 100f)
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(colors))
            )
        }
    }
}

private fun humanKb(kb: Long?): String {
    if (kb == null || kb <= 0) return "—"
    return humanBytes(kb * 1024)
}

private fun humanBytes(b: Long): String {
    var v = b.toDouble()
    val units = listOf("B", "KB", "MB", "GB", "TB", "PB")
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return if (i == 0) "${b}${units[0]}" else "%.1f %s".format(v, units[i])
}

private fun humanRate(bytesPerSec: Double): String {
    var v = bytesPerSec * 8 // bits per sec
    val units = listOf("bps", "Kbps", "Mbps", "Gbps")
    var i = 0
    while (v >= 1000 && i < units.lastIndex) { v /= 1000; i++ }
    return "%.1f %s".format(v, units[i])
}
