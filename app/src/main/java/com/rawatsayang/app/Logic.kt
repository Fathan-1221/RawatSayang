package com.rawatsayang.app

enum class Level { NORMAL, WASPADA, KRITIS }

data class Ttv(val sistolik: Int, val gula: Int, val suhu: Double, val jamTidur: Double)
data class Aktivitas(val jalan: Int, val senam: Int, val istirahat: Double) {
    val total: Int get() = jalan + senam
}
data class HasilTtv(val level: Level, val poin: Int, val alasan: List<String>)
data class HasilAktivitas(val level: Level, val judul: String, val pesan: String)
data class HasilTren(val rataRata: Double, val awal: Double, val akhir: Double, val membaik: Boolean)
data class HasilPorsi(val kondisi: String, val fokus: String, val jalan: Int, val senam: Int, val level: Level)

/** Fungsi 1: Evaluasi Status Tanda Vital */
fun evaluasiTtv(now: Ttv, prev: Ttv?): HasilTtv {
    var poin = 0
    val alasan = mutableListOf<String>()
    if (now.sistolik > 140 || now.sistolik < 90) { poin++; alasan += "Tekanan darah ${now.sistolik} mmHg di luar batas 90-140" }
    if (now.gula > 200 || now.gula < 70) { poin++; alasan += "Gula darah ${now.gula} mg/dL di luar batas 70-200" }
    if (now.suhu > 37.5 || now.suhu < 36.0) { poin++; alasan += "Suhu tubuh ${now.suhu} °C di luar batas 36,0-37,5" }
    if (now.jamTidur < 5) { poin++; alasan += "Jam tidur ${now.jamTidur} jam kurang dari 5 jam" }
    if (prev != null) {
        if (now.sistolik - prev.sistolik >= 20) { poin++; alasan += "Tekanan darah naik ${now.sistolik - prev.sistolik} mmHg dari data sebelumnya" }
        if (now.gula - prev.gula >= 50) { poin++; alasan += "Gula darah naik ${now.gula - prev.gula} mg/dL dari data sebelumnya" }
    }
    val level = when {
        poin == 0 -> Level.NORMAL
        poin <= 2 -> Level.WASPADA
        else -> Level.KRITIS
    }
    return HasilTtv(level, poin, alasan)
}

/** Fungsi 2: Deteksi Deviasi Aktivitas Fisik */
fun evaluasiAktivitas(now: Aktivitas, kemarin: Aktivitas?): HasilAktivitas {
    val selisih = maxOf(0, 30 - now.total)
    val penurunan = if (kemarin != null) kemarin.total - now.total else 0
    val saran = if (selisih > 0) "Rekomendasi tambahan gerak: $selisih menit." else "Pantau kondisi lansia dan batasi waktu istirahat."
    return when {
        now.total < 15 || penurunan >= 30 || now.istirahat > 12 ->
            HasilAktivitas(Level.KRITIS, "Peringatan: pola tidak biasa (sangat pasif / kelelahan)", saran)
        now.total < 30 ->
            HasilAktivitas(Level.WASPADA, "Peringatan: lansia kurang aktif", saran)
        else ->
            HasilAktivitas(Level.NORMAL, "Pola perilaku normal dan aktif", "Total gerak ${now.total} menit, sudah memenuhi standar 30 menit.")
    }
}

/** Fungsi 3: Kompilasi Rata-Rata Kesehatan Mingguan (data urut dari terlama ke terbaru) */
fun hitungTren(riwayat: List<Int>): HasilTren {
    val d = riwayat.takeLast(7)
    var total = 0
    for (v in d) total += v
    val awal = d.take(3).average()
    val akhir = d.drop(3).average()
    return HasilTren(total.toDouble() / d.size, awal, akhir, akhir < awal)
}

/** Fungsi 4: Penentu Porsi Aktivitas Harian Aman */
fun hitungPorsi(sistolik: Int, suhu: Double, nyenyak: Boolean): HasilPorsi = when {
    sistolik > 150 || suhu > 37.5 -> HasilPorsi("Tidak layak olahraga", "Istirahat", 0, 0, Level.KRITIS)
    sistolik > 130 || !nyenyak -> HasilPorsi("Setengah bugar", "Beraktivitas ringan", 10, 10, Level.WASPADA)
    else -> HasilPorsi("Bugar", "Beraktivitas normal", 20, 30, Level.NORMAL)
}
