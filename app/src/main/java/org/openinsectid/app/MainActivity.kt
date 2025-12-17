package org.openinsectid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import org.openinsectid.app.data.ImageStore
import org.openinsectid.app.ui.screens.MainNavHost
import org.openinsectid.app.ui.theme.OpenInsectIdTheme
import org.openinsectid.app.utils.InferenceManager

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        // Load model and classes
        InferenceManager.init(this)

        ImageStore.ensureImagesDir(applicationContext)

        enableEdgeToEdge()
        setContent {
            OpenInsectIdTheme {
                val navController = rememberNavController()
                MainNavHost(navController = navController)
            }
        }
    }
}
