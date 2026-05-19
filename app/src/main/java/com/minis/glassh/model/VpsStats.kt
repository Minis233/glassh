package com.minis.glassh.model

/** Live VPS metrics rendered on the dashboard. */
data class VpsStats(
    val hostname: String? = null,
    val kernel: String? = null,
    val os: String? = null,
    val uptime: String? = null,
    val loadAvg: Triple<Double, Double, Double>? = null,
    val cpuModel: String? = null,
    val cpuCores: Int? = null,
    val cpuUsagePercent: Float? = null,
    /** Map of sensor label → temperature °C. Empty when no sensors found. */
    val temperatures: Map<String, Float> = emptyMap(),
    val memTotalKb: Long? = null,
    val memAvailableKb: Long? = null,
    val swapTotalKb: Long? = null,
    val swapFreeKb: Long? = null,
    val disks: List<DiskInfo> = emptyList(),
    /** sum of received/sent bytes since boot, all interfaces except lo. */
    val netRxBytes: Long? = null,
    val netTxBytes: Long? = null,
    val publicIp: String? = null,
    val processCount: Int? = null,
    /** Snapshot wall-clock time at the moment the stats were collected. */
    val collectedAt: Long = System.currentTimeMillis(),
) {
    val memUsagePercent: Float?
        get() {
            val total = memTotalKb ?: return null
            val avail = memAvailableKb ?: return null
            if (total <= 0) return null
            return ((total - avail).toFloat() / total.toFloat() * 100f).coerceIn(0f, 100f)
        }

    val maxTempCelsius: Float?
        get() = temperatures.values.maxOrNull()
}

data class DiskInfo(
    val mount: String,
    val sizeHuman: String,
    val usedHuman: String,
    val availHuman: String,
    val usedPercent: Float,
)
