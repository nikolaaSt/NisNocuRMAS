package com.example.nisnocu.Screens

import android.annotation.SuppressLint
import android.location.Location
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore


data class Cafe(
    val id: String = "",
    val name: String = "",
    val photo: String? = null,
    val tables: Int = 0,
    val addedBy: String = "",
    val createdAt: com.google.firebase.Timestamp? = null,
    val averageRating: Double = 0.0,
    val ratingsCount: Int = 0,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@SuppressLint("MissingPermission")
@Composable
fun SearchScreen(navController: NavHostController, currentUserId: String, userLat: Double, userLon: Double) {
    val firestore = FirebaseFirestore.getInstance()

    var cafes by remember { mutableStateOf(listOf<Cafe>()) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("none") }

    // fetch data
    LaunchedEffect(Unit) {
        firestore.collection("kafici")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    cafes = snapshot.documents.mapNotNull { doc ->
                        Cafe(
                            id = doc.id,
                            name = doc.getString("name") ?: "",
                            photo = doc.getString("photo"),
                            tables = (doc.getLong("tables") ?: 0).toInt(),
                            addedBy = doc.getString("addedBy") ?: "",
                            createdAt = doc.getTimestamp("createdAt"),
                            averageRating = doc.getDouble("averageRating") ?: 0.0,
                            ratingsCount = (doc.getLong("ratingsCount") ?: 0).toInt(),
                            latitude = doc.getDouble("latitude"),
                            longitude = doc.getDouble("longitude")
                        )
                    }
                }
            }
    }

    // apply filters + search
    val filteredCafes = cafes
        .filter { it.name.contains(searchQuery, ignoreCase = true) || searchQuery.isEmpty() }
        .let { list ->
            when (selectedFilter) {
                "rating" -> list.sortedByDescending { it.averageRating }
                "user" -> list.filter { it.addedBy == currentUserId }
                "newest" -> list.sortedByDescending { it.createdAt }
                "radius" -> list.sortedBy { cafe ->
                    if (cafe.latitude != null && cafe.longitude != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            userLat, userLon,
                            cafe.latitude, cafe.longitude,
                            results
                        )
                        results[0] // meters
                    } else {
                        Float.MAX_VALUE
                    }
                }
                else -> list
            }
        }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        // search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Search by name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // filter buttons
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(onClick = { selectedFilter = "rating" }) { Text("By Rating") }
            Button(onClick = { selectedFilter = "user" }) { Text("By User") }
            Button(onClick = { selectedFilter = "newest" }) { Text("Newest") }
            Button(onClick = { selectedFilter = "radius" }) { Text("Nearest") }
        }

        Spacer(Modifier.height(16.dp))

        // results
        LazyColumn {
            items(filteredCafes) { cafe ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .clickable {
                            navController.navigate("Kafic/${cafe.id}/$currentUserId")
                        }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(cafe.name, style = MaterialTheme.typography.titleMedium)
                        Text("Tables: ${cafe.tables}")
                        Text("⭐ ${cafe.averageRating} (${cafe.ratingsCount})")
                        cafe.createdAt?.let {
                            Text("Added: ${it.toDate()}")
                        }
                    }
                }
            }
        }
    }
}

