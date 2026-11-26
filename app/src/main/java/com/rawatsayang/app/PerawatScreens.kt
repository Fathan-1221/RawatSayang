package com.rawatsayang.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PerawatApp(
    vm: AppViewModel, nama: String, onKeluar: () -> Unit, onDetail: (Long) -> Unit, onFavorit: () -> Unit,
    onLokasi: () -> Unit
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val l by vm.terpilih.collectAsState()
    val menu = listOf(
        "Beranda" to Icons.Default.Home, "Periksa" to Icons.Default.Favorite, "Obat" to IkonObat,
        "Shift" to Icons.Default.Edit, "Lansia" to Icons.Default.Person
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { BarMenu(menu, tab) { tab = it } }
    ) { pad ->
        Column(Modifier.fillMaxSize().padding(bottom = pad.calculateBottomPadding())) {
            PerawatHeader(vm, nama, tab, onKeluar)
            AnimatedContent(targetState = tab, label = "tab", modifier = Modifier.weight(1f)) { t ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    when (t) {
                        0 -> Beranda(vm, onLokasi) { tab = 4 }
                        1 -> PeriksaScreen(vm, l)
                        2 -> ObatScreen(vm, l)
                        3 -> ShiftScreen(vm, l)
                        else -> LansiaScreen(vm, onDetail, onFavorit)
                    }
                }
            }
        }
    }
}

// ---------- Beranda: ringkasan + pengingat ----------

@Composable
fun Beranda(vm: AppViewModel, onLokasi: () -> Unit, onTambah: () -> Unit) {
    val daftar by vm.daftar.collectAsState()
    val terbaru by vm.ttvTerbaru.collectAsState()
    val shift by vm.shiftTerbaru.collectAsState()
    val nama = daftar.associate { it.id to it.nama }
    val peringatan = peringatanPerawat(vm)

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Tile("Lansia", "${daftar.size}", Modifier.weight(1f))
        Tile("Kritis", "${terbaru.count { it.level == "KRITIS" }}", Modifier.weight(1f))
        Tile("Waspada", "${terbaru.count { it.level == "WASPADA" }}", Modifier.weight(1f))
    }
    Panel("Lansia bertambah?", "Daftarkan lansia baru agar bisa diperiksa dan dipantau") {
        Tombol("Tambah Lansia Baru") { onTambah() }
    }
    Panel("Lokasi kunjungan", "Cek posisi perangkat perawat saat kunjungan") {
        Tombol("Buka Lokasi") { onLokasi() }
    }
    Panel("Pengingat dan peringatan", "Berdasarkan pemeriksaan terakhir dan jadwal obat hari ini") {
        if (peringatan.isEmpty()) Text("Tidak ada peringatan saat ini.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        peringatan.forEach { (lv, teks) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box5 { Dot(lv) }
                Text(teks, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
    Riwayat("Catatan shift terbaru", shift.map { Triple(it.waktu.tgl(), "${nama[it.lansiaId] ?: "Lansia"} · ${it.shift}: ${it.isi}", Level.NORMAL) })
}

@Composable
private fun Box5(content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 5.dp)) { content() }
}

// ---------- Periksa: 4 fungsi ----------

@Composable
fun PeriksaScreen(vm: AppViewModel, l: Lansia?) {
    if (l == null) { Kosong("Tambahkan lansia dulu di tab Lansia."); return }
    var sub by rememberSaveable { mutableIntStateOf(0) }
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("TTV", "Aktivitas", "Tren", "Porsi").forEachIndexed { i, t ->
            FilterChip(selected = sub == i, onClick = { sub = i }, label = { Text(t) })
        }
    }
    when (sub) {
        0 -> TtvScreen(vm, l)
        1 -> AktivitasScreen(vm, l)
        2 -> TrenScreen(vm)
        else -> PorsiScreen(vm, l)
    }
}

