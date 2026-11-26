package com.rawatsayang.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Peringatan lintas lansia: dipakai oleh lonceng di header dan panel di Beranda. */
@Composable
fun peringatanPerawat(vm: AppViewModel): List<Pair<Level, String>> {
    val daftar by vm.daftar.collectAsState()
    val terbaru by vm.ttvTerbaru.collectAsState()
    val obat by vm.semuaObat.collectAsState()
    val obatHariIni by vm.obatHariIni.collectAsState()
    val nama = daftar.associate { it.id to it.nama }
    val sekarang = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

    val hasil = mutableListOf<Pair<Level, String>>()
    terbaru.filter { it.level != "NORMAL" }.forEach {
        hasil += Level.valueOf(it.level) to "${nama[it.lansiaId] ?: "Lansia"}: status ${it.level.lowercase()} (poin bahaya ${it.poin})"
    }
    obat.filter { o -> o.jam <= sekarang && obatHariIni.none { it.obatId == o.id } }.forEach {
        hasil += Level.WASPADA to "Obat ${it.nama} untuk ${nama[it.lansiaId] ?: "lansia"} belum diberikan (jadwal ${it.jam})"
    }
    return hasil
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerawatHeader(vm: AppViewModel, nama: String, tab: Int, onKeluar: () -> Unit) {
    val peringatan = peringatanPerawat(vm)
    val daftar by vm.daftar.collectAsState()
    val terpilih by vm.terpilih.collectAsState()
    var buka by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf(false) }
    val tampilNama = nama.ifBlank { "Perawat" }
    val permukaan = MaterialTheme.colorScheme.surface

    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, Green)), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "RawatSayang · Perawat", Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f)
            )
            TextButton(onClick = onKeluar) { Text("Keluar", color = Color.White) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(
                    tampilNama.trim().firstOrNull()?.uppercase() ?: "P",
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White
                )
            }
            Column(Modifier.weight(1f)) {
                Text("${salamWaktu()},", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.75f))
                Text(
                    tampilNama, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(
                    onClick = { dialog = true },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                ) { Icon(Icons.Default.Notifications, contentDescription = "Notifikasi", tint = Color.White) }
                if (peringatan.isNotEmpty()) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Bad),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${peringatan.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Pemilih lansia hanya relevan di tab Periksa, Obat, dan Shift.
        if (tab in 1..3) {
            Text("Lansia yang diperiksa", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
            ExposedDropdownMenuBox(expanded = buka, onExpandedChange = { buka = it }) {
                OutlinedTextField(
                    value = terpilih?.nama ?: "Belum ada lansia", onValueChange = {}, readOnly = true,
                    singleLine = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buka) },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = permukaan, unfocusedContainerColor = permukaan,
                        unfocusedBorderColor = Color.Transparent, focusedBorderColor = Color.White
                    ),
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = buka, onDismissRequest = { buka = false }) {
                    daftar.forEach { p ->
                        DropdownMenuItem(
                            text = { Text("${p.nama} (${p.umur} th)") },
                            onClick = { vm.pilih(p.id); buka = false }
                        )
                    }
                }
            }
        }
    }

    if (dialog) {
        AlertDialog(
            onDismissRequest = { dialog = false },
            title = { Text("Notifikasi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (peringatan.isEmpty()) Text("Tidak ada peringatan saat ini.")
                    peringatan.forEach { (lv, teks) ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(Modifier.padding(top = 5.dp)) { Dot(lv) }
                            Text(teks, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { dialog = false }) { Text("Tutup") } }
        )
    }
}
