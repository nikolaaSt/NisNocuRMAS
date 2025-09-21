package com.example.nisnocu.Screens



import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth

@Composable
fun LoginScreen(
    navController: NavHostController
) {
    var username by remember { mutableStateOf("") }
    var password by remember{ mutableStateOf("") }
    val context= LocalContext.current
    val auth=FirebaseAuth.getInstance()




    Box(
        modifier= Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ){
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(text="Username", fontSize = 16.sp)

            Spacer(modifier=Modifier.height(20.dp))

            TextField(
                value=username,
                onValueChange = {username=it},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(text="Password", fontSize = 16.sp)
            Spacer(modifier = Modifier.height(20.dp))

            TextField(
                value=password,
                onValueChange = {password=it},
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )

            Spacer(modifier=Modifier.height(24.dp))

            Button(
                onClick = {
                    if(username.isNotEmpty()&&password.isNotEmpty()){
                        val fakeEmail="$username@app.com"
                        auth.signInWithEmailAndPassword(fakeEmail,password)
                            .addOnSuccessListener {
                                Toast.makeText(context,"Uspesna prijava!", Toast.LENGTH_SHORT).show()
                                navController.navigate("Map")
                            }
                            .addOnFailureListener{e->
                                Toast.makeText(context,"greska pri prijavljivanju:${e.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else{
                      Toast.makeText(context, "Popunite oba polja", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Login")
            }

            Text(
                text="Nemate nalog, ulogujte se ovde",
                modifier = Modifier.clickable { navController.navigate("register") },
                color = MaterialTheme.colorScheme.primary
            )
        }
    }

}