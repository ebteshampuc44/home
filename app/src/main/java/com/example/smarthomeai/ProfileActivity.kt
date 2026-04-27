package com.example.smarthomeai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProfileScreen(
                onBackClick = { finish() }
            )
        }
    }
}

@Composable
fun ProfileScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUser = auth.currentUser
    val storage = FirebaseStorage.getInstance()
    val storageRef = storage.reference

    // একই ডাটাবেস ব্যবহার করুন - শুধু "users" path এ
    val databaseRef = FirebaseDatabase.getInstance().getReference("users")

    val coroutineScope = rememberCoroutineScope()

    // User data states
    var userName by remember { mutableStateOf(currentUser?.displayName ?: "Smart User") }
    var userEmail by remember { mutableStateOf(currentUser?.email ?: "user@example.com") }
    var phoneNumber by remember { mutableStateOf("") }
    var profileImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    // Dialog states
    var showEditNameDialog by remember { mutableStateOf(false) }
    var showEditPhoneDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showSuccessToast by remember { mutableStateOf(false) }
    var toastMessage by remember { mutableStateOf("") }

    // Password change states
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }

    // নতুন ইউজারের জন্য ডাটাবেস এন্ট্রি তৈরি করুন
    fun createUserEntry() {
        val userId = currentUser?.uid ?: return
        val userData = mapOf(
            "displayName" to userName,
            "email" to userEmail,
            "createdAt" to System.currentTimeMillis()
        )
        databaseRef.child(userId).setValue(userData)
    }

    // Load user data from Realtime Database (একই ডাটাবেসের users নোড থেকে)
    fun loadUserData() {
        val userId = currentUser?.uid ?: return

        databaseRef.child(userId).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                phoneNumber = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                val storedImageUrl = snapshot.child("profileImageUrl").getValue(String::class.java)
                if (storedImageUrl != null && storedImageUrl.isNotEmpty()) {
                    profileImageUrl = storedImageUrl
                }
            } else {
                // নতুন ইউজার, ডাটাবেসে এন্ট্রি তৈরি করুন
                createUserEntry()
            }
        }.addOnFailureListener {
            // Handle error silently
        }
    }

    // Load data on start
    LaunchedEffect(Unit) {
        loadUserData()
    }

    fun showFeedback(message: String) {
        toastMessage = message
        showSuccessToast = true
        coroutineScope.launch {
            delay(2000)
            showSuccessToast = false
        }
    }

    // Upload image to Firebase Storage
    suspend fun uploadImageToStorage(imageUri: Uri): String? {
        return try {
            val userId = currentUser?.uid ?: return null

            // Delete old profile images first
            try {
                val oldImageRef = storageRef.child("profile_images/$userId")
                val listResult = oldImageRef.listAll().await()
                listResult.items.forEach { item ->
                    item.delete().await()
                }
            } catch (e: Exception) {
                // No old images or error, continue
            }

            // Upload new image
            val imageRef = storageRef.child("profile_images/$userId/${UUID.randomUUID()}.jpg")
            val uploadTask = imageRef.putFile(imageUri).await()
            val downloadUrl = imageRef.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            showFeedback("✗ Image upload failed: ${e.message}")
            null
        }
    }

    // Update profile image
    fun updateProfileImage(imageUri: Uri) {
        isUploadingImage = true
        coroutineScope.launch {
            try {
                val imageUrl = uploadImageToStorage(imageUri)

                if (imageUrl != null) {
                    val userId = currentUser?.uid ?: return@launch

                    // Update Realtime Database (একই ডাটাবেসে)
                    databaseRef.child(userId).child("profileImageUrl").setValue(imageUrl)
                        .addOnSuccessListener {
                            profileImageUrl = imageUrl
                            showFeedback("✓ Profile image updated successfully")

                            // Update Firebase Auth profile photo URL
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setPhotoUri(Uri.parse(imageUrl))
                                .build()
                            currentUser?.updateProfile(profileUpdates)
                        }
                        .addOnFailureListener {
                            showFeedback("✗ Failed to save image URL to database")
                        }
                }
            } catch (e: Exception) {
                showFeedback("✗ Error: ${e.message}")
            } finally {
                isUploadingImage = false
            }
        }
    }

    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            updateProfileImage(it)
        }
    }

    fun updateDisplayName(name: String) {
        isLoading = true
        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()

        currentUser?.updateProfile(profileUpdates)?.addOnCompleteListener { task ->
            isLoading = false
            if (task.isSuccessful) {
                userName = name
                // Save to Realtime Database (একই ডাটাবেসে)
                val userId = currentUser?.uid
                if (userId != null) {
                    databaseRef.child(userId).child("displayName").setValue(name)
                }
                showFeedback("✓ Name updated successfully")
            } else {
                showFeedback("✗ Failed to update name: ${task.exception?.message}")
            }
        }
    }

    fun updatePhoneNumber(phone: String) {
        isLoading = true
        val userId = currentUser?.uid ?: return

        databaseRef.child(userId).child("phoneNumber").setValue(phone).addOnCompleteListener { task ->
            isLoading = false
            if (task.isSuccessful) {
                phoneNumber = phone
                showFeedback("✓ Phone number updated successfully")
            } else {
                showFeedback("✗ Failed to update phone number")
            }
        }
    }

    fun changePassword(currentPwd: String, newPwd: String) {
        passwordError = ""

        if (newPwd.length < 6) {
            passwordError = "Password must be at least 6 characters"
            return
        }

        if (newPwd != confirmPassword) {
            passwordError = "Passwords do not match"
            return
        }

        isLoading = true

        val credential = EmailAuthProvider.getCredential(currentUser?.email ?: "", currentPwd)
        currentUser?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                currentUser.updatePassword(newPwd).addOnCompleteListener { updateTask ->
                    isLoading = false
                    if (updateTask.isSuccessful) {
                        showChangePasswordDialog = false
                        currentPassword = ""
                        newPassword = ""
                        confirmPassword = ""
                        showFeedback("✓ Password changed successfully")
                    } else {
                        showFeedback("✗ Failed to change password: ${updateTask.exception?.message}")
                    }
                }
            } else {
                isLoading = false
                passwordError = "Current password is incorrect"
            }
        }
    }

    fun logout() {
        auth.signOut()
        showLogoutDialog = false
        context.startActivity(
            Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        (context as? ComponentActivity)?.finish()
    }

    fun deleteAccount() {
        isLoading = true
        val userId = currentUser?.uid

        coroutineScope.launch {
            try {
                userId?.let {
                    val imageRef = storageRef.child("profile_images/$userId")
                    val listResult = imageRef.listAll().await()
                    listResult.items.forEach { item ->
                        item.delete().await()
                    }
                }
            } catch (e: Exception) {
                // Ignore error if image doesn't exist
            }

            // Delete from Realtime Database (একই ডাটাবেসের users নোড থেকে)
            userId?.let {
                databaseRef.child(it).removeValue().await()
            }

            // Delete user from Authentication
            currentUser?.delete()?.addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    showDeleteAccountDialog = false
                    showFeedback("Account deleted successfully")
                    coroutineScope.launch {
                        delay(1500)
                        context.startActivity(
                            Intent(context, LoginActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            }
                        )
                        (context as? ComponentActivity)?.finish()
                    }
                } else {
                    showFeedback("✗ Failed to delete account: ${task.exception?.message}")
                }
            }
        }
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
        ) {
            AnimatedProfileTopBar(onBackClick = onBackClick)

            Spacer(modifier = Modifier.height(16.dp))

            ProfileImageSection(
                profileImageUrl = profileImageUrl,
                userName = userName,
                isLoading = isUploadingImage,
                onImageClick = { imagePickerLauncher.launch("image/*") }
            )

            Spacer(modifier = Modifier.height(24.dp))

            UserInfoCard(
                icon = Icons.Outlined.Person,
                iconColor = GreenAccent,
                title = "Full Name",
                value = userName,
                onEditClick = { showEditNameDialog = true }
            )

            UserInfoCard(
                icon = Icons.Outlined.Email,
                iconColor = BlueAccent,
                title = "Email Address",
                value = userEmail,
                isEditable = false,
                onEditClick = {}
            )

            UserInfoCard(
                icon = Icons.Outlined.Phone,
                iconColor = YellowAccent,
                title = "Phone Number",
                value = phoneNumber.ifEmpty { "Not set" },
                onEditClick = { showEditPhoneDialog = true }
            )

            Spacer(modifier = Modifier.height(16.dp))

            SectionHeaderProfile(title = "Account Settings")

            ActionButton(
                icon = Icons.Outlined.Lock,
                title = "Change Password",
                subtitle = "Update your password",
                iconColor = PurpleAccent,
                onClick = { showChangePasswordDialog = true }
            )

            ActionButton(
                icon = Icons.Outlined.Logout,
                title = "Logout",
                subtitle = "Sign out from your account",
                iconColor = EmergencyRed,
                onClick = { showLogoutDialog = true }
            )

            ActionButton(
                icon = Icons.Outlined.Delete,
                title = "Delete Account",
                subtitle = "Permanently delete your account",
                iconColor = EmergencyRed,
                isDanger = true,
                onClick = { showDeleteAccountDialog = true }
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Smart Home AI v1.0.0",
                color = TextSecondary.copy(alpha = 0.5f),
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        // Dialogs (same as before)
        if (showEditNameDialog) {
            EditNameDialog(
                currentName = userName,
                onDismiss = { showEditNameDialog = false },
                onSave = { newName ->
                    updateDisplayName(newName)
                    showEditNameDialog = false
                }
            )
        }

        if (showEditPhoneDialog) {
            EditPhoneDialog(
                currentPhone = phoneNumber,
                onDismiss = { showEditPhoneDialog = false },
                onSave = { newPhone ->
                    updatePhoneNumber(newPhone)
                    showEditPhoneDialog = false
                }
            )
        }

        if (showChangePasswordDialog) {
            ChangePasswordDialog(
                currentPassword = currentPassword,
                onCurrentPasswordChange = { currentPassword = it },
                newPassword = newPassword,
                onNewPasswordChange = { newPassword = it },
                confirmPassword = confirmPassword,
                onConfirmPasswordChange = { confirmPassword = it },
                error = passwordError,
                isLoading = isLoading,
                onDismiss = {
                    showChangePasswordDialog = false
                    passwordError = ""
                    currentPassword = ""
                    newPassword = ""
                    confirmPassword = ""
                },
                onSave = { changePassword(currentPassword, newPassword) }
            )
        }

        if (showLogoutDialog) {
            AlertDialog(
                onDismissRequest = { showLogoutDialog = false },
                title = { Text("Logout", color = TextPrimary) },
                text = { Text("Are you sure you want to logout?", color = TextSecondary) },
                confirmButton = {
                    Button(
                        onClick = { logout() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                    ) {
                        Text("Logout", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLogoutDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardDark
            )
        }

        if (showDeleteAccountDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAccountDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = EmergencyRed)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Account", color = TextPrimary)
                    }
                },
                text = {
                    Column {
                        Text("This action is permanent and cannot be undone.", color = TextSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("All your data will be erased from our servers.", color = TextSecondary)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { deleteAccount() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmergencyRed)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Delete", color = Color.White)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAccountDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                },
                containerColor = CardDark
            )
        }

        if (showSuccessToast) {
            ProfileToast(message = toastMessage)
        }
    }
}

@Composable
fun AnimatedProfileTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(CardDark)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = GreenAccent,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = "My Profile",
                color = TextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = "Manage your account",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ProfileImageSection(
    profileImageUrl: String?,
    userName: String,
    isLoading: Boolean,
    onImageClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(3.dp, GreenAccent, CircleShape)
                    .clickable { onImageClick() },
                contentAlignment = Alignment.BottomEnd
            ) {
                if (!profileImageUrl.isNullOrEmpty() && !isLoading) {
                    AsyncImage(
                        model = profileImageUrl,
                        contentDescription = "Profile Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(40.dp),
                                color = GreenAccent,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Text(
                                text = userName.take(1).uppercase(),
                                color = GreenAccent,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(GreenAccent)
                        .border(2.dp, DarkBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Edit",
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Text(
                text = userName,
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = GreenAccent.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "Tap photo to change",
                    color = GreenAccent,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun UserInfoCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    isEditable: Boolean = true,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, Color(0xFF252525))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
                }

                Column {
                    Text(
                        title,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        value,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isEditable) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(GreenAccent.copy(alpha = 0.1f))
                ) {
                    Icon(
                        Icons.Outlined.Edit,
                        contentDescription = "Edit",
                        tint = GreenAccent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeaderProfile(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(1.dp)
                .background(GreenAccent.copy(alpha = 0.3f))
        )
    }
}

@Composable
fun ActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    isDanger: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDanger) EmergencyRed.copy(alpha = 0.05f) else CardDark
        ),
        border = BorderStroke(
            1.dp,
            if (isDanger) EmergencyRed.copy(alpha = 0.3f) else Color(0xFF252525)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(22.dp))
                }

                Column {
                    Text(
                        title,
                        color = if (isDanger) EmergencyRed else TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (isDanger) EmergencyRed else TextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun EditNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Name", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name") },
                placeholder = { Text("Enter your full name") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenAccent,
                    unfocusedBorderColor = Color(0xFF252525),
                    cursorColor = GreenAccent
                ),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(name) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black),
                enabled = name.isNotBlank()
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardDark
    )
}

@Composable
fun EditPhoneDialog(
    currentPhone: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var phone by remember { mutableStateOf(currentPhone) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Phone Number", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "Enter your phone number with country code",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    placeholder = { Text("+8801XXXXXXXXX") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = Color(0xFF252525),
                        cursorColor = GreenAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(phone) },
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardDark
    )
}

@Composable
fun ChangePasswordDialog(
    currentPassword: String,
    onCurrentPasswordChange: (String) -> Unit,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    error: String,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Enter your current password and create a new one",
                    color = TextSecondary,
                    fontSize = 12.sp
                )

                OutlinedTextField(
                    value = currentPassword,
                    onValueChange = onCurrentPasswordChange,
                    label = { Text("Current Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = Color(0xFF252525),
                        cursorColor = GreenAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = onNewPasswordChange,
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = Color(0xFF252525),
                        cursorColor = GreenAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = onConfirmPasswordChange,
                    label = { Text("Confirm New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GreenAccent,
                        unfocusedBorderColor = Color(0xFF252525),
                        cursorColor = GreenAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(
                        error,
                        color = EmergencyRed,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent, contentColor = Color.Black),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = CardDark
    )
}

@Composable
fun ProfileToast(message: String) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Card(
            modifier = Modifier
                .padding(24.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.contains("✓")) GreenAccent else EmergencyRed
            )
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (message.contains("✓")) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    message,
                    color = Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}