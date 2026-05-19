package com.minis.glassh.ssh

import com.minis.glassh.model.DiskInfo
import com.minis.glassh.model.VpsStats

/** Parses the sectioned output of [RemoteScript] into a [VpsStats]. */
internal object StatsParser {

    fun parse(raw: String): VpsStats {
        val sections = splitSections(raw)
        return VpsStats(
            hostname = sections["hostname"]?.trim()?.takeIf { it.isNotEmpty() },
            kernel = sections["uname"]?.trim()?.takeIf { it.isNotEmpty() },
            os = parseOsRelease(sections["os_release"].orEmpty()),
            uptime = parseUptime(sections["uptime"].orEmpty()),
            loadAvg = parseLoadAvg(sections["loadavg"].orEmpty()),
            cpuModel = parseCpuModel(sections["cpuinfo"].orEmpty()),
            cpuCores = parseCpuCores(sections["cpuinfo"].orEmpty()),
            cpuUsagePercent = parseCpuUsage(
                sections["cpustat1"].orEmpty(),
                sections["cpustat2"].orEmpty()
            ),
            temperatures = parseTemperatures(
                sections["thermal"].orEmpty(),
                sections["hwmon"].orEmpty()
            ),
            memTotalKb = parseMem(sections["meminfo"].orEmpty(), "MemTotal"),
            memAvailableKb = parseMem(sections["meminfo"].orEmpty(), "MemAvailable")
                ?: estimateAvailable(sections["meminfo"].orEmpty()),
            swapTotalKb = parseMem(sections["meminfo"].orEmpty(), "SwapTotal"),
            swapFreeKb = parseMem(sections["meminfo"].orEmpty(), "SwapFree"),
            disks = parseDisks(sections["disks"].orEmpty()),
            netRxBytes = parseNet(sections["netdev"].orEmpty()).first,
            netTxBytes = parseNet(sections["netdev"].orEmpty()).second,
            processCount = sections["procs"]?.trim()?.toIntOrNull(),
            publicIp = sections["publicip"]?.trim()?.takeIf { it.isNotEmpty() && !it.contains(' ') },
        )
    }

    private fun splitSections(raw: String): Map<String, String> {
        val out = mutableMapOf<String, String>()
        var current: String? = null
        val buf = StringBuilder()
        for (line in raw.lineSequence()) {
            if (line.startsWith(RemoteScript.MARKER_PREFIX)) {
                current?.let { out[it] = buf.toString() }
                buf.setLength(0)
                val key = line.removePrefix(RemoteScript.MARKER_PREFIX)
                    .removeSuffix(RemoteScript.MARKER_SUFFIX)
                current = key
            } else if (line == RemoteScript.END_MARKER) {
                current?.let { out[it] = buf.toString() }
                current = null
                buf.setLength(0)
            } else {
                buf.appendLine(line)
            }
        }
        current?.let { out[it] = buf.toString() }
        return out
    }

    private fun parseOsRelease(s: String): String? {
        val m = Regex("""^PRETTY_NAME="?([^"\n]+)"?""", RegexOption.MULTILINE).find(s)
        return m?.groupValues?.get(1)?.trim()
    }

    private fun parseUptime(s: String): String? {
        val secs = s.trim().substringBefore(' ').toDoubleOrNull() ?: return null
        return formatUptime(secs.toLong())
    }

    private fun formatUptime(seconds: Long): String {
        val d = seconds / 86400
        val h = (seconds % 86400) / 3600
        val m = (seconds % 3600) / 60
        return buildString {
            if (d > 0) append("${d}d ")
            if (h > 0 || d > 0) append("${h}h ")
            append("${m}m")
        }
    }

    private fun parseLoadAvg(s: String): Triple<Double, Double, Double>? {
        val parts = s.trim().split(' ')
        if (parts.size < 3) return null
        val a = parts[0].toDoubleOrNull() ?: return null
        val b = parts[1].toDoubleOrNull() ?: return null
        val c = parts[2].toDoubleOrNull() ?: return null
        return Triple(a, b, c)
    }

    private fun parseCpuModel(s: String): String? {
        // Prefer "model name", fall back to "Hardware" / "Processor" / "cpu model"
        listOf("model name", "Hardware", "cpu model", "Processor").forEach { key ->
            val m = Regex("""^${Regex.escape(key)}\s*:\s*(.+)$""", RegexOption.MULTILINE).find(s)
            if (m != null) return m.groupValues[1].trim()
        }
        return null
    }

    private fun parseCpuCores(s: String): Int? {
        val count = Regex("""^processor\s*:""", RegexOption.MULTILINE).findAll(s).count()
        return count.takeIf { it > 0 }
    }

    private fun parseCpuUsage(a: String, b: String): Float? {
        val (ai, at) = parseCpuStatLine(a) ?: return null
        val (bi, bt) = parseCpuStatLine(b) ?: return null
        val totalDelta = bt - at
        val idleDelta = bi - ai
        if (totalDelta <= 0) return null
        val usage = (1.0 - idleDelta.toDouble() / totalDelta.toDouble()) * 100.0
        return usage.toFloat().coerceIn(0f, 100f)
    }

