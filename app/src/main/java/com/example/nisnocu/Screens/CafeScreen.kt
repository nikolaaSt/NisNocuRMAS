package com.example.nisnocu.Screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

@Composable
fun CafeScreen(navController: NavHostController, cafeId: String, currentUserId: String) {
    val firestore = FirebaseFirestore.getInstance()

    var cafeData by remember { mutableStateOf<Map<String, Any>?>(null) }
    var hasReserved by remember { mutableStateOf(false) }

    //gledanje za promene u kaficima u ovom slucaju za rezervacije ukoliko korisnik rezervisao na overlay bice onemogucena rezervacija
    LaunchedEffect(cafeId) {
        firestore.collection("kafici")
            .document(cafeId)
            .collection("reservations")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { doc ->
                hasReserved=doc.exists()
            }
    //pokupljanje informacija o koaficima za njihov id
        firestore.collection("kafici").document(cafeId)
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null && snapshot.exists()) {
                    cafeData = snapshot.data
                }
            }
    }
    //dobijanje informacija preko ida i stavljanje to u promenljive gde se cuva
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
        }
    }
}
