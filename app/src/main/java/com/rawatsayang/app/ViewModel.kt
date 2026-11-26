package com.rawatsayang.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(FlowPreview::class)
class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RawatRepository(RawatDb.get(app).dao())
    private val prefs = UserPreferencesRepository(app)
    private val ctx = app.applicationContext

    private fun <T> Flow<T>.state(awal: T): StateFlow<T> = stateIn(viewModelScope, SharingStarted.Eagerly, awal)

    val daftar = repo.semuaLansia().state(emptyList())
    private val terpilihId = MutableStateFlow(0L)
    val terpilih: StateFlow<Lansia?> =
        combine(daftar, terpilihId) { l, id -> l.firstOrNull { it.id == id } ?: l.firstOrNull() }.state(null)

    fun pilih(id: Long) { terpilihId.value = id }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> perLansia(f: (Long) -> Flow<List<T>>): StateFlow<List<T>> =
        terpilih.flatMapLatest { l -> if (l == null) flowOf(emptyList()) else f(l.id) }.state(emptyList())

    val ttvLogs = perLansia { repo.ttvLogs(it) }
    val aktivitasLogs = perLansia { repo.aktivitasLogs(it) }
    val porsiLogs = perLansia { repo.porsiLogs(it) }
    val obatLansia = perLansia { repo.obatLansia(it) }
    val obatLogs = perLansia { repo.obatLogs(it) }
    val shiftNotes = perLansia { repo.shiftNotes(it) }

    val ttvTerbaru = repo.ttvTerbaru().state(emptyList())
    val semuaObat = repo.semuaObat().state(emptyList())
    val shiftTerbaru = repo.shiftTerbaru().state(emptyList())
    val obatHariIni = repo.obatLogSejak(awalHari()).state(emptyList())

    // ----- DataStore: pencarian terakhir & urutan daftar lansia -----
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    val sortAscending = prefs.sortAscending.state(true)
    val daftarTersaring = combine(daftar, query, sortAscending) { l, q, asc ->
        val f = l.filter { it.nama.contains(q.trim(), ignoreCase = true) }
        if (asc) f.sortedBy { it.nama.lowercase() } else f.sortedByDescending { it.nama.lowercase() }
    }.state(emptyList())

    init {
        viewModelScope.launch { _query.value = prefs.lastSearchQuery.first() }
        viewModelScope.launch {
            query.debounce(500).distinctUntilChanged().collectLatest { prefs.saveLastSearchQuery(it) }
        }
    }

    fun onQueryChanged(v: String) { _query.value = v }
    fun toggleUrutan() { viewModelScope.launch { prefs.saveSortAscending(!sortAscending.value) } }

    // ----- Favorit (Room + Flow) -----
    val favorit = repo.observeFavorites().state(emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val isFavorit = terpilih.flatMapLatest { l -> if (l == null) flowOf(false) else repo.observeIsFavorite(l.id) }.state(false)

    fun toggleFavorit(l: Lansia) {
        viewModelScope.launch { if (isFavorit.value) repo.hapusFavorit(l.id) else repo.tambahFavorit(l.id) }
    }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            seed()
            val nama = repo.lansiaSekali().associate { it.id to it.nama }
            repo.obatSekali().forEach { Pengingat.jadwalkan(ctx, it.id, it.nama, nama[it.lansiaId] ?: "", it.dosis, it.jam) }
        }
    }

    private fun awalHari(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private suspend fun seed() {
        if (repo.jumlahLansia() > 0) return
        val now = System.currentTimeMillis()
        val hari = 86_400_000L
        val data = listOf(
            Triple("Ibu Sari Wulandari", 72, listOf(138, 142, 140, 136, 132, 130, 128)),
            Triple("Bapak Hendra Gunawan", 78, listOf(128, 130, 134, 138, 142, 145, 148)),
            Triple("Ibu Lastri Handayani", 69, listOf(126, 124, 125, 122, 121, 120, 121))
        )
        data.forEachIndexed { i, (nama, umur, sis) ->
            val id = repo.tambahLansia(Lansia(nama = nama, umur = umur, kamar = "A${i + 1}"))
            var prev: Ttv? = null
            sis.forEachIndexed { j, s ->
                val t = Ttv(s, 110 + i * 35 + j * 2, 36.6 + i * 0.2, 6.5 - i * 0.5)
                val h = evaluasiTtv(t, prev)
                prev = t
                repo.tambahTtv(
                    TtvLog(
                        lansiaId = id, waktu = now - (6 - j) * hari, sistolik = t.sistolik, gula = t.gula,
                        suhu = t.suhu, jamTidur = t.jamTidur, level = h.level.name, poin = h.poin
                    )
                )
            }
            if (i == 0) repo.tambahObat(Obat(lansiaId = id, nama = "Amlodipine", dosis = "5 mg", jam = "08:00"))
        }
    }

    // ----- CRUD lansia -----
    /** Mengembalikan false jika nama sudah terdaftar. Lansia baru otomatis menjadi lansia terpilih. */
    fun tambahLansia(nama: String, umur: Int, kamar: String): Boolean {
        if (daftar.value.any { it.nama.equals(nama, ignoreCase = true) }) return false
        viewModelScope.launch {
            val id = repo.tambahLansia(Lansia(nama = nama, umur = umur, kamar = kamar))
            pilih(id)
        }
        return true
    }
    fun ubahLansia(l: Lansia) { viewModelScope.launch { repo.ubahLansia(l) } }
    fun hapusLansia(l: Lansia) {
        viewModelScope.launch {
            repo.obatLansiaSekali(l.id).forEach { Pengingat.batalkan(ctx, it.id) }
            repo.hapusLansia(l)
        }
    }

    // ----- Fungsi pemeriksaan (hasil dikembalikan untuk ditampilkan, lalu disimpan sebagai riwayat) -----
    fun simpanTtv(l: Lansia, t: Ttv): HasilTtv {
        val prev = ttvLogs.value.firstOrNull()?.let { Ttv(it.sistolik, it.gula, it.suhu, it.jamTidur) }
        val h = evaluasiTtv(t, prev)
        viewModelScope.launch {
            repo.tambahTtv(
                TtvLog(
                    lansiaId = l.id, waktu = System.currentTimeMillis(), sistolik = t.sistolik, gula = t.gula,
                    suhu = t.suhu, jamTidur = t.jamTidur, level = h.level.name, poin = h.poin
                )
            )
        }
        return h
    }

    fun simpanAktivitas(l: Lansia, a: Aktivitas): HasilAktivitas {
        val prev = aktivitasLogs.value.firstOrNull()?.let { Aktivitas(it.jalan, it.senam, it.istirahat) }
        val h = evaluasiAktivitas(a, prev)
        viewModelScope.launch {
            repo.tambahAktivitas(
                AktivitasLog(
                    lansiaId = l.id, waktu = System.currentTimeMillis(), jalan = a.jalan, senam = a.senam,
                    istirahat = a.istirahat, level = h.level.name, judul = h.judul, pesan = h.pesan
                )
            )
        }
        return h
    }

    fun simpanPorsi(l: Lansia, sistolik: Int, suhu: Double, nyenyak: Boolean): HasilPorsi {
        val h = hitungPorsi(sistolik, suhu, nyenyak)
        viewModelScope.launch {
            repo.tambahPorsi(
                PorsiLog(
                    lansiaId = l.id, waktu = System.currentTimeMillis(), sistolik = sistolik, suhu = suhu, nyenyak = nyenyak,
                    kondisi = h.kondisi, fokus = h.fokus, jalan = h.jalan, senam = h.senam, level = h.level.name
                )
            )
        }
        return h
    }

    // ----- Obat -----
    fun tambahObat(l: Lansia, nama: String, dosis: String, jam: String) {
        viewModelScope.launch {
            val id = repo.tambahObat(Obat(lansiaId = l.id, nama = nama, dosis = dosis, jam = jam))
            Pengingat.jadwalkan(ctx, id, nama, l.nama, dosis, jam)
        }
    }

    fun hapusObat(o: Obat) {
        viewModelScope.launch {
            Pengingat.batalkan(ctx, o.id)
            repo.hapusObat(o)
        }
    }

    fun catatObat(l: Lansia, o: Obat, foto: String?) {
        viewModelScope.launch {
            repo.tambahObatLog(ObatLog(obatId = o.id, lansiaId = l.id, namaObat = o.nama, waktu = System.currentTimeMillis(), fotoPath = foto))
        }
    }

    // ----- Catatan shift -----
    fun tambahShift(l: Lansia, shift: String, isi: String, foto: String?) {
        viewModelScope.launch {
            repo.tambahShift(ShiftNote(lansiaId = l.id, waktu = System.currentTimeMillis(), shift = shift, isi = isi, fotoPath = foto))
        }
    }
}