@Composable
fun TtvScreen(vm: AppViewModel, l: Lansia) {
    val logs by vm.ttvLogs.collectAsState()
    var sis by remember(l.id) { mutableStateOf("") }
    var gula by remember(l.id) { mutableStateOf("") }
    var suhu by remember(l.id) { mutableStateOf("") }
    var tidur by remember(l.id) { mutableStateOf("") }
    var err by remember(l.id) { mutableStateOf<String?>(null) }
    var hasil by remember(l.id) { mutableStateOf<HasilTtv?>(null) }

    Panel("Evaluasi TTV", "Masukkan tanda vital hari ini") {
        logs.firstOrNull()?.let {
            Text(
                "Data sebelumnya: ${it.sistolik} mmHg · ${it.gula} mg/dL · ${it.suhu} °C · tidur ${it.jamTidur} jam",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Field("Tekanan darah (sistolik)", "mmHg", sis) { sis = it }
        Field("Gula darah", "mg/dL", gula) { gula = it }
        Field("Suhu tubuh", "°C", suhu) { suhu = it }
        Field("Jam tidur", "jam", tidur) { tidur = it }
        Galat(err)
        Tombol("Evaluasi") {
            val a = sis.angka(); val b = gula.angka(); val c = suhu.angka(); val d = tidur.angka()
            if (a == null || b == null || c == null || d == null) {
                err = "Isi semua kolom dengan angka yang valid."
            } else {
                err = null
                hasil = vm.simpanTtv(l, Ttv(a.toInt(), b.toInt(), c, d))
                sis = ""; gula = ""; suhu = ""; tidur = ""
            }
        }
    }
    AnimatedVisibility(hasil != null) {
        hasil?.let { h ->
            StatusBanner(
                h.level,
                when (h.level) { Level.NORMAL -> "Status: Normal (Aman)"; Level.WASPADA -> "Status: Waspada"; Level.KRITIS -> "Status: Kritis" },
                when (h.level) {
                    Level.NORMAL -> "Semua tanda vital dalam batas aman."
                    Level.WASPADA -> "Perlu pantauan perawat. Poin bahaya: ${h.poin}."
                    Level.KRITIS -> "Segera hubungi tenaga medis. Poin bahaya: ${h.poin}."
                },
                h.alasan
            )
        }
    }
    Riwayat("Riwayat TTV", logs.take(5).map {
        Triple(it.waktu.tgl(), "${it.sistolik} mmHg · ${it.gula} mg/dL · ${it.suhu} °C · tidur ${it.jamTidur} jam", Level.valueOf(it.level))
    })
}

@Composable
fun AktivitasScreen(vm: AppViewModel, l: Lansia) {
    val logs by vm.aktivitasLogs.collectAsState()
    var jalan by remember(l.id) { mutableStateOf("") }
    var senam by remember(l.id) { mutableStateOf("") }
    var istirahat by remember(l.id) { mutableStateOf("") }
    var err by remember(l.id) { mutableStateOf<String?>(null) }
    var hasil by remember(l.id) { mutableStateOf<HasilAktivitas?>(null) }

    Panel("Deviasi aktivitas", "Standar gerak lansia: minimal 30 menit per hari") {
        logs.firstOrNull()?.let {
            Text(
                "Pencatatan terakhir: gerak ${it.jalan + it.senam} menit, istirahat ${it.istirahat} jam",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Field("Durasi jalan kaki hari ini", "menit", jalan) { jalan = it }
        Field("Durasi senam lansia hari ini", "menit", senam) { senam = it }
        Field("Durasi istirahat hari ini", "jam", istirahat) { istirahat = it }
        Galat(err)
        Tombol("Cek Deviasi") {
            val a = jalan.angka(); val b = senam.angka(); val c = istirahat.angka()
            if (a == null || b == null || c == null) {
                err = "Isi semua kolom dengan angka yang valid."
            } else {
                err = null
                hasil = vm.simpanAktivitas(l, Aktivitas(a.toInt(), b.toInt(), c))
                jalan = ""; senam = ""; istirahat = ""
            }
        }
    }
    AnimatedVisibility(hasil != null) {
        hasil?.let { StatusBanner(it.level, it.judul, it.pesan) }
    }
    Riwayat("Riwayat aktivitas", logs.take(5).map {
        Triple(it.waktu.tgl(), "Gerak ${it.jalan + it.senam} menit · istirahat ${it.istirahat} jam · ${it.judul}", Level.valueOf(it.level))
    })
}

@Composable
fun TrenScreen(vm: AppViewModel) {
    val logs by vm.ttvLogs.collectAsState()
    var hitung by remember(logs.firstOrNull()?.id) { mutableStateOf(false) }
    if (logs.size < 7) {
        Kosong("Tren butuh 7 data TTV. Saat ini baru ada ${logs.size}. Lengkapi lewat tab TTV.")
        return
    }
    val data = logs.take(7).reversed().map { it.sistolik }
    Panel("Tren mingguan", "Tekanan darah sistolik 7 data terakhir (mmHg)") {
        LineChart(data, Modifier.fillMaxWidth().height(190.dp))
        Tombol("Kalkulasi Tren") { hitung = true }
    }
    AnimatedVisibility(hitung) {
        val t = hitungTren(data)
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile("Rata-rata", t.rataRata.f1(), Modifier.weight(1f))
                Tile("Hari 1-3", t.awal.f1(), Modifier.weight(1f))
                Tile("Hari 4-7", t.akhir.f1(), Modifier.weight(1f))
            }
            StatusBanner(
                if (t.membaik) Level.NORMAL else Level.KRITIS,
                if (t.membaik) "Tren Kemajuan Kesehatan: MEMBAIK" else "Tren Kemajuan Kesehatan: MENURUN",
                if (t.membaik) "Rata-rata 4 hari terakhir lebih rendah dari 3 hari awal." else "Perlu evaluasi medis."
            )
        }
    }
}

@Composable
fun PorsiScreen(vm: AppViewModel, l: Lansia) {
    val logs by vm.porsiLogs.collectAsState()
    var sis by remember(l.id) { mutableStateOf("") }
    var suhu by remember(l.id) { mutableStateOf("") }
    var nyenyak by remember(l.id) { mutableStateOf(true) }
    var err by remember(l.id) { mutableStateOf<String?>(null) }
    var hasil by remember(l.id) { mutableStateOf<HasilPorsi?>(null) }

    Panel("Porsi olahraga", "Kondisi pagi ini sebelum beraktivitas") {
        Field("Tekanan darah pagi ini", "mmHg", sis) { sis = it }
        Field("Suhu tubuh pagi ini", "°C", suhu) { suhu = it }
        Text("Kualitas tidur semalam", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(true to "Nyenyak", false to "Kurang tidur").forEach { (v, teks) ->
                FilterChip(selected = nyenyak == v, onClick = { nyenyak = v }, label = { Text(teks) })
            }
        }
        Galat(err)
        Tombol("Hitung Porsi") {
            val a = sis.angka(); val b = suhu.angka()
            if (a == null || b == null) {
                err = "Isi tekanan darah dan suhu tubuh dengan angka yang valid."
            } else {
                err = null
                hasil = vm.simpanPorsi(l, a.toInt(), b, nyenyak)
                sis = ""; suhu = ""
            }
        }
    }
    AnimatedVisibility(hasil != null) {
        hasil?.let { h ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatusBanner(h.level, h.kondisi, "Fokus: ${h.fokus}")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Tile("Jalan kaki", "${h.jalan} menit", Modifier.weight(1f))
                    Tile("Senam", "${h.senam} menit", Modifier.weight(1f))
                }
            }
        }
    }
    Riwayat("Riwayat porsi olahraga", logs.take(5).map {
        Triple(it.waktu.tgl(), "${it.kondisi} · jalan ${it.jalan} menit, senam ${it.senam} menit", Level.valueOf(it.level))
    })
}

