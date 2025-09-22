package com.example.nisnocu.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun UserScreen(navController: NavHostController, userId: String) {
    val firestore = FirebaseFirestore.getInstance()

    var userData by remember { mutableStateOf<Map<String, Any>?>(null) }

    // Listen for changes in user document
    LaunchedEffect(userId) {
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    userData = snapshot.data
                }
            }
    }

    userData?.let { data ->
        val name = data["name"] as? String ?: "Nepoznato"
        val surname = data["surname"] as? String ?: ""
        val username = data["username"] as? String ?: ""
        val points = (data["points"] as? Long)?.toInt() ?: 0
        val photoUrl = data["photo"] as? String

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            if (photoUrl != null) {
                Image(
                    painter = rememberAsyncImagePainter(photoUrl),
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(150.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "$name $surname", color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "@$username", color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Points: $points", color = Color.Black)

            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { navController.navigate("Map") },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Back to Map")
            }
        }
    }
}
