package com.ipca.lojasocial

import android.database.sqlite.SQLiteDatabase          // 🔴 ADICIONADO, APAGARRRRRR
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

        // Apagar isto mais tarde, teste do CodeQL
        vulnerableQuery()
    }

    // Apagar assim que puder
    private fun vulnerableQuery() {

        val username = intent.getStringExtra("username") ?: ""

        // DB de teste local
        val db: SQLiteDatabase = openOrCreateDatabase("test.db", MODE_PRIVATE, null)


        val query = "SELECT * FROM users WHERE name = '$username'"

        db.rawQuery(query, null)
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