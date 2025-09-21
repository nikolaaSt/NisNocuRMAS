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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID



@Composable
fun RegistrationScreen(navController: NavHostController) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }

    val auth = FirebaseAuth.getInstance()
    val firestore=FirebaseFirestore.getInstance()
    val storage=FirebaseStorage.getInstance()
    val context= LocalContext.current

    // Launcher for picking from gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> photoUri = uri }

    // Launcher for taking a photo


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
            // Profile Image
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clickable { galleryLauncher.launch("image/*") }, // Click to pick from gallery
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

            // Username
            Text(text = "Username", fontSize = 16.sp)
            TextField(
                value = username,
                onValueChange = { username = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Password
            Text(text = "Password", fontSize = 16.sp)
            TextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // First name
            Text(text = "First Name", fontSize = 16.sp)
            TextField(
                value = firstName,
                onValueChange = { firstName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Last name
            Text(text = "Last Name", fontSize = 16.sp)
            TextField(
                value = lastName,
                onValueChange = { lastName = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Phone number
            Text(text = "Phone Number", fontSize = 16.sp)
            TextField(
                value = phoneNumber,
                onValueChange = { phoneNumber = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Register Button
            Button(
                onClick = {
                    if(username.isNotEmpty()&&password.isNotEmpty()&&photoUri!=null){
                        val fakeEmail="$username@app.com"
                        auth.createUserWithEmailAndPassword(fakeEmail,password)
                            .addOnSuccessListener { result->
                                val userid=result.user?.uid?:return@addOnSuccessListener
                                val storageRef=storage.reference
                                    .child("profile_photos/$userid-${UUID.randomUUID()}.jpg")


                                //za upload slika
                                storageRef.putFile(photoUri!!)
                                    .addOnSuccessListener {
                                        storageRef.downloadUrl.addOnSuccessListener { downloadUrl->
                                            val userData= hashMapOf(
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
                                                    Toast.makeText(context, "Registracija uspesna!", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(context,"Popunite sva polja za registraciju", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Register")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to Login
            Text(
                text = "Already have an account? Login",
                modifier = Modifier.clickable { navController.navigate("login") },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
