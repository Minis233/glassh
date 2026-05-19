package com.minis.glassh.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.minis.glassh.model.AuthMethod
import com.minis.glassh.model.HostConfig

@Composable
fun HostEditScreen(vm: GlasshViewModel, hostId: String?) {
    val hosts by vm.hosts.collectAsState()
    val existing = remember(hostId, hosts) { hosts.firstOrNull { it.id == hostId } }

    var name by rememberSaveable { mutableStateOf(existing?.name.orEmpty()) }
    var host by rememberSaveable { mutableStateOf(existing?.host.orEmpty()) }
    var port by rememberSaveable { mutableStateOf((existing?.port ?: 22).toString()) }
    var user by rememberSaveable { mutableStateOf(existing?.user.orEmpty()) }
    var auth by rememberSaveable { mutableStateOf(existing?.auth ?: AuthMethod.PASSWORD) }
    var password by rememberSaveable { mutableStateOf(existing?.password.orEmpty()) }
    var privateKey by rememberSaveable { mutableStateOf(existing?.privateKey.orEmpty()) }
    var passphrase by rememberSaveable { mutableStateOf(existing?.keyPassphrase.orEmpty()) }
    var pinned by rememberSaveable { mutableStateOf(existing?.pinnedFingerprint.orEmpty()) }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 56.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BackPill(onClick = { vm.back() })
                Spacer(Modifier.size(12.dp))
                Text(
                    if (existing == null) "New server" else "Edit server",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabelText("Connection")
                    GlassField(name, "Display name", { name = it })
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.weight(2f)) { GlassField(host, "Host or IP", { host = it }) }
                        Box(Modifier.weight(1f)) {
                            GlassField(
                                port, "Port", { port = it.filter { c -> c.isDigit() }.take(5) },
                                keyboard = KeyboardType.Number
                            )
                        }
                    }
                    GlassField(user, "Username", { user = it })
                }
            }

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabelText("Authentication")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AuthChip("Password", auth == AuthMethod.PASSWORD) { auth = AuthMethod.PASSWORD }
                        AuthChip("Private key", auth == AuthMethod.KEY) { auth = AuthMethod.KEY }
                    }
                    if (auth == AuthMethod.PASSWORD) {
                        GlassField(password, "Password", { password = it }, isPassword = true)
                    } else {
                        GlassField(
                            privateKey, "Paste OpenSSH private key (-----BEGIN ... PRIVATE KEY-----)",
                            { privateKey = it },
                            singleLine = false,
                        )
                        GlassField(passphrase, "Key passphrase (optional)", { passphrase = it }, isPassword = true)
                    }
                }
            }

            GlassSurface(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabelText("Host key (optional)")
                    Text(
                        "Pin the SHA256 fingerprint of the server's SSH host key. " +
                            "Run `ssh-keyscan -t ed25519 host | ssh-keygen -lf - -E sha256` and paste the value after the SHA256: prefix.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                    GlassField(pinned, "SHA256 fingerprint", { pinned = it.trim() })
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ActionButton(
                    label = "Cancel",
                    modifier = Modifier.weight(1f),
                    onClick = { vm.back() },
                )
                ActionButton(
                    label = "Save",
                    modifier = Modifier.weight(1f),
                    primary = true,
                    enabled = host.isNotBlank() && user.isNotBlank() &&
                        (port.toIntOrNull() ?: 0) in 1..65535,
                    onClick = {
                        vm.saveHost(
                            HostConfig(
                                id = existing?.id.orEmpty(),
                                name = name.ifBlank { host },
                                host = host.trim(),
                                port = port.toIntOrNull() ?: 22,
                                user = user.trim(),
                                auth = auth,
                                password = if (auth == AuthMethod.PASSWORD) password else "",
                                privateKey = if (auth == AuthMethod.KEY) privateKey else "",
                                keyPassphrase = if (auth == AuthMethod.KEY) passphrase else "",
                                pinnedFingerprint = pinned.takeIf { it.isNotBlank() },
                                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
                                lastUsed = existing?.lastUsed ?: 0L,
                            )
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun LabelText(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
    )
}

@Composable
private fun GlassField(
    value: String,
    placeholder: String,
    onChange: (String) -> Unit,
    isPassword: Boolean = false,
    singleLine: Boolean = true,
    keyboard: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f)) },
        singleLine = singleLine,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (singleLine) 56.dp else 120.dp),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = if (isPassword) KeyboardType.Password else keyboard),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
            unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
            focusedContainerColor = Color.White.copy(alpha = 0.12f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.06f),
            cursorColor = MaterialTheme.colorScheme.primary,
            focusedTextColor = MaterialTheme.colorScheme.onBackground,
            unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
        ),
    )
}

@Composable
private fun AuthChip(label: String, selected: Boolean, onClick: () -> Unit) {
    GlassSurface(
        shape = RoundedCornerShape(20.dp),
        tintAlpha = if (selected) 0.4f else 0.18f,
        modifier = Modifier
            .height(40.dp)
            .clickable { onClick() },
    ) {
        Box(Modifier.padding(horizontal = 18.dp).fillMaxSize().heightIn(min = 40.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 14.sp,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
) {
    GlassSurface(
        modifier = modifier
            .height(54.dp)
            .clickable(enabled = enabled) { if (enabled) onClick() },
        shape = RoundedCornerShape(20.dp),
        tintAlpha = if (primary) 0.45f else 0.22f,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (!enabled) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                else if (primary) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
internal fun BackPill(onClick: () -> Unit) {
    GlassSurface(
        shape = CircleShape,
        tintAlpha = 0.22f,
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