// ---------- Obat ----------

@Composable
fun ObatScreen(vm: AppViewModel, l: Lansia?) {
    if (l == null) { Kosong("Tambahkan lansia dulu di tab Lansia."); return }
    val obat by vm.obatLansia.collectAsState()
    val log by vm.obatLogs.collectAsState()
    var pilihId by remember(l.id) { mutableStateOf<Long?>(null) }
    var foto by remember(l.id) { mutableStateOf<String?>(null) }
    var err by remember(l.id) { mutableStateOf<String?>(null) }
    var nama by remember(l.id) { mutableStateOf("") }
    var dosis by remember(l.id) { mutableStateOf("") }
    var jam by remember(l.id) { mutableStateOf("") }
    var err2 by remember(l.id) { mutableStateOf<String?>(null) }

    Panel("Pemberian obat", "Catat obat yang sudah diberikan hari ini") {
        if (obat.isEmpty()) {
            Text("Belum ada jadwal obat. Tambahkan di bawah.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                obat.forEach { o ->
                    FilterChip(selected = pilihId == o.id, onClick = { pilihId = o.id }, label = { Text("${o.nama} ${o.jam}") })
                }
            }
            FotoPicker(foto) { foto = it }
            Galat(err)
            Tombol("Catat sudah diberikan") {
                val o = obat.firstOrNull { it.id == pilihId }
                if (o == null) {
                    err = "Pilih obat dulu."
                } else {
                    err = null
                    vm.catatObat(l, o, foto)
                    foto = null; pilihId = null
                }
            }
        }
    }
    if (obat.isNotEmpty()) {
        Panel("Jadwal obat", "Pengingat harian aktif untuk jadwal ini") {
            obat.forEach { o ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(o.nama, fontWeight = FontWeight.Bold)
                        Text("${o.dosis} · tiap hari ${o.jam}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { vm.hapusObat(o) }) { Text("Hapus") }
                }
            }
        }
    }
    Panel("Tambah jadwal obat", "Notifikasi pengingat muncul setiap hari pada jam yang diisi") {
        TeksField("Nama obat", nama, { nama = it })
        TeksField("Dosis (contoh: 5 mg)", dosis, { dosis = it })
        TeksField("Jam minum (HH:mm, contoh 08:00)", jam, { jam = it })
        Galat(err2)
        Tombol("Simpan jadwal") {
            val jamOk = Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(jam.trim())
            if (nama.isBlank() || dosis.isBlank() || !jamOk) {
                err2 = "Isi nama, dosis, dan jam dengan format HH:mm (contoh 08:00)."
            } else {
                err2 = null
                vm.tambahObat(l, nama.trim(), dosis.trim(), jam.trim())
                nama = ""; dosis = ""; jam = ""
            }
        }
    }
    Riwayat("Riwayat pemberian obat", log.take(8).map {
        Triple(it.waktu.tgl(), it.namaObat + if (it.fotoPath != null) " · ada foto bukti" else "", Level.NORMAL)
    })
}

