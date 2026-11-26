package com.rawatsayang.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val izinNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= 33) izinNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { RawatSayangTheme { Root() } }
    }
}

object Rute {
    const val WELCOME = "welcome"
    const val DAFTAR = "daftar"
    const val MASUK = "masuk"
    const val PERAN = "peran"
    const val PERAWAT = "perawat"
    const val KELUARGA = "keluarga/{lansiaId}"
    const val FAVORIT = "favorit"
    const val DETAIL = "detail/{id}"
    const val LOKASI = "lokasi"
    fun detail(id: Long) = "detail/$id"
    fun keluarga(id: Long) = "keluarga/$id"
}

@Composable
fun Root(vm: AppViewModel = viewModel(), auth: AuthViewModel = viewModel()) {
    val nav = rememberNavController()
    val keluar: () -> Unit = { nav.popBackStack(Rute.WELCOME, false) }
    NavHost(navController = nav, startDestination = Rute.WELCOME) {
        composable(Rute.WELCOME) {
            // Kembali ke layar awal selalu mengakhiri sesi.
            LaunchedEffect(Unit) { auth.keluar() }
            WelcomeScreen(
                onDaftar = { nav.navigate(Rute.DAFTAR) },
                onMasuk = { nav.navigate(Rute.MASUK) }
            )
        }
        composable(Rute.DAFTAR) {
            DaftarScreen(
                auth, onBack = { nav.popBackStack() },
                onMasuk = { nav.navigate(Rute.MASUK) { popUpTo(Rute.DAFTAR) { inclusive = true } } },
                onBerhasil = { nav.navigate(Rute.PERAN) { popUpTo(Rute.WELCOME) } }
            )
        }
        composable(Rute.MASUK) {
            MasukScreen(
                auth, onBack = { nav.popBackStack() },
                onDaftar = { nav.navigate(Rute.DAFTAR) { popUpTo(Rute.MASUK) { inclusive = true } } },
                onBerhasil = { nav.navigate(Rute.PERAN) { popUpTo(Rute.WELCOME) } }
            )
        }
        composable(Rute.PERAN) {
            PeranScreen(auth, vm, onBack = { nav.popBackStack() }) { peran, lansiaId ->
                val tujuan = if (peran == PERAN_KELUARGA) Rute.keluarga(lansiaId) else Rute.PERAWAT
                nav.navigate(tujuan) { popUpTo(Rute.PERAN) { inclusive = true } }
            }
        }
        composable(Rute.PERAWAT) {
            val nama = remember { auth.akun.value?.nama.orEmpty() }
            PerawatApp(
                vm, nama,
                onKeluar = keluar,
                onDetail = { nav.navigate(Rute.detail(it)) },
                onFavorit = { nav.navigate(Rute.FAVORIT) },
                onLokasi = { nav.navigate(Rute.LOKASI) }
            )
        }
        composable(Rute.KELUARGA, arguments = listOf(navArgument("lansiaId") { type = NavType.LongType })) { entry ->
            val id = entry.arguments?.getLong("lansiaId") ?: 0L
            val nama = remember { auth.akun.value?.nama.orEmpty() }
            KeluargaApp(vm, id, nama, keluar)
        }
        composable(Rute.FAVORIT) {
            FavoritScreen(vm, onBack = { nav.popBackStack() }, onDetail = { nav.navigate(Rute.detail(it)) })
        }
        composable(Rute.DETAIL, arguments = listOf(navArgument("id") { type = NavType.LongType })) { entry ->
            DetailScreen(vm, entry.arguments?.getLong("id") ?: 0L) { nav.popBackStack() }
        }
        composable(Rute.LOKASI) {
            val lvm: LocationViewModel = viewModel()
            LocationScreen(lvm) { nav.popBackStack() }
        }
    }
}