    /** @return Pair(idle+iowait, total) jiffies, or null. */
    private fun parseCpuStatLine(line: String): Pair<Long, Long>? {
        val cleaned = line.trim()
        if (!cleaned.startsWith("cpu")) return null
        val parts = cleaned.split(Regex("\\s+"))
        if (parts.size < 5) return null
        val nums = parts.drop(1).mapNotNull { it.toLongOrNull() }
        if (nums.size < 4) return null
        val idle = nums.getOrElse(3) { 0L } + nums.getOrElse(4) { 0L }
        val total = nums.sum()
        return idle to total
    }

    private fun parseTemperatures(thermal: String, hwmon: String): Map<String, Float> {
        val out = linkedMapOf<String, Float>()
        thermal.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split('\t')
            if (parts.size < 2) return@forEach
            val name = parts[0].ifBlank { "thermal" }
            val v = parts[1].toLongOrNull() ?: return@forEach
            // /sys/class/thermal value is millidegrees (sometimes °C if very low values)
            val celsius = if (v > 1000) v / 1000f else v.toFloat()
            if (celsius in -10f..150f) out["thermal:$name"] = celsius
        }
        hwmon.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val tabSplit = line.split('\t')
            if (tabSplit.size < 2) return@forEach
            val left = tabSplit[0]
            val v = tabSplit[1].toLongOrNull() ?: return@forEach
            val (name, label) = left.split('|', limit = 2).let {
                (it.getOrNull(0) ?: "hwmon") to (it.getOrNull(1) ?: "")
            }
            val celsius = if (v > 1000) v / 1000f else v.toFloat()
            if (celsius !in -10f..150f) return@forEach
            val key = if (label.isNotBlank()) "$name:$label" else name
            // keep highest reading per key
            val cur = out[key]
            if (cur == null || celsius > cur) out[key] = celsius
        }
        return out
    }

    private fun parseMem(s: String, key: String): Long? {
        val m = Regex("""^${Regex.escape(key)}:\s+(\d+)\s+kB""", RegexOption.MULTILINE).find(s)
        return m?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun estimateAvailable(s: String): Long? {
        val free = parseMem(s, "MemFree") ?: return null
        val buffers = parseMem(s, "Buffers") ?: 0
        val cached = parseMem(s, "Cached") ?: 0
        return free + buffers + cached
    }

    private fun parseDisks(s: String): List<DiskInfo> {
        val out = mutableListOf<DiskInfo>()
        s.lineSequence().forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val parts = line.split(Regex("\\s+"))
            if (parts.size < 6) return@forEach
            val source = parts[0]
            val sizeKb = parts[1].toLongOrNull() ?: return@forEach
            val usedKb = parts[2].toLongOrNull() ?: return@forEach
            val availKb = parts[3].toLongOrNull() ?: return@forEach
            val pctRaw = parts[4].removeSuffix("%").toFloatOrNull() ?: return@forEach
            val mount = parts.drop(5).joinToString(" ")
            // Skip pseudo / overlay / tmpfs etc. but keep / and named mounts that look real.
            if (source.startsWith("tmpfs") || source.startsWith("devtmpfs") || source == "overlay") return@forEach
            if (mount.startsWith("/proc") || mount.startsWith("/sys") || mount.startsWith("/run") ||
                mount.startsWith("/dev") || mount.startsWith("/snap") || mount.startsWith("/var/lib/docker")
            ) return@forEach
            out += DiskInfo(
                mount = mount,
                sizeHuman = humanKb(sizeKb),
                usedHuman = humanKb(usedKb),
                availHuman = humanKb(availKb),
                usedPercent = pctRaw,
            )
        }
        return out.sortedByDescending { it.usedPercent }
    }

    private fun humanKb(kb: Long): String {
        var v = kb.toDouble() * 1024.0
        val units = listOf("B", "KB", "MB", "GB", "TB", "PB")
        var i = 0
        while (v >= 1024 && i < units.lastIndex) {
            v /= 1024.0
            i++
        }
        return "%.1f %s".format(v, units[i])
    }

    private fun parseNet(s: String): Pair<Long?, Long?> {
        var rx = 0L
        var tx = 0L
        var found = false
        s.lineSequence().drop(2).forEach { raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEach
            val colon = line.indexOf(':')
            if (colon < 0) return@forEach
            val name = line.substring(0, colon).trim()
            if (name == "lo" || name.startsWith("docker") || name.startsWith("br-") ||
                name.startsWith("veth") || name.startsWith("virbr")
            ) return@forEach
            val parts = line.substring(colon + 1).trim().split(Regex("\\s+"))
            if (parts.size < 16) return@forEach
            val r = parts[0].toLongOrNull() ?: return@forEach
            val t = parts[8].toLongOrNull() ?: return@forEach
            rx += r
            tx += t
            found = true
        }
        return if (found) rx to tx else null to null
    }
}