// ---------- Catatan shift ----------

@Composable
fun ShiftScreen(vm: AppViewModel, l: Lansia?) {
    if (l == null) { Kosong("Tambahkan lansia dulu di tab Lansia."); return }
    val notes by vm.shiftNotes.collectAsState()
    var shift by remember(l.id) { mutableStateOf("Pagi") }
    var isi by remember(l.id) { mutableStateOf("") }
    var foto by remember(l.id) { mutableStateOf<String?>(null) }
    var err by remember(l.id) { mutableStateOf<String?>(null) }

    Panel("Catatan serah terima shift", "Tulis kondisi lansia untuk perawat shift berikutnya") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Pagi", "Siang", "Malam").forEach { s ->
                FilterChip(selected = shift == s, onClick = { shift = s }, label = { Text(s) })
            }
        }
        TeksField("Catatan", isi, { isi = it }, baris = 3)
        FotoPicker(foto) { foto = it }
        Galat(err)
        Tombol("Simpan catatan") {
            if (isi.isBlank()) {
                err = "Catatan tidak boleh kosong."
            } else {
                err = null
                vm.tambahShift(l, shift, isi.trim(), foto)
                isi = ""; foto = null
            }
        }
    }
    notes.take(10).forEach { KartuCatatan("Shift ${it.shift}", it.waktu.tgl(), it.isi, it.fotoPath) }
}

