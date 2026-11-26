package com.rawatsayang.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.userPreferencesDataStore by preferencesDataStore(name = "user_preferences")

/** Preferences DataStore: query pencarian terakhir dan urutan daftar lansia. */
class UserPreferencesRepository(private val context: Context) {
    private val kunciQuery = stringPreferencesKey("last_search_query")
    private val kunciUrut = booleanPreferencesKey("sort_ascending")

    val lastSearchQuery: Flow<String> = context.userPreferencesDataStore.data.map { it[kunciQuery] ?: "" }
    val sortAscending: Flow<Boolean> = context.userPreferencesDataStore.data.map { it[kunciUrut] ?: true }

    suspend fun saveLastSearchQuery(value: String) {
        context.userPreferencesDataStore.edit { it[kunciQuery] = value }
    }

    suspend fun saveSortAscending(value: Boolean) {
        context.userPreferencesDataStore.edit { it[kunciUrut] = value }
    }
}

/** Repository Room: ViewModel tidak memanggil DAO secara langsung. */
class RawatRepository(private val dao: RawatDao) {
    fun semuaLansia() = dao.semuaLansia()
    suspend fun lansiaSekali() = dao.lansiaSekali()
    suspend fun jumlahLansia() = dao.jumlahLansia()
    suspend fun tambahLansia(l: Lansia) = dao.tambahLansia(l)
    suspend fun ubahLansia(l: Lansia) = dao.ubahLansia(l)
    suspend fun hapusLansia(l: Lansia) = dao.hapusLansia(l)

    fun ttvLogs(id: Long) = dao.ttvLogs(id)
    fun ttvTerbaru() = dao.ttvTerbaru()
    suspend fun tambahTtv(t: TtvLog) = dao.tambahTtv(t)

    fun aktivitasLogs(id: Long) = dao.aktivitasLogs(id)
    suspend fun tambahAktivitas(a: AktivitasLog) = dao.tambahAktivitas(a)

    fun porsiLogs(id: Long) = dao.porsiLogs(id)
    suspend fun tambahPorsi(p: PorsiLog) = dao.tambahPorsi(p)

    fun obatLansia(id: Long) = dao.obatLansia(id)
    suspend fun obatLansiaSekali(id: Long) = dao.obatLansiaSekali(id)
    fun semuaObat() = dao.semuaObat()
    suspend fun obatSekali() = dao.obatSekali()
    suspend fun tambahObat(o: Obat) = dao.tambahObat(o)
    suspend fun hapusObat(o: Obat) = dao.hapusObat(o)

    fun obatLogs(id: Long) = dao.obatLogs(id)
    fun obatLogSejak(sejak: Long) = dao.obatLogSejak(sejak)
    suspend fun tambahObatLog(o: ObatLog) = dao.tambahObatLog(o)

    fun shiftNotes(id: Long) = dao.shiftNotes(id)
    fun shiftTerbaru() = dao.shiftTerbaru()
    suspend fun tambahShift(s: ShiftNote) = dao.tambahShift(s)

    suspend fun akunByEmail(email: String) = dao.akunByEmail(email)
    suspend fun tambahAkun(a: Akun) = dao.tambahAkun(a)
    suspend fun ubahAkun(a: Akun) = dao.ubahAkun(a)

    fun observeFavorites() = dao.observeFavorites()
    fun observeIsFavorite(id: Long) = dao.observeIsFavorite(id)
    suspend fun tambahFavorit(id: Long) = dao.insertFavorit(FavoritLansia(id))
    suspend fun hapusFavorit(id: Long) = dao.deleteFavoritById(id)
}
