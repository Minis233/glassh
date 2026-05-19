package com.minis.glassh.ssh

/**
 * Single shell script run on the remote VPS to collect everything we need
 * in a single SSH exec round-trip. Output is a sectioned key/value stream:
 *
 *   ===SECTION:hostname===
 *   somehost
 *   ===SECTION:uname===
 *   Linux ...
 *   ===END===
 *
 * The client splits on `===SECTION:` markers and parses per section.
 *
 * Designed to be portable: only POSIX shell + coreutils, no python/awk
 * tricks beyond what busybox provides. Anything missing is silently skipped.
 */
internal object RemoteScript {
    const val MARKER_PREFIX = "===SECTION:"
    const val MARKER_SUFFIX = "==="
    const val END_MARKER = "===END==="

    val script: String = """
        emit() { printf '${MARKER_PREFIX}%s${MARKER_SUFFIX}\n' "${'$'}1"; }

        emit hostname
        hostname 2>/dev/null || cat /proc/sys/kernel/hostname 2>/dev/null

        emit uname
        uname -a 2>/dev/null

        emit os_release
        cat /etc/os-release 2>/dev/null

        emit uptime
        cat /proc/uptime 2>/dev/null

        emit loadavg
        cat /proc/loadavg 2>/dev/null

        emit cpuinfo
        grep -E '^(model name|Hardware|Processor|cpu model|processor)' /proc/cpuinfo 2>/dev/null | head -n 64

        emit cpustat1
        head -n 1 /proc/stat 2>/dev/null
        sleep 1
        emit cpustat2
        head -n 1 /proc/stat 2>/dev/null

        emit thermal
        for z in /sys/class/thermal/thermal_zone*; do
            [ -r "${'$'}z/temp" ] || continue
            t=$(cat "${'$'}z/temp" 2>/dev/null)
            n=$(cat "${'$'}z/type" 2>/dev/null)
            printf '%s\t%s\n' "${'$'}n" "${'$'}t"
        done

        emit hwmon
        for h in /sys/class/hwmon/hwmon*; do
            [ -d "${'$'}h" ] || continue
            name=$(cat "${'$'}h/name" 2>/dev/null)
            for f in "${'$'}h"/temp*_input; do
                [ -r "${'$'}f" ] || continue
                base=${'$'}{f%_input}
                label=$(cat "${'$'}{base}_label" 2>/dev/null)
                t=$(cat "${'$'}f" 2>/dev/null)
                printf '%s|%s\t%s\n' "${'$'}name" "${'$'}label" "${'$'}t"
            done
        done

        emit meminfo
        grep -E '^(MemTotal|MemAvailable|MemFree|Buffers|Cached|SwapTotal|SwapFree):' /proc/meminfo 2>/dev/null

        emit disks
        df -P -k 2>/dev/null | tail -n +2

        emit netdev
        cat /proc/net/dev 2>/dev/null

        emit procs
        ls -1 /proc 2>/dev/null | grep -c '^[0-9]'

        emit publicip
        curl -fsS --max-time 3 https://api.ipify.org 2>/dev/null || true

        emit done
        printf '${END_MARKER}\n'
    """.trimIndent()
}