// ---------- CRUD lansia ----------

@Composable
fun LansiaScreen(vm: AppViewModel, onDetail: (Long) -> Unit, onFavorit: () -> Unit) {
    val tampil by vm.daftarTersaring.collectAsState()
    val query by vm.query.collectAsState()
    val urut by vm.sortAscending.collectAsState()
    var edit by remember { mutableStateOf<Lansia?>(null) }
    var hapus by remember { mutableStateOf<Lansia?>(null) }
    var nama by remember { mutableStateOf("") }
    var umur by remember { mutableStateOf("") }
    var kamar by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var info by remember { mutableStateOf<String?>(null) }

    fun reset() { edit = null; nama = ""; umur = ""; kamar = ""; err = null; info = null }

    Panel(if (edit == null) "Tambah lansia" else "Ubah data lansia") {
        TeksField("Nama lengkap", nama, { nama = it })
        Field("Umur", "tahun", umur) { umur = it }
        TeksField("Kamar / ruangan (opsional)", kamar, { kamar = it })
        Galat(err)
        info?.let { Text(it, color = Ok, fontWeight = FontWeight.SemiBold) }
        Tombol(if (edit == null) "Tambah" else "Simpan perubahan") {
            val u = umur.angka()?.toInt()
            if (nama.isBlank() || u == null || u <= 0) {
                err = "Isi nama dan umur dengan benar."
            } else {
                val e = edit
                if (e == null) {
                    val baru = nama.trim()
                    if (vm.tambahLansia(baru, u, kamar.trim())) {
                        reset()
                        info = "$baru berhasil ditambahkan dan sudah bisa diperiksa."
                    } else {
                        err = "Nama lansia sudah terdaftar."
                    }
                } else {
                    vm.ubahLansia(e.copy(nama = nama.trim(), umur = u, kamar = kamar.trim()))
                    reset()
                }
            }
        }
        if (edit != null) TextButton(onClick = { reset() }) { Text("Batal") }
    }
    Panel("Daftar lansia (${tampil.size})") {
        TeksField("Cari nama lansia", query, { vm.onQueryChanged(it) })
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { vm.toggleUrutan() }) { Text(if (urut) "Urutan: A-Z" else "Urutan: Z-A") }
            TextButton(onClick = onFavorit) { Text("Lihat favorit") }
        }
        if (tampil.isEmpty()) Text("Tidak ada lansia yang cocok.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        tampil.forEachIndexed { i, p ->
            if (i > 0) HorizontalDivider()
            Column {
                Text(p.nama, fontWeight = FontWeight.Bold)
                Text(
                    "${p.umur} tahun" + if (p.kamar.isNotEmpty()) " · Kamar ${p.kamar}" else "",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row {
                    TextButton(onClick = { onDetail(p.id) }) { Text("Detail") }
                    TextButton(onClick = { edit = p; nama = p.nama; umur = p.umur.toString(); kamar = p.kamar }) { Text("Ubah") }
                    TextButton(onClick = { hapus = p }) { Text("Hapus") }
                }
            }
        }
    }
    hapus?.let { t ->
        AlertDialog(
            onDismissRequest = { hapus = null },
            title = { Text("Hapus ${t.nama}?") },
            text = { Text("Semua riwayat pemeriksaan, jadwal obat, dan catatan lansia ini ikut terhapus.") },
            confirmButton = { TextButton(onClick = { vm.hapusLansia(t); hapus = null }) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { hapus = null }) { Text("Batal") } }
        )
    }
}
