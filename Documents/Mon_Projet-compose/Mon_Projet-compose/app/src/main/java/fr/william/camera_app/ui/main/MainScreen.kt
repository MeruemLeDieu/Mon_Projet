package fr.william.camera_app.ui.main

import androidx.compose.material3.Button
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import fr.william.camera_app.ui.camera.CameraScreen


@Composable
fun MainScreen(
    widthSizeClass: WindowWidthSizeClass,
    navController: NavHostController = rememberNavController(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    val isExpandedScreen = widthSizeClass == WindowWidthSizeClass.Expanded

    NavHost(
        navController = navController,
        startDestination = "camera"
    ) {
        composable("camera") {
            CameraScreen(
                isExpandedScreen = isExpandedScreen,
            )
        }
        composable("connexion") {
            ConnexionScreen(
                onNavigateToCamera = {
                    navController.navigate("camera")
                }
            )
        }
    }
}

@Composable
fun ConnexionScreen(
    onNavigateToCamera: () -> Unit = {}
) {
    Button(onClick = onNavigateToCamera) {
        "Connexion"
    }
}