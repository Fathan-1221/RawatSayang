package com.rawatsayang.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ----- Model domain -----
data class DeviceLocation(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float? = null,
    val precise: Boolean = false,
    val waktu: Long = System.currentTimeMillis()
)

// ----- Repository lokasi (menyembunyikan FusedLocationProviderClient dari UI) -----
interface LocationRepository {
    suspend fun getCurrentLocation(): Result<DeviceLocation>
}

class DefaultLocationRepository(
    private val context: Context,
    private val client: FusedLocationProviderClient
) : LocationRepository {

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<DeviceLocation> = runCatching {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        check(fine || coarse) { "Location permission belum diberikan" }
        val priority = if (fine) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
        val location = client.getCurrentLocation(priority, null).await() ?: error("Lokasi belum tersedia. Pastikan layanan lokasi aktif.")
        DeviceLocation(location.latitude, location.longitude, location.accuracy, precise = fine)
    }
}

// ----- UI state -----
sealed interface LocationUiState {
    data object Idle : LocationUiState
    data object Loading : LocationUiState
    data class Success(val location: DeviceLocation) : LocationUiState
    data object Denied : LocationUiState
    data class Error(val message: String) : LocationUiState
}

class LocationViewModel(app: Application) : AndroidViewModel(app) {
    private val repository: LocationRepository = DefaultLocationRepository(
        app.applicationContext,
        LocationServices.getFusedLocationProviderClient(app.applicationContext)
    )
    private val _uiState = MutableStateFlow<LocationUiState>(LocationUiState.Idle)
    val uiState: StateFlow<LocationUiState> = _uiState.asStateFlow()

    fun refreshLocation() {
        viewModelScope.launch {
            _uiState.value = LocationUiState.Loading
            _uiState.value = repository.getCurrentLocation().fold(
                onSuccess = { LocationUiState.Success(it) },
                onFailure = { LocationUiState.Error(it.message ?: "Gagal mengambil lokasi") }
            )
        }
    }

    fun onPermissionDenied() { _uiState.value = LocationUiState.Denied }
}

private fun adaIzin(ctx: Context): Boolean =
    ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

@Composable
fun LocationScreen(vm: LocationViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val state by vm.uiState.collectAsState()
    val izin = arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { hasil ->
        val coarse = hasil[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val fine = hasil[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (coarse || fine) vm.refreshLocation() else vm.onPermissionDenied()
    }
    fun minta() {
        if (adaIzin(ctx)) vm.refreshLocation() else launcher.launch(izin)
    }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarKembali("Lokasi saya", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Panel("Lokasi kunjungan", "Lokasi hanya dibaca satu kali saat tombol ditekan (foreground, tanpa pelacakan terus-menerus).") {
                Tombol(if (state is LocationUiState.Success) "Perbarui Lokasi" else "Ambil Lokasi Saya") { minta() }
            }
            when (val s = state) {
                LocationUiState.Idle -> Text("Lokasi belum diminta.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                LocationUiState.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is LocationUiState.Success -> {
                    val l = s.location
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile("Latitude", "%.5f".format(l.latitude), Modifier.weight(1f))
                        Tile("Longitude", "%.5f".format(l.longitude), Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Tile("Akses", if (l.precise) "Precise" else "Approximate", Modifier.weight(1f))
                        Tile("Akurasi", "±${(l.accuracy ?: 0f).toInt()} m", Modifier.weight(1f))
                    }
                    Text(
                        "Terakhir diperbarui ${SimpleDateFormat("HH:mm:ss", Locale("id", "ID")).format(Date(l.waktu))}" +
                            if (!l.precise) ". Izin approximate: posisi kurang presisi." else "",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                LocationUiState.Denied -> {
                    StatusBanner(Level.WASPADA, "Izin lokasi ditolak", "Lokasi tidak dibaca tanpa izin. Fitur lain di aplikasi tetap berjalan normal.")
                    Tombol("Minta Izin Lagi") { launcher.launch(izin) }
                    TextButton(onClick = {
                        ctx.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                        )
                    }) { Text("Buka pengaturan izin") }
                }
                is LocationUiState.Error -> {
                    StatusBanner(Level.KRITIS, "Gagal mengambil lokasi", s.message)
                    Tombol("Coba Lagi") { minta() }
                }
            }
        }
    }
}
