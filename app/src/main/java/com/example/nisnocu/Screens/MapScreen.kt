package com.example.nisnocu.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.location.Location
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.*
import kotlin.math.*

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(navController: NavHostController) {
    val context = LocalContext.current
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    var tablesAvailableInput by remember { mutableStateOf("10") }
    var tablesAvailable by remember { mutableStateOf(10) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var addPlace by remember { mutableStateOf(false) }
    var placePhotoUri by remember { mutableStateOf<Uri?>(null) }
    var placeName by remember { mutableStateOf("") }
    var radiusKm by remember { mutableStateOf(1f) } //neka bude radijus po default 1km
    var radiusDialog by remember { mutableStateOf(false) }
    var kaficiList by remember { mutableStateOf<List<Pair<String, Map<String, Any>>>>(emptyList()) } //pravi listu kafica koje cemo posle dobiti iz baze pozivanjem
    var selectedKafic by remember { mutableStateOf<Pair<String, Map<String, Any>>?>(null) }

    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    val auth = FirebaseAuth.getInstance()
    val trenutniUserId = auth.currentUser?.uid ?: "testUser"
    var userRezervisao by remember { mutableStateOf(false) }
    val notifiedCafes = remember { mutableStateOf(mutableSetOf<String>()) }

    var searchQuery by remember { mutableStateOf("") }
    var showFilterDialog by remember { mutableStateOf(false) }
    var minRating by remember { mutableStateOf(0f) }
    var selectedUser by remember { mutableStateOf<String?>(null) }
    var allUsers by remember { mutableStateOf(listOf<Pair<String, String>>()) }

    // Funkcija za racunanje distance izmedju dve tacke
    fun Daljina(start: LatLng, end: LatLng): Double {
        val radijusZemlje = 6371000.0
        val dLat = Math.toRadians(end.latitude - start.latitude)
        val dLng = Math.toRadians(end.longitude - start.longitude)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(start.latitude)) * cos(Math.toRadians(end.latitude)) *
                sin(dLng / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radijusZemlje * c
    }

    //naci objasnjenje za ovaj deo koda jer mi nista nije jasno
    LaunchedEffect(Unit) {
        firestore.collection("kafici").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                val docs = snapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) doc.id to data else null
                }
                kaficiList = docs
            }
        }
        //dobijamo username od svih usera u bazi za popup filter
        firestore.collection("users").addSnapshotListener { snapshot, _ ->
            if (snapshot != null) {
                allUsers = snapshot.documents.mapNotNull { doc ->
                    val username = doc.getString("username") ?: return@mapNotNull null
                    Pair(doc.id, username)
                }
            }
        }
    }

    //filtrirani kafici za po korisniku, searchu ili ratingu(rating kao minimum rating npr user stavi 2 onda prikazuje 2+ zvezdice rating na mapi)
    val filteredCafes = kaficiList.filter { (id, data) ->
        val name = data["name"] as? String ?: ""
        val addedBy = data["addedBy"] as? String
        val avgRating = (data["averageRating"] as? Double) ?: 0.0

        val matchesSearch = name.contains(searchQuery, ignoreCase = true)
        val matchesUser = selectedUser?.let { addedBy == it } ?: true
        val matchesRating = avgRating >= minRating

        matchesSearch && matchesUser && matchesRating
    }

    // Ovo je da bi se pokrenuo dijalog za permisiju
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) {
                // ukoliko dobijemo permisiju uzimamo za user location koordinate
                fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
                    location?.let { userLocation = LatLng(it.latitude, it.longitude) }
                }
            }
        }
    )

    // Kreiranje notification channel-a
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "nearby_channel",
                "Kafici u blizini",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifikacije za kafice u radijusu" }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    //notifikacija stize za kafic koji je u radijusu
    LaunchedEffect(kaficiList, userLocation, radiusKm) {
        while (true) {
            val loc = userLocation
            if (loc != null) {
                kaficiList.forEach { (id, kafic) ->
                    val locationMap = kafic["location"] as? Map<*, *>
                    val lat = locationMap?.get("lat") as? Double
                    val lng = locationMap?.get("lng") as? Double
                    val imeKafica = kafic["name"] as? String ?: "Nepoznato"

                    if (lat != null && lng != null) {
                        val kaficLoc = LatLng(lat, lng)
                        val distanca = Daljina(loc, kaficLoc)

                        if (distanca <= radiusKm * 1000 && !notifiedCafes.value.contains(id)) {
                            val builder = NotificationCompat.Builder(context, "nearby_channel")
                                .setSmallIcon(android.R.drawable.ic_dialog_info)
                                .setContentTitle("$imeKafica je unutar izabranog radijusa")
                                .setPriority(NotificationCompat.PRIORITY_HIGH)

                            val notificationManager =
                                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                            notificationManager.notify(id.hashCode(), builder.build())

                            notifiedCafes.value.add(id)
                        }
                    }
                }
            }
            kotlinx.coroutines.delay(10000L)
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> placePhotoUri = uri }

    // Znaci ovo je launcher za permisiju za koriscenje lokacije(ima i compatibility resenje za starije verzije
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Kamera se pozicionira na 0 duzinu i 0 sirinu bez lokacije korisnika ili dok je ne dobija
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation ?: LatLng(0.0, 0.0), 1f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(isMyLocationEnabled = true)
        ) {
            // Ako imamo lokaciju onda se stavlja marker i na njegov stisak dobijamo naslov Vasa lokacija
            userLocation?.let { location ->
                Marker(
                    state = MarkerState(position = location),
                    title = "Vasa lokacija",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )

                Circle(
                    center = location,
                    radius = (radiusKm * 1000).toDouble(),
                    strokeColor = Color.Blue,
                    strokeWidth = 2f,
                    fillColor = Color.Blue.copy(alpha = 0.2f)
                )

                // Pomera se kamera na lokaciju koja je dobijena
                LaunchedEffect(location) {
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }

            // Dodavanje markera za filtrirane kafice
            filteredCafes.forEach { (id, kafic) ->
                val lat = (kafic["location"] as? Map<*, *>)?.get("lat")?.let { (it as Number).toDouble() }
                val lng = (kafic["location"] as? Map<*, *>)?.get("lng").let { (it as Number).toDouble() }
                val imeKafica = kafic["name"] as? String ?: "Nepoznato"

                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = imeKafica,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                        onClick = {
                            selectedKafic = id to kafic
                            firestore.collection("kafici").document(id)
                                .collection("reservations").document(trenutniUserId)
                                .get().addOnSuccessListener { doc ->
                                    userRezervisao = doc.exists()
                                }
                            true
                        }
                    )
                }
            }
        }

        // Dugmad za navigaciju i akcije
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { addPlace = true }) { Text("+") }
                Button(onClick = { radiusDialog = true }) { Text("R") }
                Button(onClick = { navController.navigate("User/$trenutniUserId") }) { Text("Profile") }
                Button(onClick = { navController.navigate("rangLista") }) { Text("Lista") }
                Button(
                    onClick = {
                        userLocation?.let { loc ->
                            navController.navigate("search/$trenutniUserId/${loc.latitude}/${loc.longitude}")
                        }
                    },
                    enabled = userLocation != null
                ) { Text("Search") }
            }
        }

        // Search bar i filter dugme
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = { showFilterDialog = true }) { Text("Filters") }
            }
        }

        // -------- ALERTDIALOGS --------

        // Filter dialog
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filteri") },
                text = {
                    Column {
                        Text("Minimalna ocena: ${minRating.toInt()}")
                        Slider(
                            value = minRating,
                            onValueChange = { minRating = it },
                            valueRange = 0f..5f,
                            steps = 5
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Filter po korisniku:")
                        LazyColumn(Modifier.heightIn(max = 200.dp)) {
                            items(allUsers) { (userId, username) ->
                                Text(
                                    text = username,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedUser =
                                                if (selectedUser == userId) null else userId
                                        }
                                        .padding(8.dp),
                                    color = if (selectedUser == userId) Color.Blue else Color.Unspecified
                                )
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { showFilterDialog = false }) { Text("Primeni") } },
                dismissButton = { Button(onClick = { showFilterDialog = false }) { Text("Otkaži") } },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Selected kafic details
        if (selectedKafic != null) {
            val (id, data) = selectedKafic!!
            val ime = data["name"] as? String ?: "Nepoznato"
            val photoUrl = data["photo"] as? String
            val tables = (data["tables"] as? Long)?.toInt() ?: 0

            AlertDialog(
                onDismissRequest = { selectedKafic = null },
                title = { Text(ime) },
                text = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        photoUrl?.let {
                            AsyncImage(
                                model = it,
                                contentDescription = ime,
                                modifier = Modifier
                                    .height(150.dp)
                                    .fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Dostupno:$tables") //dodati da se tables updateuju kad se pritisne na rezervisi
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val KaficRef = firestore.collection("kafici").document(id)
                                    val reservationRef =
                                        KaficRef.collection("reservations").document(trenutniUserId)

                                    reservationRef.get().addOnSuccessListener { doc ->
                                        if (!doc.exists()) {
                                            reservationRef.set(mapOf("reserved" to true))
                                            KaficRef.update("tables", FieldValue.increment(-1))
                                            firestore.collection("users")
                                                .document(trenutniUserId)
                                                .update("points", FieldValue.increment(1))
                                            userRezervisao = true
                                        }
                                    }
                                },
                                enabled = !userRezervisao && tables > 0
                            ) { Text("Rezervisi") }

                            Button(onClick = { navController.navigate("Kafic/$id/$trenutniUserId") }) {
                                Text("Detalji")
                            }
                        }
                    }
                },
                confirmButton = { Button(onClick = { selectedKafic = null }) { Text("Zatvori") } },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Add place dialog
        if (addPlace) {
            AlertDialog(
                onDismissRequest = { addPlace = false },
                title = { Text("Dodaj mesto") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = placeName,
                            onValueChange = { placeName = it },
                            label = { Text("Naziv mesta") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { photoLauncher.launch("image/*") }) {
                            Text(if (placePhotoUri == null) "Dodaj fotografiju" else "Izabrana fotografija")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = tablesAvailableInput,
                            onValueChange = { input ->
                                tablesAvailableInput = input
                                tablesAvailable = input.toIntOrNull() ?: 10
                            },
                            label = { Text("Broj stolova") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        if (placeName.isNotEmpty() && placePhotoUri != null && userLocation != null) {
                            val photoRef =
                                storage.reference.child("kafici_photos/${System.currentTimeMillis()}.jpg")
                            photoRef.putFile(placePhotoUri!!).addOnSuccessListener {
                                photoRef.downloadUrl.addOnSuccessListener { downloadUri ->
                                    val kaficiData = hashMapOf(
                                        "name" to placeName,
                                        "photo" to downloadUri.toString(),
                                        "location" to hashMapOf(
                                            "lat" to userLocation!!.latitude,
                                            "lng" to userLocation!!.longitude
                                        ),
                                        "tables" to tablesAvailable,
                                        "addedBy" to trenutniUserId,
                                        "createdAt" to FieldValue.serverTimestamp(),
                                        "averageRating" to 0.0,
                                        "ratingsCount" to 0
                                    )
                                    firestore.collection("kafici").add(kaficiData)
                                        .addOnSuccessListener {
                                            addPlace = false
                                            placeName = ""
                                            placePhotoUri = null
                                            tablesAvailable = 10
                                            tablesAvailableInput = "10"
                                        }
                                        .addOnFailureListener { e -> println("Firestore greska: $e") }
                                }
                            }
                        }
                    }) { Text("Dodaj") }
                },
                dismissButton = { Button(onClick = { addPlace = false }) { Text("Otkaži") } },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Radius selection dialog
        if (radiusDialog) {
            AlertDialog(
                onDismissRequest = { radiusDialog = false },
                title = { Text("Izaberi radijus (km)") },
                text = {
                    Column {
                        Text("${radiusKm} km")
                        Slider(
                            value = radiusKm,
                            onValueChange = { radiusKm = it },
                            valueRange = 0.1f..10f,
                            steps = 99
                        )
                    }
                },
                confirmButton = { Button(onClick = { radiusDialog = false }) { Text("Potvrdi") } },
                dismissButton = { Button(onClick = { radiusDialog = false }) { Text("Otkaži") } },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // Poruka kada se korisnikova lokacija ucitava
        if (userLocation == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("Dobijanje vaše lokacije...")
            }
        }
    }
}
