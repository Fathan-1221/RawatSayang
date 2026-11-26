package com.rawatsayang.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private data class Kunjungan(val waktu: Long, val judul: String, val foto: String)

fun salamWaktu(): String {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when (h) {
        in 4..10 -> "Selamat pagi"
        in 11..14 -> "Selamat siang"
        in 15..17 -> "Selamat sore"
        else -> "Selamat malam"
    }
}

/** "Ibu Sari Wulandari" -> "Ibu Sari", "Sutrisno Adi" -> "Sutrisno". */
fun panggilan(nama: String): String {
    val kata = nama.trim().split(" ").filter { it.isNotEmpty() }
    if (kata.isEmpty()) return "lansia"
    val gelar = setOf("ibu", "bapak", "oma", "opa", "nenek", "kakek", "mbah", "bu", "pak")
    return if (kata.size > 1 && kata[0].lowercase() in gelar) "${kata[0]} ${kata[1]}" else kata[0]
}

@Composable
private fun notifikasiKeluarga(vm: AppViewModel): List<String> {
    val ttv by vm.ttvLogs.collectAsState()
    val obat by vm.obatLansia.collectAsState()
    val hariIni by vm.obatHariIni.collectAsState()
    val sekarang = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    val hasil = mutableListOf<String>()
    ttv.firstOrNull()?.let {
        if (it.level != "NORMAL") hasil += "Kondisi terakhir berstatus ${it.level.lowercase()} (${it.waktu.tgl()})."
    }
    obat.filter { o -> o.jam <= sekarang && hariIni.none { it.obatId == o.id } }.forEach {
        hasil += "Obat ${it.nama} (jadwal ${it.jam}) belum tercatat diberikan hari ini."
    }
    return hasil
}

@Composable
fun BerandaHeader(vm: AppViewModel, nama: String, onKeluar: () -> Unit) {
    val notif = notifikasiKeluarga(vm)
    var buka by remember { mutableStateOf(false) }
    val tampilNama = nama.ifBlank { "Keluarga" }

    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, Green)), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "RawatSayang", Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.75f)
            )
            TextButton(onClick = onKeluar) { Text("Keluar", color = Color.White) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Text(
                    tampilNama.trim().firstOrNull()?.uppercase() ?: "K",
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
                    onClick = { buka = true },
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f))
                ) { Icon(Icons.Default.Notifications, contentDescription = "Notifikasi", tint = Color.White) }
                if (notif.isNotEmpty()) {
                    Box(
                        Modifier.align(Alignment.TopEnd).size(20.dp).clip(CircleShape).background(Bad),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${notif.size}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    if (buka) {
        AlertDialog(
            onDismissRequest = { buka = false },
            title = { Text("Notifikasi") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (notif.isEmpty()) Text("Tidak ada notifikasi saat ini.")
                    notif.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            },
            confirmButton = { TextButton(onClick = { buka = false }) { Text("Tutup") } }
        )
    }
}

@Composable
private fun KartuFoto(k: Kunjungan) {
    Column(Modifier.width(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) { FotoBukti(k.foto, Modifier.fillMaxSize()) }
        Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
            Text(
                k.judul, Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis
            )
        }
        Text(k.waktu.tgl(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun BerandaKeluarga(vm: AppViewModel, l: Lansia, onLihatBukti: () -> Unit) {
    val shift by vm.shiftNotes.collectAsState()
    val obatLog by vm.obatLogs.collectAsState()
    var reaksi by rememberSaveable { mutableIntStateOf(-1) }
    val sebut = panggilan(l.nama)
    val primer = MaterialTheme.colorScheme.primary

    val kunjungan = (shift.mapNotNull { n -> n.fotoPath?.let { Kunjungan(n.waktu, "Shift ${n.shift}", it) } } +
        obatLog.mapNotNull { o -> o.fotoPath?.let { Kunjungan(o.waktu, "Obat ${o.namaObat}", it) } })
        .sortedByDescending { it.waktu }.take(6)

    Text(
        "Satu kunjungan kecil bisa jadi momen besar bagi $sebut.",
        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = primer
    )
    Text(
        "Yuk, luangkan waktu sebentar untuk hadir dan berbagi senyum hari ini 💚",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Lihat senyum $sebut di kunjungan terakhir", Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = primer
        )
        Icon(Icons.Default.Favorite, null, tint = primer.copy(alpha = 0.4f), modifier = Modifier.size(32.dp))
    }

    if (kunjungan.isEmpty()) {
        Box(
            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceVariant).padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Belum ada foto kunjungan. Foto dari catatan shift dan pemberian obat akan tampil di sini.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            kunjungan.forEach { KartuFoto(it) }
        }
    }

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf("😍", "💖", "🥹", "😭").forEachIndexed { i, emoji ->
            val aktif = reaksi == i
            val bentuk = RoundedCornerShape(16.dp)
            Box(
                Modifier.weight(1f).height(48.dp).clip(bentuk)
                    .background(if (aktif) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                    .border(1.dp, if (aktif) primer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), bentuk)
                    .clickable { reaksi = if (aktif) -1 else i },
                contentAlignment = Alignment.Center
            ) { Text(emoji, fontSize = 22.sp) }
        }
    }

    Button(onClick = onLihatBukti, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Icon(Icons.Default.DateRange, null)
        Spacer(Modifier.width(8.dp))
        Text("Lihat Kunjungan Lainnya", fontWeight = FontWeight.SemiBold)
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Default.Edit, null, tint = primer) }
        Text("Catatan Terakhir Perawat", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
    val catatan = shift.firstOrNull()
    if (catatan == null) {
        Kosong("Belum ada catatan dari perawat untuk $sebut.")
    } else {
        KartuCatatan("Shift ${catatan.shift}", catatan.waktu.tgl(), catatan.isi, null)
    }
}
