package com.example

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.ui.theme.MyApplicationTheme

fun isAccessibilityEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
    val colonSplitter = TextUtils.SimpleStringSplitter(':')
    colonSplitter.setString(enabledServices)
    while (colonSplitter.hasNext()) {
        val componentName = colonSplitter.next()
        if (componentName.equals(ComponentName(context, AppBlockerService::class.java).flattenToString(), ignoreCase = true)) {
            return true
        }
    }
    return false
}

fun isDeviceAdminActive(context: Context): Boolean {
    val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    return dpm.isAdminActive(ComponentName(context, AdminReceiver::class.java))
}

@Composable
fun MainScreen(
    onStartVpn: () -> Unit,
    onEnableAccessibility: () -> Unit,
    onEnableAdmin: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val sharedPrefs = context.getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE)
    
    var savedPin by remember { mutableStateOf(sharedPrefs.getString("pin", null)) }
    var savedSecurityAnswer by remember { mutableStateOf(sharedPrefs.getString("security_answer", null)) }
    var enteredPin by remember { mutableStateOf("") }
    var enteredSecurityAnswer by remember { mutableStateOf("") }
    var isAuthenticated by remember { mutableStateOf(savedPin == null) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    
    var isVpnActive by remember { mutableStateOf(false) }
    var isAccActive by remember { mutableStateOf(false) }
    var isAdminActive by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isVpnActive = VpnService.prepare(context) == null
                isAccActive = isAccessibilityEnabled(context)
                isAdminActive = isDeviceAdminActive(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!isAuthenticated) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp).imePadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Enter PIN to Access", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = enteredPin,
                onValueChange = { enteredPin = it },
                label = { Text("PIN") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                if (enteredPin == savedPin) {
                    isAuthenticated = true
                } else {
                    android.widget.Toast.makeText(context, "Incorrect PIN", android.widget.Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth(0.6f)) {
                Text("Unlock")
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { showForgotPinDialog = true }) {
                Text("Forgot PIN?")
            }
        }

        if (showForgotPinDialog) {
            var answerInput by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = { showForgotPinDialog = false },
                title = { Text("Reset PIN") },
                text = {
                    Column {
                        if (savedSecurityAnswer != null) {
                            Text("Security Question: What is your childhood pet's name?", style = MaterialTheme.typography.bodyMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = answerInput,
                                onValueChange = { answerInput = it },
                                label = { Text("Answer") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Text("No security question was set. You can reset your PIN directly.", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (savedSecurityAnswer == null || answerInput.trim().equals(savedSecurityAnswer, ignoreCase = true)) {
                            sharedPrefs.edit().remove("pin").remove("security_answer").apply()
                            savedPin = null
                            savedSecurityAnswer = null
                            isAuthenticated = true
                            showForgotPinDialog = false
                            android.widget.Toast.makeText(context, "PIN Reset Successful", android.widget.Toast.LENGTH_SHORT).show()
                        } else {
                            android.widget.Toast.makeText(context, "Incorrect Answer", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    }) {
                        Text("Reset")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showForgotPinDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    } else {
        Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp)
              .imePadding()
              .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("SafeGuard", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Content Blocker & Monitor", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Spacer(modifier = Modifier.height(32.dp))

            if (savedPin == null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Security Setup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = enteredPin,
                            onValueChange = { enteredPin = it },
                            label = { Text("Set a PIN") },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Security Question: What is your childhood pet's name?", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = enteredSecurityAnswer,
                            onValueChange = { enteredSecurityAnswer = it },
                            label = { Text("Security Answer") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = {
                            if (enteredPin.isNotBlank() && enteredSecurityAnswer.isNotBlank()) {
                                sharedPrefs.edit()
                                    .putString("pin", enteredPin)
                                    .putString("security_answer", enteredSecurityAnswer.trim())
                                    .apply()
                                savedPin = enteredPin
                                savedSecurityAnswer = enteredSecurityAnswer.trim()
                                android.widget.Toast.makeText(context, "Security Setup Saved", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Please fill in all fields", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }, modifier = Modifier.align(Alignment.End)) {
                            Text("Save Setup")
                        }
                    }
                }
            }

            FeatureCard(
                title = "DNS Shield (VPN)",
                description = "Blocks adult websites at network level.",
                icon = Icons.Default.Lock,
                isActive = isVpnActive,
                onClick = onStartVpn
            )
            
            FeatureCard(
                title = "App & URL Monitor",
                description = "Monitors and blocks specific apps & URLs.",
                icon = Icons.Default.Warning,
                isActive = isAccActive,
                onClick = onEnableAccessibility
            )
            
            FeatureCard(
                title = "Uninstall Protection",
                description = "Prevents app removal without PIN.",
                icon = Icons.Default.Build,
                isActive = isAdminActive,
                onClick = onEnableAdmin
            )
        }
    }
}

@Composable
fun FeatureCard(title: String, description: String, icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = if (isActive) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = if (isActive) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                Text(description, style = MaterialTheme.typography.bodySmall, color = if (isActive) MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Spacer(modifier = Modifier.width(8.dp))
            if (isActive) {
                Text("ON", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
            } else {
                Text("OFF", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
        }
    }
}


class MainActivity : ComponentActivity() {

  private val vpnLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    if (result.resultCode == RESULT_OK) {
      startService(Intent(this, BlockerVpnService::class.java))
      getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE).edit().putBoolean("vpn_enabled", true).apply()
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
          Box(modifier = Modifier.padding(innerPadding)) {
              MainScreen(
                  onStartVpn = { startVpn() },
                  onEnableAccessibility = { openAccessibilitySettings() },
                  onEnableAdmin = { requestDeviceAdmin() }
              )
          }
        }
      }
    }
  }

  private fun startVpn() {
    try {
      val intent = VpnService.prepare(this)
      if (intent != null) {
        vpnLauncher.launch(intent)
      } else {
        startService(Intent(this, BlockerVpnService::class.java))
        getSharedPreferences("safeguard_prefs", Context.MODE_PRIVATE).edit().putBoolean("vpn_enabled", true).apply()
      }
    } catch (e: SecurityException) {
      // VpnService prepare might fail due to AI Studio emulator limitation
      android.widget.Toast.makeText(this, "VPN might be blocked in this preview environment.", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
      android.widget.Toast.makeText(this, "Error starting VPN", android.widget.Toast.LENGTH_SHORT).show()
    }
  }

  private fun openAccessibilitySettings() {
    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
  }

  private fun requestDeviceAdmin() {
    val componentName = ComponentName(this, AdminReceiver::class.java)
    val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
    intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
    intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Required to prevent uninstallation.")
    startActivity(intent)
  }
}
