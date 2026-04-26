package com.example.smarthomeai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

// ── Colors ──
val DarkBg       = Color(0xFF111111)
val CardDark     = Color(0xFF1E1E1E)
val CardDarker   = Color(0xFF181818)
val GreenAccent  = Color(0xFF9EF53B)
val GreenDark    = Color(0xFF6ABF1F)
val TextPrimary  = Color(0xFFFFFFFF)
val TextSecondary= Color(0xFF999999)
val ChipBg       = Color(0xFF2A2A2A)

class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if user is logged in
        val currentUser = Firebase.auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContent {
            HomeScreen()
        }
    }
}

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val auth = Firebase.auth
    val currentUser = auth.currentUser

    // Get user name from Firebase displayName
    val userName = remember(currentUser) {
        currentUser?.displayName?.let { displayName ->
            // If displayName exists, use it
            displayName
        } ?: run {
            // Fallback: try to get from email
            currentUser?.email?.split("@")?.firstOrNull()
                ?.replace(".", " ")
                ?.split(" ")
                ?.joinToString(" ") { word ->
                    word.replaceFirstChar { char -> char.uppercase() }
                } ?: "Smart User"
        }
    }

    var selectedNav    by remember { mutableIntStateOf(0) }
    var selectedFilter by remember { mutableIntStateOf(0) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    // Device toggle states
    var lampOn  by remember { mutableStateOf(true) }
    var tvOn    by remember { mutableStateOf(false) }
    var acOn    by remember { mutableStateOf(false) }
    var cctvOn  by remember { mutableStateOf(false) }

    // Logout Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout", color = TextPrimary) },
            text = { Text("Are you sure you want to logout?", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        auth.signOut()
                        showLogoutDialog = false
                        val intent = Intent(context, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        context.startActivity(intent)
                        (context as? ComponentActivity)?.finish()
                    }
                ) {
                    Text("Yes", color = GreenAccent)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("No", color = TextSecondary)
                }
            },
            containerColor = CardDark,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {

            // ── TOP BAR ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3A3A3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(26.dp))
                    }
                    Column {
                        Text("Welcome", color = TextSecondary, fontSize = 12.sp)
                        Text(
                            text = userName,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Notification and Logout icons
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Logout Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CardDark)
                            .clickable { showLogoutDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Notification Button
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(CardDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(22.dp))
                    }
                }
            }

            // ── ROW 1: Home Card + Weather Card ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Home Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardDark)
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("$userName's\nHome", color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                            repeat(3) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF3A3A3A))
                                        .border(1.5.dp, DarkBg, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(ChipBg)
                                    .border(1.5.dp, DarkBg, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("3 Users", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }

                // Weather Card
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(130.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(CardDark)
                        .padding(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2A2A2A)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.FlashOn, contentDescription = null, tint = Color(0xFFFFDD55), modifier = Modifier.size(22.dp))
                            }
                            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                        }
                        Text("12°C", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            WeatherStat("Hum", "09%")
                            WeatherStat("Wind", "587")
                            WeatherStat("AQI", "21")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // ── MANAGE SCENES ──
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Manage Scenes", color = TextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SceneChip(icon = Icons.Default.WbSunny, label = "Morning", active = true)
                    SceneChip(icon = Icons.Outlined.Nightlight, label = "Night", active = false)
                    SceneChip(icon = Icons.Outlined.WaterDrop, label = "Calm", active = false)
                    SceneChip(icon = Icons.Outlined.FlashOn, label = "Energetic", active = false)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── FILTER CHIPS ──
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(GreenAccent),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                }

                listOf("All Devices", "Living Room", "Bedroom", "Kitchen").forEachIndexed { i, label ->
                    val isSelected = selectedFilter == i
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (isSelected) GreenAccent else ChipBg)
                            .clickable { selectedFilter = i }
                            .padding(horizontal = 16.dp, vertical = 9.dp)
                    ) {
                        Text(label, color = if (isSelected) Color.Black else TextSecondary, fontSize = 13.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── DEVICE GRID ──
            Column(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeviceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Lightbulb,
                        title = "Smart Lamp",
                        subtitle = "3 Devices",
                        isOn = lampOn,
                        onToggle = { lampOn = it },
                        highlighted = true
                    )
                    DeviceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Tv,
                        title = "Smart Tv",
                        subtitle = "1 Device",
                        isOn = tvOn,
                        onToggle = { tvOn = it },
                        highlighted = false
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    DeviceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.AcUnit,
                        title = "Air Conditioner",
                        subtitle = "1 Devices",
                        isOn = acOn,
                        onToggle = { acOn = it },
                        highlighted = false
                    )
                    DeviceCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Videocam,
                        title = "Smart CCTV",
                        subtitle = "1 Device",
                        isOn = cctvOn,
                        onToggle = { cctvOn = it },
                        highlighted = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // ── BOTTOM NAV ──
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(CardDarker)
                .padding(vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(icon = Icons.Default.Home, index = 0, selected = selectedNav) { selectedNav = it }
                BottomNavItem(icon = Icons.Default.GridView, index = 1, selected = selectedNav) { selectedNav = it }

                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(GreenAccent)
                        .clickable { selectedNav = 2 },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                }

                BottomNavItem(icon = Icons.Default.BarChart, index = 3, selected = selectedNav) { selectedNav = it }
                BottomNavItem(icon = Icons.Default.Person, index = 4, selected = selectedNav) { selectedNav = it }
            }
        }
    }
}

@Composable
fun WeatherStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = TextSecondary, fontSize = 10.sp)
        Text(value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SceneChip(icon: ImageVector, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (active) GreenAccent else ChipBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = if (active) Color.Black else TextSecondary, modifier = Modifier.size(22.dp))
        }
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun DeviceCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit,
    highlighted: Boolean
) {
    val bgColor = if (highlighted && isOn)
        Brush.verticalGradient(listOf(GreenAccent, GreenDark))
    else
        Brush.verticalGradient(listOf(CardDark, CardDark))

    Box(
        modifier = modifier
            .height(140.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (highlighted && isOn) Color(0x33000000) else ChipBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = if (highlighted && isOn) Color.Black else TextSecondary, modifier = Modifier.size(20.dp))
                }
                Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = if (highlighted && isOn) Color(0x88000000) else TextSecondary, modifier = Modifier.size(18.dp))
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = if (highlighted && isOn) Color.Black else TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, color = if (highlighted && isOn) Color(0x99000000) else TextSecondary, fontSize = 11.sp)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (isOn) "ON" else "OFF", color = if (highlighted && isOn) Color.Black else TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = isOn,
                    onCheckedChange = { onToggle(it) },
                    modifier = Modifier.height(24.dp).width(44.dp),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = if (highlighted) Color.Black else Color.White,
                        checkedTrackColor = if (highlighted) Color(0x66000000) else GreenAccent,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = ChipBg,
                        uncheckedBorderColor = ChipBg
                    )
                )
            }
        }
    }
}

@Composable
fun BottomNavItem(icon: ImageVector, index: Int, selected: Int, onClick: (Int) -> Unit) {
    val isSelected = selected == index
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(CircleShape)
            .clickable { onClick(index) },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = if (isSelected) GreenAccent else TextSecondary, modifier = Modifier.size(24.dp))
    }
}