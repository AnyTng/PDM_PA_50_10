package com.ipca.lojasocial

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.ipca.lojasocial.ui.theme.LojaSocialIPCATheme

//Imports de coisas inseguras
import javax.crypto.Cipher
import java.security.MessageDigest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LojaSocialIPCATheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Chamar as funções
        badCrypto()
        badHash()
    }

    // Teste 1.
    private fun badCrypto() {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        println(cipher) // só para usar a variável
    }

    // Teste 2-
    private fun badHash() {
        val md5 = MessageDigest.getInstance("MD5")
        println(md5) // só para usar a variável
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    LojaSocialIPCATheme {
        Greeting("Android")
    }
}