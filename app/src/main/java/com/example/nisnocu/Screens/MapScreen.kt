package com.example.nisnocu.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(navController:NavHostController) {
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    var tablesAvailableInput by remember { mutableStateOf("10") }
    var tablesAvailable by remember{ mutableStateOf(10) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var addPlace by remember { mutableStateOf(false) }
    var placePhotoUri by remember{ mutableStateOf<Uri?>(null) }
    var placeName by remember { mutableStateOf("") }
    var radiusKm by remember{ mutableStateOf(1f) }//neka bude radijus po default 1km
    var radiusDialog by remember{ mutableStateOf(false) }

    val firestore=FirebaseFirestore.getInstance()
    val storage=FirebaseStorage.getInstance()



    // Ovo je da bi se pokrenuo dijalog za permisiju
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                // ukoliko dobijemo permisiju uzimamo za user location koordinate
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                    }
                }
            }
        }
    )

    val photoLauncher= rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ){
        uri: Uri? ->
        placePhotoUri=uri
    }

    // Znaci ovo je launcher za permisiju za koriscenje lokacije(ima i compatibility resenje za starije verzije
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Kamera se pozicionira na 0 duzinu i 0 sirinu bez lokacije korisnika ili dok je ne dobija
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            userLocation ?: LatLng(0.0, 0.0), // Default if location is null
            1f
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            cameraPositionState = cameraPositionState,
            properties = com.google.maps.android.compose.MapProperties(isMyLocationEnabled = true)
        ) {
            // Ako imamo lokaciju onda se stavlja marker i na njegov stisak dobijamo naslov Vasa lokacija
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Vasa lokacija"
                )

                Circle(
                    center=location,
                    radius=(radiusKm*1000f).toDouble(),
                    strokeColor = Color.Blue,           // border color
                    strokeWidth = 2f,
                    fillColor = Color.Blue.copy(alpha = 0.2f)
                )
                // Pomera se kamera na lokaciju koja je dobijena
                LaunchedEffect(location) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }
        }

        //dugme za dodavanje kafica
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ){
            Row(
                modifier=Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ){
                Button(onClick = {addPlace=true}) {
                    Text("+")
                }

                Button(onClick = {radiusDialog=true}) {
                    Text("R")
                }
            }
        }



        //sada kada se pritisne na dugme izlazi overlay

        if(addPlace){
            AlertDialog(
                onDismissRequest = {addPlace=false},
                title = { Text("Dodaj mesto") },
                text={
                    Column {
                        OutlinedTextField(
                            value=placeName,
                            onValueChange = {placeName=it},
                            label={Text("Naziv mesta")},
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = {photoLauncher.launch("image/*")}) {
                            Text(if(placePhotoUri==null) "Dodaj fotografiju" else "Izabrana fotografija")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value=tablesAvailableInput,
                            onValueChange = {input->
                                tablesAvailableInput=input
                                tablesAvailable=input.toIntOrNull()?:10
                            },
                            label = { Text("Broj stolova") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true

                        )
                    }
                },
                //kada se pritisne na dugme za potvrdu, dodaje se novi kafic na toj lokaciji
                confirmButton = {
                    Button(onClick = {
                        if(placeName.isNotEmpty() && placePhotoUri !=null && userLocation!=null){
                            val photoRef=storage.reference
                                .child("kafici_photos/${System.currentTimeMillis()}.jpg") //cisto da imamo razliku izmedju user slika i kafic slika
                            photoRef.putFile(placePhotoUri!!).addOnSuccessListener {
                                photoRef.downloadUrl.addOnSuccessListener { downloadUri->
                                    val kaficiData= hashMapOf(
                                        "name" to placeName,
                                        "photo" to downloadUri.toString(),
                                        "location" to hashMapOf(
                                            "lat" to userLocation!!.latitude,
                                            "lng" to userLocation!!.longitude
                                        ),
                                        "tables" to tablesAvailable
                                    )//upisivanje u bazu i takodje resetovanje alert dialog pop upa kako bi mogla da se doda nova lokacija
                                    firestore.collection("kafici")
                                        .add(kaficiData)
                                        .addOnSuccessListener {
                                            addPlace=false
                                            placeName=""
                                            placePhotoUri=null
                                            tablesAvailable=10
                                            tablesAvailableInput="10"
                                        }
                                        .addOnFailureListener{e->
                                            println("Firestore greska")
                                        }
                                        .addOnFailureListener{e->
                                    println("Firestore greska")
                                }.addOnFailureListener{e->
                                            println("Firestore greska")
                                        }
                                }
                            }
                        }

                    }){
                        Text("Dodaj")
                    }
                },
                dismissButton = {
                    Button(onClick = {addPlace=false}) {
                        Text("Otkazi")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
        if (radiusDialog){
            AlertDialog(
                onDismissRequest = {radiusDialog=false },
                title = { Text("Odaberite radijus") },
                text= {
                    Column {
                        Text("Radijus:${radiusKm.toInt()}")
                        Slider(
                            value = radiusKm,
                            onValueChange = { radiusKm = it },
                            valueRange = 1f..5f,
                            steps = 4
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {radiusDialog=false}) {
                        Text("Sacuvaj")
                    }
                },
                dismissButton = {
                    Button(onClick={radiusDialog=false}) {
                        Text("Otkazi")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
        // ako lokacija nije dobijena pored default kamere, korisnik dobija poruku da se lokacija ucitava
        if (userLocation == null) {
            Text("Dobijanje vaše lokacije...", modifier = Modifier.align(androidx.compose.ui.Alignment.Center))
        }
    }
}
