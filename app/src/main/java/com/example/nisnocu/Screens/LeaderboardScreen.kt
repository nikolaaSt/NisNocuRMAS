package com.example.nisnocu.Screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

@Composable
fun LeaderboardScreen(navController:NavHostController){
    val firestore=FirebaseFirestore.getInstance()
    var rangLista by remember { mutableStateOf<List<Map<String,Any>>>(emptyList()) }

    // pracenje promena u bazi za usere i njihove poene

    LaunchedEffect(Unit) {
        firestore.collection("users")
            .orderBy("points", Query.Direction.DESCENDING)
            .addSnapshotListener{snapshot, _ ->
                if(snapshot!=null){
                    rangLista=snapshot.documents.mapNotNull { it.data }
                }
            }
    }

    Column(
        modifier=Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment=Alignment.CenterHorizontally
    ){
        Text("Rang lista", fontWeight = FontWeight.Bold)

        Spacer(modifier=Modifier.height(16.dp))

        LazyColumn (verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier=Modifier.fillMaxWidth()){
            itemsIndexed(rangLista){index, user ->
                val username=user["username"] as? String ?: "Nepoznato"
                val poeni=(user["points"] as? Long ?: 0L).toInt()

                Card (
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp
                    )

                ){
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${index+1}. $username")
                        Text("$poeni pts" )
                    }

                }

            }
        }
    }
}