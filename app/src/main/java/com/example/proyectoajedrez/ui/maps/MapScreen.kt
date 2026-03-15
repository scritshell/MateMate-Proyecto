package com.example.proyectoajedrez.ui.maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@SuppressLint("MissingPermission") // Suprimimos el aviso porque controlamos el permiso manualmente
@Composable
fun MapScreen() {
    val context = LocalContext.current
    var hasLocationPermission by remember { mutableStateOf(false) }

    // Cliente para obtener las coordenadas reales del móvil
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Estado de la cámara. Por defecto, vista global
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f)
    }

    // Función auxiliar para centrar la cámara
    val centrarEnUsuario = {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            location?.let {
                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                    LatLng(it.latitude, it.longitude), 14f // Nivel de zoom 14 (nivel ciudad/barrio)
                )
            }
        }
    }

    // Lanzador para pedir permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
            if (isGranted) {
                centrarEnUsuario() // Si acepta, viajamos a su ubicación al instante
            }
        }
    )

    // Comprobamos el permiso al iniciar la pantalla
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
            centrarEnUsuario() // Si ya lo tenía concedido, viajamos directamente
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        properties = MapProperties(isMyLocationEnabled = hasLocationPermission),
        uiSettings = MapUiSettings(myLocationButtonEnabled = hasLocationPermission)
    ) {
        // NOTA: Estos marcadores de clubes están fijos a modo de demo

        Marker(
            state = MarkerState(position = LatLng(40.4200, -3.7000)),
            title = "Club Ajedrez Centro",
            snippet = "Torneos todos los viernes a las 18:00"
        )
        Marker(
            state = MarkerState(position = LatLng(40.4100, -3.7150)),
            title = "Peón Aislado",
            snippet = "Clases para principiantes. ¡Únete!"
        )
    }
}