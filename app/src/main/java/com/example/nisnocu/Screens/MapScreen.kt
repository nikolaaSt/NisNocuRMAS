package com.example.nisnocu.Screens

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import android.content.Context
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
import coil.compose.AsyncImage
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

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
    var kaficiList by remember{ mutableStateOf<List<Pair<String,Map<String,Any>>>>(emptyList()) }//pravi listu kafica koje cemo posle dobiti iz baze pozivanjem
    var selectedKafic by remember{ mutableStateOf<Pair<String, Map<String,Any>>?>(null)}

    val firestore=FirebaseFirestore.getInstance()
    val storage=FirebaseStorage.getInstance()
    val auth=FirebaseAuth.getInstance()
    val trenutniUserId=auth.currentUser?.uid?:"testUser"
    var userRezervisao by remember { mutableStateOf(false) }
    val notifiedCafes = remember { mutableStateOf(mutableSetOf<String>()) }

    fun Daljina(start:LatLng,end:LatLng): Double{
        val radijusZemlje=6371000.0
        val dLat=Math.toRadians(end.latitude-start.latitude)
        val dLng=Math.toRadians(end.longitude-start.longitude)
        val a=sin(dLat/2).pow(2.0)+
                cos(Math.toRadians(start.latitude))* cos(Math.toRadians(end.latitude))*
                sin(dLng/2).pow(2.0)
        val c=2* atan2(sqrt(a), sqrt(1-a))
        return radijusZemlje*c
    }

//naci objasnjenje za ovaj deo koda jer mi nista nije jasno
    LaunchedEffect(Unit) {
        firestore.collection("kafici").addSnapshotListener{snapshot,_->
            if(snapshot!=null){
                val docs=snapshot.documents.mapNotNull { doc->
                    val data=doc.data
                    if(data!=null) doc.id to data else null
                }
                kaficiList=docs
            }
        }
    }

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

    LaunchedEffect(Unit) {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val channel=NotificationChannel(
                "nearby_channel",
                "Kafici u blizini",
                NotificationManager.IMPORTANCE_HIGH)
                .apply { description="Notifikacije za kafice u radijusu" }
            val notificationManager:NotificationManager=
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)

        }
    }

    LaunchedEffect(kaficiList,userLocation,radiusKm) {
        while(true){
            val loc=userLocation
            if(loc!=null){

            kaficiList.forEach{(id,kafic)->
                val locationMap=kafic["location"] as? Map<*,*>
                val lat=locationMap?.get("lat") as? Double
                val lng=locationMap?.get("lng") as? Double
                val imeKafica=kafic["name"] as? String ?: "Nepoznato"

                if(lat!=null && lng !=null){
                    val kaficLoc=LatLng(lat,lng)
                    val distanca=Daljina(loc,kaficLoc)


                    if(distanca<=radiusKm*1000&&!notifiedCafes.value.contains(id)){
                        val builder=NotificationCompat.Builder(context,"nearby_channel")
                            .setSmallIcon(android.R.drawable.ic_dialog_info)
                            .setContentTitle("kakfic u blizini")
                            .setContentTitle("$imeKafica je unutar izabranog radijusa")
                            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                        val notificationManager=
                            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        notificationManager.notify(id.hashCode(),builder.build())

                        notifiedCafes.value.add(id)
                    }
                }
            }
        }
            kotlinx.coroutines.delay(10000L)
        }
    }

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
                    title = "Vasa lokacija",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)

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
            kaficiList.forEach{ (id,kafic)->
                val lat=(kafic["location"] as? Map<*,*>)?.get("lat") as? Double
                val lng=(kafic["location"] as? Map<*,*>)?.get("lng") as? Double
                val imeKafica=kafic["name"] as? String?:"Nepoznato"

                if(lat!=null && lng!=null){
                    Marker(
                        state=MarkerState(position = LatLng(lat,lng)),
                        title= imeKafica,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET),
                        onClick = {
                            selectedKafic=id to kafic
                            firestore.collection("kafici").document(id)
                                .collection("reservations").document(trenutniUserId)
                                .get().addOnSuccessListener { doc->
                                    userRezervisao=doc.exists()
                                }
                             //naci objasnjenje za ovaj deo koda
                       true }
                    )
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
                Button(onClick = { navController.navigate("User/$trenutniUserId") }) {
                    Text("Profile")
                }

                Button(onClick={navController.navigate("rangLista")}){
                    Text("Lista")
                }
            }
        }

        if(selectedKafic!=null){
            val(id,data)=selectedKafic!!
            val ime=data["name"] as? String ?:"Nepoznato"
            val photoUrl=data["photo"] as? String
            val tables=(data["tables"] as? Long)?.toInt() ?:0

            AlertDialog(
                onDismissRequest = {selectedKafic=null},
                title ={Text(ime)},
                text={
                    Column(horizontalAlignment = Alignment.CenterHorizontally){
                        photoUrl?.let{
                            AsyncImage(
                                model=it,
                                contentDescription = ime,
                                modifier = Modifier
                                    .height(150.dp)
                                    .fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row (
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ){
                            Text("Dostupno:$tables") //dodati da se tables updateuju kad se pritisne na rezervisi
                            Spacer(modifier=Modifier.height(8.dp))
                            Button(onClick = {
                                val KaficRef=firestore.collection("kafici").document(id)
                                val reservationRef=KaficRef.collection("reservations").document(trenutniUserId)

                                reservationRef.get().addOnSuccessListener { doc->
                                    if(!doc.exists()){
                                        reservationRef.set(mapOf("reserved" to true))
                                        KaficRef.update("tables",FieldValue.increment(-1))
                                        firestore.collection("users").document(trenutniUserId)
                                            .update("points",FieldValue.increment(1))
                                        userRezervisao=true
                                    }
                                }
                            },
                                enabled = !userRezervisao && tables>0
                                ) {
                                Text("Rezervisi")
                            }

                            Button(onClick = {
                                navController.navigate("Kafic/$id/$trenutniUserId")
                            }) {
                                Text("Detalji")
                            }

                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {selectedKafic=null}) {
                        Text("Zatvori")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
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
