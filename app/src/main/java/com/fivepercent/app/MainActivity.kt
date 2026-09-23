package com.fivepercent.app

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

private val Navy = Color(0xFF08121F)
private val NavyCard = Color(0xFF102235)
private val Turquoise = Color(0xFF39D6C7)
private val SoftWhite = Color(0xFFEAF3F5)
private val Muted = Color(0xFF9AAEB8)

data class BatteryState(val percent: Int = 0, val charging: Boolean = false)
data class TrustedContact(val name: String, val phone: String)

class MainActivity : ComponentActivity() {
    private var battery by mutableStateOf(BatteryState())
    private var receiver: BroadcastReceiver? = null
    private lateinit var preferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = getSharedPreferences("safety", MODE_PRIVATE)
        updateBattery(registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)))
        setContent { FivePercentApp(battery, preferences) { shareLocation() } }
    }

    override fun onStart() {
        super.onStart()
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) { updateBattery(intent) }
        }
        registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStop() {
        receiver?.let { unregisterReceiver(it) }
        receiver = null
        super.onStop()
    }

    private fun updateBattery(intent: Intent?) {
        if (intent == null) return
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
        battery = BatteryState(level * 100 / scale, status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL)
    }

    private fun shareLocation() {
        val locationManager = getSystemService(LocationManager::class.java)
        val hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasPermission) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), 20)
            return
        }
        val location = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }
        val message = if (location == null) "I’m sharing my location from 5%, but a location fix is not available yet." else
            "My last known location from 5%: https://maps.google.com/?q=${location.latitude},${location.longitude}"
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }, "Share location"))
    }
}

@Composable
private fun FivePercentApp(battery: BatteryState, preferences: SharedPreferences, shareLocation: () -> Unit) {
    var tab by remember { mutableStateOf(0) }
    var showContacts by remember { mutableStateOf(false) }
    MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme(background = Navy, surface = NavyCard, primary = Turquoise, onBackground = SoftWhite, onSurface = SoftWhite)) {
        Surface(modifier = Modifier.fillMaxSize(), color = Navy) {
            Scaffold(containerColor = Navy, bottomBar = {
                NavigationBar(containerColor = NavyCard) {
                    NavigationBarItem(tab == 0, { tab = 0 }, icon = { Icon(Icons.Default.BatteryFull, null) }, label = { Text("Battery") })
                    NavigationBarItem(tab == 1, { tab = 1 }, icon = { Icon(Icons.Default.Group, null) }, label = { Text("Family") })
                }
            }) { padding ->
                if (showContacts) ContactScreen(preferences, { showContacts = false })
                else if (tab == 0) HomeScreen(battery, preferences, shareLocation, { showContacts = true }, Modifier.padding(padding))
                else FamilyScreen(preferences, { showContacts = true }, Modifier.padding(padding))
            }
        }
    }
}

@Composable
private fun HomeScreen(battery: BatteryState, preferences: SharedPreferences, shareLocation: () -> Unit, openContacts: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val contacts = readContacts(preferences)
    val mode = when { battery.percent <= 2 -> "CRITICAL BATTERY"; battery.percent <= 5 -> "PROTECTION MODE"; battery.percent <= 10 -> "LOW BATTERY"; else -> "NORMAL" }
    val message = when (mode) {
        "CRITICAL BATTERY" -> "Preserve your phone for urgent communication."
        "PROTECTION MODE" -> "You've reached your final 5%. Preserve your phone for essential contact."
        "LOW BATTERY" -> "Your battery is getting low. 5% helps you keep your final reserve available."
        else -> "Your battery is in a comfortable range. Keep 5% ready for when it matters."
    }
    val accent = if (battery.percent <= 10) Color(0xFFFFC857) else Turquoise
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Spacer(Modifier.height(22.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Shield, null, tint = Turquoise, modifier = Modifier.size(28.dp))
                Column { Text("5%", fontSize = 22.sp, fontWeight = FontWeight.Bold); Text("Save 5%. Stay Connected.", color = Muted, fontSize = 12.sp) }
            }
            Spacer(Modifier.height(25.dp))
            Text(mode, color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.3.sp)
            Text("${battery.percent}%", color = SoftWhite, fontSize = 88.sp, fontWeight = FontWeight.Bold, lineHeight = 94.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(if (battery.charging) Turquoise else Muted))
                Text(if (battery.charging) "Charging" else "Not charging", color = Muted)
            }
            Spacer(Modifier.height(4.dp))
            Text(message, color = SoftWhite, fontSize = 17.sp, lineHeight = 24.sp)
        }
        item { SafetyAction("I'm safe", Icons.Default.Check, Turquoise) {} }
        item { SafetyAction("Share my location", Icons.Default.LocationOn, Turquoise, shareLocation) }
        item { SafetyAction("Call family", Icons.Default.Call, Color(0xFFFFC857)) {
            val number = contacts.firstOrNull()?.phone
            if (number != null) context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(number)}"))) else openContacts()
        } }
        item {
            HorizontalDivider(color = Color(0xFF203548))
            Text("Your safety circle", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            if (contacts.isEmpty()) Text("Add trusted family contacts so calling stays one tap away.", color = SoftWhite, modifier = Modifier.padding(top = 6.dp))
            contacts.take(2).forEach { contact -> ContactRow(contact) }
            TextButton(onClick = openContacts) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.size(7.dp)); Text("Manage trusted contacts") }
        }
    }
}

