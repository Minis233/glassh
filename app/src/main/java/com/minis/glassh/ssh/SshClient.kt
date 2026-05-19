package com.minis.glassh.ssh

import com.jcraft.jsch.ChannelExec
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.UserInfo
import com.minis.glassh.model.AuthMethod
import com.minis.glassh.model.HostConfig
import com.minis.glassh.model.VpsStats
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.security.MessageDigest

/** Thin coroutine-friendly wrapper around JSch focused on running a single
 *  metric-collection script. SSH terminal sessions are out of scope for v0.1. */
class SshClient(private val host: HostConfig) {

    suspend fun fetchStats(): VpsStats = withContext(Dispatchers.IO) {
        val session = openSession()
        try {
            val raw = exec(session, RemoteScript.script)
            StatsParser.parse(raw)
        } finally {
            runCatching { session.disconnect() }
        }
    }

    /** Run an arbitrary command and return stdout (stderr is captured but discarded). */
    suspend fun runCommand(cmd: String): String = withContext(Dispatchers.IO) {
        val session = openSession()
        try {
            exec(session, cmd)
        } finally {
            runCatching { session.disconnect() }
        }
    }

    /** Compute remote host key fingerprint after a connect attempt. */
    suspend fun probeHostKey(): String? = withContext(Dispatchers.IO) {
        val session = openSession(probeOnly = true)
        try {
            session.hostKey?.let { fingerprintFromBase64(it.key) }
        } finally {
            runCatching { session.disconnect() }
        }
    }

    private fun openSession(probeOnly: Boolean = false): Session {
        val jsch = JSch()
        if (host.auth == AuthMethod.KEY && host.privateKey.isNotBlank()) {
            val keyBytes = host.privateKey.toByteArray(Charsets.UTF_8)
            val passphraseBytes = host.keyPassphrase.takeIf { it.isNotEmpty() }?.toByteArray(Charsets.UTF_8)
            jsch.addIdentity("glassh-${host.id}", keyBytes, null, passphraseBytes)
        }
        val session = jsch.getSession(host.user, host.host, host.port)
        if (host.auth == AuthMethod.PASSWORD) {
            session.setPassword(host.password)
        }
        session.userInfo = SilentUserInfo(host.password)
        // Compatibility: enable a wide algorithm set for old VPS images.
        session.setConfig("PreferredAuthentications", "publickey,password,keyboard-interactive")
        session.setConfig("server_host_key",
            "ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521")
        session.setConfig("PubkeyAcceptedAlgorithms",
            "ssh-ed25519,rsa-sha2-512,rsa-sha2-256,ssh-rsa,ecdsa-sha2-nistp256,ecdsa-sha2-nistp384,ecdsa-sha2-nistp521")
        // Host key handling: TOFU when no pin, strict when pinned.
        if (host.pinnedFingerprint != null) {
            session.setConfig("StrictHostKeyChecking", "yes")
        } else {
            session.setConfig("StrictHostKeyChecking", "no")
        }
        session.connect(15_000)
        if (probeOnly) return session
        // Verify pinned fingerprint after connect.
        val pin = host.pinnedFingerprint
        if (pin != null) {
            val actual = session.hostKey?.let { fingerprintFromBase64(it.key) }
            if (actual != pin) {
                runCatching { session.disconnect() }
                throw SecurityException(
                    "Host key fingerprint mismatch.\nExpected SHA256:$pin\nGot      SHA256:$actual"
                )
            }
        }
        return session
    }

    private fun exec(session: Session, command: String): String {
        val channel = session.openChannel("exec") as ChannelExec
        channel.setCommand(command)
        val stdout = ByteArrayOutputStream()
        val stderr = ByteArrayOutputStream()
        channel.outputStream = null
        channel.setErrStream(stderr)
        channel.inputStream = null
        val input = channel.inputStream
        channel.connect(15_000)
        val buf = ByteArray(8192)
        while (true) {
            while (input.available() > 0) {
                val n = input.read(buf)
                if (n < 0) break
                stdout.write(buf, 0, n)
            }
            if (channel.isClosed) {
                if (input.available() > 0) continue
                break
            }
            Thread.sleep(50)
        }
        runCatching { channel.disconnect() }
        return stdout.toString(Charsets.UTF_8)
    }

    private class SilentUserInfo(private val password: String) : UserInfo {
        override fun getPassphrase(): String? = null
        override fun getPassword(): String? = password
        override fun promptPassword(message: String?): Boolean = true
        override fun promptPassphrase(message: String?): Boolean = true
        override fun promptYesNo(message: String?): Boolean = true
        override fun showMessage(message: String?) {}
    }

    companion object {
        fun sha256Base64(bytes: ByteArray): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return android.util.Base64.encodeToString(
                digest,
                android.util.Base64.NO_WRAP or android.util.Base64.NO_PADDING
            )
        }

        fun fingerprintFromBase64(b64: String): String {
            val bytes = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
            return sha256Base64(bytes)
        }
    }
}
