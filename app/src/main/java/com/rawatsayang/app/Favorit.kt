package com.rawatsayang.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TopBarKembali(judul: String, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, Green)), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White) }
        Text(judul, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
    }
}

@Composable
fun DetailScreen(vm: AppViewModel, id: Long, onBack: () -> Unit) {
    LaunchedEffect(id) { vm.pilih(id) }
    val l by vm.terpilih.collectAsState()
    val favorit by vm.isFavorit.collectAsState()
    val ttv by vm.ttvLogs.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarKembali("Detail lansia", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val p = l
            if (p == null || p.id != id) {
                Kosong("Memuat data lansia...")
            } else {
                Panel(p.nama, "${p.umur} tahun" + if (p.kamar.isNotEmpty()) " · Kamar ${p.kamar}" else "") {
                    Tombol(if (favorit) "Hapus dari Favorit" else "Simpan ke Favorit") { vm.toggleFavorit(p) }
                }
                val t0 = ttv.firstOrNull()
                if (t0 == null) {
                    Kosong("Belum ada data tanda vital.")
                } else {
                    val lv = Level.valueOf(t0.level)
                    StatusBanner(
                        lv,
                        when (lv) { Level.NORMAL -> "Status terakhir: Normal"; Level.WASPADA -> "Status terakhir: Waspada"; Level.KRITIS -> "Status terakhir: Kritis" },
                        "Poin bahaya ${t0.poin}. Diperbarui ${t0.waktu.tgl()}."
                    )
                }
                Riwayat("Riwayat TTV", ttv.take(5).map {
                    Triple(it.waktu.tgl(), "${it.sistolik} mmHg · ${it.gula} mg/dL · ${it.suhu} °C · tidur ${it.jamTidur} jam", Level.valueOf(it.level))
                })
            }
        }
    }
}

@Composable
fun FavoritScreen(vm: AppViewModel, onBack: () -> Unit, onDetail: (Long) -> Unit) {
    val favorit by vm.favorit.collectAsState()
    val terbaru by vm.ttvTerbaru.collectAsState()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarKembali("Lansia favorit", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (favorit.isEmpty()) Kosong("Belum ada lansia favorit. Buka Detail lansia lalu tekan Simpan ke Favorit.")
            favorit.forEach { p ->
                val lv = terbaru.firstOrNull { it.lansiaId == p.id }?.let { Level.valueOf(it.level) }
                Card(
                    onClick = { onDetail(p.id) },
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(p.nama, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(
                                "${p.umur} tahun" + if (p.kamar.isNotEmpty()) " · Kamar ${p.kamar}" else "",
                                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lv != null) Dot(lv)
                    }
                }
            }
        }
    }
}