@Composable
private fun SafetyAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, action: () -> Unit) {
    Button(onClick = action, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Navy)) {
        Icon(icon, null); Spacer(Modifier.size(10.dp)); Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
private fun FamilyScreen(preferences: SharedPreferences, openContacts: () -> Unit, modifier: Modifier) {
    val contacts = readContacts(preferences)
    LazyColumn(modifier.fillMaxSize().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { Spacer(Modifier.height(22.dp)); Text("Family", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Your safety circle, in one place.", color = Muted); Spacer(Modifier.height(10.dp)) }
        item { SectionTitle("Family members"); if (contacts.isEmpty()) EmptyPanel("No trusted contacts yet.", "Add people you want to keep in the loop.", openContacts) else contacts.forEach { ContactRow(it) } }
        item { SectionTitle("Safety tools") }
        item { ToolRow("Emergency contacts", "Choose the people you trust most", Icons.Default.PersonAdd, openContacts) }
        item { ToolRow("Last known location", "Only shared when you choose to share", Icons.Default.LocationOn) {} }
        item { ToolRow("Safety check-ins", "Check in manually when you arrive safely", Icons.Default.Check) {} }
        item { ToolRow("Check-in history", "Your recent check-ins will appear here", Icons.Default.ChevronRight) {} }
        item { ToolRow("Location sharing", "Use your phone's normal sharing options", Icons.Default.Share) {} }
        item { Spacer(Modifier.height(8.dp)); Text("5% never contacts emergency services automatically. For immediate danger, use your phone's emergency call feature.", color = Muted, fontSize = 12.sp, lineHeight = 18.sp) }
    }
}

@Composable private fun SectionTitle(text: String) { Text(text, color = Turquoise, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp) }
@Composable private fun ContactRow(contact: TrustedContact) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(38.dp).clip(CircleShape).background(Turquoise.copy(alpha = .16f)), contentAlignment = Alignment.Center) { Text(contact.name.take(1).uppercase(), color = Turquoise, fontWeight = FontWeight.Bold) }; Column(Modifier.padding(start = 12.dp)) { Text(contact.name, fontWeight = FontWeight.SemiBold); Text(contact.phone, color = Muted, fontSize = 12.sp) } } }
@Composable private fun EmptyPanel(title: String, body: String, action: () -> Unit) { Surface(color = NavyCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { Text(title, fontWeight = FontWeight.Bold); Text(body, color = Muted); OutlinedButton(onClick = action) { Icon(Icons.Default.Add, null); Spacer(Modifier.size(6.dp)); Text("Add contact") } } } }
@Composable private fun ToolRow(title: String, body: String, icon: androidx.compose.ui.graphics.vector.ImageVector, action: () -> Unit) { Surface(onClick = action, color = NavyCard, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = Turquoise); Column(Modifier.weight(1f).padding(horizontal = 14.dp)) { Text(title, fontWeight = FontWeight.SemiBold); Text(body, color = Muted, fontSize = 12.sp) }; Icon(Icons.Default.ChevronRight, null, tint = Muted) } } }

@Composable
private fun ContactScreen(preferences: SharedPreferences, close: () -> Unit) {
    val context = LocalContext.current
    var contacts by remember { mutableStateOf(readContacts(preferences)) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri ->
        if (uri != null) {
            val projection = arrayOf(ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(0) ?: "Trusted contact"
                    val phone = cursor.getString(1) ?: ""
                    if (phone.isNotBlank()) { preferences.edit().putString("contacts", (contacts + TrustedContact(name, phone)).joinToString("|") { "${it.name}¦${it.phone}" }).apply(); contacts = readContacts(preferences) }
                }
            }
        }
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) { TextButton(onClick = close) { Text("Back") }; Text("Trusted contacts", fontSize = 26.sp, fontWeight = FontWeight.Bold) }
        Text("Select people you trust. 5% only opens your phone's dialler or sharing sheet when you ask it to.", color = Muted, lineHeight = 20.sp)
        contacts.forEach { ContactRow(it) }
        Button(onClick = {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) picker.launch(null)
            else (context as? ComponentActivity)?.requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 21)
        }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Turquoise, contentColor = Navy)) { Icon(Icons.Default.PersonAdd, null); Spacer(Modifier.size(8.dp)); Text("Select from contacts") }
    }
}

private fun readContacts(preferences: SharedPreferences): List<TrustedContact> = preferences.getString("contacts", "").orEmpty().split("|").filter { it.contains("¦") }.map { value -> val parts = value.split("¦", limit = 2); TrustedContact(parts[0], parts[1]) }