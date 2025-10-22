package com.example.nisnocu.Screens


import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import java.util.UUID



@Composable
fun RegistrationScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var cameraPhotoUri by remember{ mutableStateOf<Uri?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val firestore=FirebaseFirestore.getInstance()
    val storage=FirebaseStorage.getInstance()
    val context= LocalContext.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission denied", Toast.LENGTH_SHORT).show() //permisija za kameru
        }
    }


    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraPhotoUri != null) {
            photoUri = cameraPhotoUri
        } else {
            Toast.makeText(context, "Failed to capture photo", Toast.LENGTH_SHORT).show()
        }
    }


    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent() //uzimamo iz gallery launcher content i stavljamo u photouri
    ) { uri: Uri? ->
        if (uri != null) {
            photoUri = uri
        } else {
            Toast.makeText(context, "Failed to select photo", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(android.Manifest.permission.CAMERA)
    }


    fun createImageUri(context: Context): Uri? {
        return try {
            val imagesDir = File(context.cacheDir, "images")
            if (!imagesDir.exists()) imagesDir.mkdirs() //pravimo direktorijum za slike ukoliko folder ne postoji
            val imageFile = File(imagesDir, "temp_photo_${System.currentTimeMillis()}.jpg")
            if (!imageFile.createNewFile()) {
                Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show()
                return null
            }
            FileProvider.getUriForFile(
                context,
                "com.example.nisnocu.provider",
                imageFile
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error creating image URI: ${e.message}", Toast.LENGTH_SHORT).show()
            null
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Choose Photo") },
            text = {
                Column {
                    TextButton(onClick = {
                        showDialog = false
                        galleryLauncher.launch("image/*")
                    }) {
                        Text("Gallery")
                    }
                    TextButton(onClick = {
                        showDialog = false
                        if (hasCameraPermission) {
                            cameraPhotoUri = createImageUri(context)
                            cameraPhotoUri?.let { uri ->
                                cameraLauncher.launch(uri)
                            } ?: Toast.makeText(context, "Failed to create image URI", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Camera permission required", Toast.LENGTH_SHORT).show()
                            permissionLauncher.launch(android.Manifest.permission.CAMERA)
                        }
                    }) {
                        Text("Camera")
                    }
                }
            },
            confirmButton = { },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }


    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable {
                        showDialog=true

                    },
                contentAlignment = Alignment.Center
            ) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Profile photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text("Tap to add photo", fontSize = 14.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            Text(text = "Username", fontSize = 16.sp)
            TextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))


            Text(text = "Password", fontSize = 16.sp)
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))


            Text(text = "First Name", fontSize = 16.sp)
            TextField(
                value = firstName,
                onValueChange = { firstName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))


            Text(text = "Last Name", fontSize = 16.sp)
            TextField(
                value = lastName,
                onValueChange = { lastName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))


            Text(text = "Phone Number", fontSize = 16.sp)
            TextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))


            Button(
                onClick = {
                    if(username.isNotEmpty()&&password.isNotEmpty()&&photoUri!=null){
                        val fakeEmail="$username@app.com"  //pravi se fake email koristeci username po specifikacijama projekta :)
                        auth.createUserWithEmailAndPassword(fakeEmail,password)
                            .addOnSuccessListener { result->
                                val userid=result.user?.uid?:return@addOnSuccessListener
                                val storageRef=storage.reference
                                    .child("profile_photos/$userid-${UUID.randomUUID()}.jpg")


                                //za upload slika
                                storageRef.putFile(photoUri!!)
                                    .addOnSuccessListener {
                                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl->
                                            val userData= hashMapOf( //hashmap bukv stavlja info po parovima
                                                "username" to username,
                                                "name" to firstName,
                                                "surname" to lastName,
                                                "phone_number" to phoneNumber,
                                                "photo" to downloadUrl.toString(),
                                                "points" to 0
                                            )

                                            firestore.collection("users")
                                                .document(userid)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    Toast.makeText(context, "Registration successful!", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("login")
                                                }
                                                .addOnFailureListener{ e->
                                                    Toast.makeText(context, "Firestore greska: ${e.message}",Toast.LENGTH_SHORT).show()
                                        }
                                    }


                                    }
                                    .addOnFailureListener{e->
                                        Toast.makeText(context, "Fotografija se nije uploadovala", Toast.LENGTH_SHORT).show()
                                    }

                            }
                            .addOnFailureListener{e->
                                Toast.makeText(context, "autentifikaciona greska:${e.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        Toast.makeText(context,"Fill all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }

            Spacer(modifier = Modifier.height(16.dp))


            Text(
                text = "Already have an account? Login",
                modifier = Modifier.clickable { navController.navigate("login") },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
