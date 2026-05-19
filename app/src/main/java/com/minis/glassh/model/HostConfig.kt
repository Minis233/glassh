package com.minis.glassh.model

import kotlinx.serialization.Serializable

@Serializable
enum class AuthMethod { PASSWORD, KEY }

@Serializable
data class HostConfig(
    val id: String,
    val name: String,
    val host: String,
    val port: Int = 22,
    val user: String,
    val auth: AuthMethod = AuthMethod.PASSWORD,
    /** Password — stored in DataStore. Note: not encrypted at rest yet. */
    val password: String = "",
    /** Private key in OpenSSH/PEM format (full text). */
    val privateKey: String = "",
    /** Optional passphrase for the private key. */
    val keyPassphrase: String = "",
    /** Optional pinned SSH host key fingerprint (sha256 base64, without "SHA256:" prefix). */
    val pinnedFingerprint: String? = null,
    /** Whether to skip host key verification (TOFU on first connect when fingerprint == null). */
    val strictHostKey: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = 0L,
)

@Serializable
data class HostStore(val hosts: List<HostConfig> = emptyList())
