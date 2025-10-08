package com.example.nisnocu.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

@Composable
fun CafeScreen(navController: NavHostController, cafeId: String, currentUserId: String) {
    val firestore = FirebaseFirestore.getInstance()

    var cafeData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var hasReserved by remember { mutableStateOf(false) }
    var hasRated by remember { mutableStateOf(false) }
    var ratings by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }

    var newRating by remember { mutableStateOf(5f) }
    var newComment by remember { mutableStateOf("") }

    // Load cafe data, reservation, rating, and listen for ratings
    LaunchedEffect(cafeId) {
        // Check reservation
        firestore.collection("kafici").document(cafeId)
            .collection("reservations").document(currentUserId)
            .get().addOnSuccessListener { doc ->
                hasReserved = doc.exists()
            }

        // Check if user already rated
        firestore.collection("kafici").document(cafeId)
            .collection("ratings").document(currentUserId)
            .get().addOnSuccessListener { doc ->
                hasRated = doc.exists()
            }

        // Listen for cafe data
        firestore.collection("kafici").document(cafeId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    cafeData = snapshot.data
                }
            }

        // Listen for ratings
        firestore.collection("kafici").document(cafeId)
            .collection("ratings")
            .addSnapshotListener { snap, _ ->
                if (snap != null) {
                    ratings = snap.documents.mapNotNull { it.data }
                }
            }
    }

    cafeData?.let { data ->
        val name = data["name"] as? String ?: "Nepoznato"
        val photoUrl = data["photo"] as? String
        val tables = (data["tables"] as? Long)?.toInt() ?: 0

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
                    contentDescription = "Cafe photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(text = name, color = Color.Black)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "Available tables: $tables", color = Color.Black)
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (!hasReserved && tables > 0) {
                        val cafeRef = firestore.collection("kafici").document(cafeId)
                        cafeRef.update("tables", tables - 1)

                        val userRef = firestore.collection("users").document(currentUserId)
                        userRef.update("points", FieldValue.increment(1))

                        cafeRef.collection("reservations")
                            .document(currentUserId)
                            .set(mapOf("reserved" to true))

                        hasReserved = true
                    }
                },
                enabled = !hasReserved && tables > 0,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (hasReserved) "Rezervisano" else "Rezervisi")
            }

            Spacer(Modifier.height(24.dp))

            // Rating input if user has not rated yet
            if (!hasRated) {
                Text("Ostavite recenziju", color = Color.Black)
                Spacer(Modifier.height(8.dp))

                Slider(
                    value = newRating,
                    onValueChange = { newRating = it },
                    valueRange = 1f..5f,
                    steps = 3
                )
                Text("Ocena: ${newRating.toInt()}/5")

                Spacer(Modifier.height(8.dp))

                TextField(
                    value = newComment,
                    onValueChange = { newComment = it },
                    label = { Text("Komentar") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(onClick = {
                    // Get username to store with rating
                    firestore.collection("users").document(currentUserId)
                        .get().addOnSuccessListener { userDoc ->
                            val username = userDoc.getString("username") ?: "Nepoznat"

                            val ratingData = mapOf(
                                "rating" to newRating.toInt(),
                                "comment" to newComment,
                                "userId" to currentUserId,
                                "username" to username
                            )

                            val cafeRef = firestore.collection("kafici").document(cafeId)

                            // ✅ Add or update the user's rating
                            cafeRef.collection("ratings")
                                .document(currentUserId)
                                .set(ratingData)
                                .addOnSuccessListener {
                                    // ✅ After adding rating, recalculate average rating
                                    updateAverageRating(firestore, cafeId)

                                    // ✅ Add 2 points to user
                                    firestore.collection("users").document(currentUserId)
                                        .update("points", FieldValue.increment(2))

                                    hasRated = true
                                }
                        }
                }) {
                    Text("Potvrdi")
                }}

            Spacer(Modifier.height(16.dp))

            // Show ratings list
            if (ratings.isNotEmpty()) {
                Text("Recenzije:", color = Color.Black)
                Spacer(Modifier.height(8.dp))

                ratings.forEach { rating ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text("${rating["username"]} ⭐ ${rating["rating"]}/5", color = Color.Black)
                        Text("${rating["comment"]}", color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

fun updateAverageRating(firestore: FirebaseFirestore, cafeId: String) {
    val cafeRef = firestore.collection("kafici").document(cafeId)

    cafeRef.collection("ratings").get()
        .addOnSuccessListener { snapshot ->
            val ratings = snapshot.documents.mapNotNull { it.getLong("rating")?.toInt() }

            if (ratings.isNotEmpty()) {
                val average = ratings.average()
                val count = ratings.size

                // ✅ Update Firestore cafe document
                cafeRef.update(
                    mapOf(
                        "averageRating" to average,
                        "ratingsCount" to count
                    )
                )
            }
        }
}


