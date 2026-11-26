package com.rawatsayang.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun KeluargaApp(vm: AppViewModel, lansiaId: Long, namaPengguna: String, onKeluar: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val terpilih by vm.terpilih.collectAsState()
    LaunchedEffect(lansiaId) { vm.pilih(lansiaId) }
    // Keluarga hanya boleh melihat lansia yang terhubung dengan akunnya.
    val milik = terpilih?.takeIf { it.id == lansiaId }
    val menu = listOf(
        "Beranda" to Icons.Default.Home, "Status" to Icons.Default.Favorite,
        "Panduan" to Icons.Default.CheckCircle, "Bukti" to Icons.Default.DateRange
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { BarMenu(menu, tab) { tab = it } }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
            if (tab == 0) BerandaHeader(vm, namaPengguna, onKeluar)
            else Header("Dashboard keluarga", "Bagaimana kabar lansia Anda?", vm, onKeluar, bisaPilih = false)
            AnimatedContent(targetState = tab, label = "tab", modifier = Modifier.weight(1f)) { t ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (milik == null) Kosong("Data lansia tidak ditemukan atau sedang dimuat. Hubungi perawat jika masalah berlanjut.")
                    else when (t) {
                        0 -> BerandaKeluarga(vm, milik) { tab = 3 }
                        1 -> StatusKeluarga(vm, milik)
                        2 -> PanduanKeluarga(vm)
                        else -> BuktiKeluarga(vm)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusKeluarga(vm: AppViewModel, l: Lansia) {
    val ttv by vm.ttvLogs.collectAsState()
    val akt by vm.aktivitasLogs.collectAsState()
    val t0 = ttv.firstOrNull()
    if (t0 == null) {
        Kosong("Perawat belum mengevaluasi tanda vital ${l.nama}.")
    } else {
        val lv = Level.valueOf(t0.level)
        StatusBanner(
            lv,
            when (lv) { Level.NORMAL -> "Kondisi terakhir: Normal"; Level.WASPADA -> "Kondisi terakhir: Waspada"; Level.KRITIS -> "Kondisi terakhir: Kritis" },
            when (lv) {
                Level.NORMAL -> "Tanda vital ${l.nama} dalam batas aman."
                Level.WASPADA -> "${l.nama} sedang dipantau perawat."
                Level.KRITIS -> "${l.nama} membutuhkan penanganan tenaga medis segera."
            } + " Diperbarui ${t0.waktu.tgl()}."
        )
    }
    if (ttv.size >= 7) {
        val data = ttv.take(7).reversed().map { it.sistolik }
        val tren = hitungTren(data)
        Panel("Tren kesehatan mingguan", "Tekanan darah sistolik 7 data terakhir (mmHg)") {
            LineChart(data, Modifier.fillMaxWidth().height(170.dp))
            Text("Rata-rata: ${tren.rataRata.f1()} mmHg", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
        StatusBanner(
            if (tren.membaik) Level.NORMAL else Level.KRITIS,
            if (tren.membaik) "Perkembangan: MEMBAIK" else "Perkembangan: MENURUN"
        )
    }
    akt.firstOrNull()?.let { StatusBanner(Level.valueOf(it.level), "Catatan aktivitas: ${it.judul}", it.pesan) }
}

@Composable
fun PanduanKeluarga(vm: AppViewModel) {
    val porsi by vm.porsiLogs.collectAsState()
    val p = porsi.firstOrNull()
    if (p == null) {
        Kosong("Panduan aktivitas muncul setelah perawat menghitung porsi olahraga.")
    } else {
        StatusBanner(Level.valueOf(p.level), p.kondisi, "Fokus hari ini: ${p.fokus}. Diperbarui ${p.waktu.tgl()}.")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Tile("Jalan kaki", "${p.jalan} menit", Modifier.weight(1f))
            Tile("Senam", "${p.senam} menit", Modifier.weight(1f))
        }
    }
}

@Composable
fun BuktiKeluarga(vm: AppViewModel) {
    val shift by vm.shiftNotes.collectAsState()
    val obat by vm.obatLogs.collectAsState()
    data class Bukti(val waktu: Long, val judul: String, val isi: String, val foto: String?)
    val semua = (shift.map { Bukti(it.waktu, "Catatan shift ${it.shift}", it.isi, it.fotoPath) } +
        obat.map { Bukti(it.waktu, "Obat diberikan: ${it.namaObat}", "", it.fotoPath) }).sortedByDescending { it.waktu }
    if (semua.isEmpty()) Kosong("Belum ada catatan atau foto bukti perawatan.")
    semua.take(15).forEach { KartuCatatan(it.judul, it.waktu.tgl(), it.isi, it.foto) }
}
