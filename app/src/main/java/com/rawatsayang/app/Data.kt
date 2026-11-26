package com.rawatsayang.app

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lansia")
data class Lansia(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nama: String, val umur: Int, val kamar: String = ""
)

@Entity(tableName = "ttv_log", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lansiaId")])
data class TtvLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lansiaId: Long, val waktu: Long, val sistolik: Int, val gula: Int,
    val suhu: Double, val jamTidur: Double, val level: String, val poin: Int
)

@Entity(tableName = "aktivitas_log", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lansiaId")])
data class AktivitasLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lansiaId: Long, val waktu: Long, val jalan: Int, val senam: Int, val istirahat: Double,
    val level: String, val judul: String, val pesan: String
)

@Entity(tableName = "porsi_log", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lansiaId")])
data class PorsiLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lansiaId: Long, val waktu: Long, val sistolik: Int, val suhu: Double, val nyenyak: Boolean,
    val kondisi: String, val fokus: String, val jalan: Int, val senam: Int, val level: String
)

@Entity(tableName = "obat", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lansiaId")])
data class Obat(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lansiaId: Long, val nama: String, val dosis: String, val jam: String
)

@Entity(tableName = "obat_log", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lansiaId")])
data class ObatLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val obatId: Long, val lansiaId: Long, val namaObat: String, val waktu: Long, val fotoPath: String?
)

@Entity(tableName = "shift_note", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)], indices = [Index("lansiaId")])
data class ShiftNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val lansiaId: Long, val waktu: Long, val shift: String, val isi: String, val fotoPath: String?
)

@Entity(tableName = "favorit_lansia", foreignKeys = [ForeignKey(entity = Lansia::class, parentColumns = ["id"], childColumns = ["lansiaId"], onDelete = ForeignKey.CASCADE)])
data class FavoritLansia(@PrimaryKey val lansiaId: Long)

@Entity(tableName = "akun", indices = [Index(value = ["email"], unique = true)])
data class Akun(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String, val nama: String, val salt: String, val hash: String,
    val peran: String = "", val lansiaId: Long? = null
)

@Dao
interface RawatDao {
    @Query("SELECT * FROM akun WHERE email = :email LIMIT 1") suspend fun akunByEmail(email: String): Akun?
    @Insert suspend fun tambahAkun(a: Akun): Long
    @Update suspend fun ubahAkun(a: Akun)

    @Query("SELECT * FROM lansia ORDER BY nama") fun semuaLansia(): Flow<List<Lansia>>
    @Query("SELECT * FROM lansia") suspend fun lansiaSekali(): List<Lansia>
    @Query("SELECT COUNT(*) FROM lansia") suspend fun jumlahLansia(): Int
    @Insert suspend fun tambahLansia(l: Lansia): Long
    @Update suspend fun ubahLansia(l: Lansia)
    @Delete suspend fun hapusLansia(l: Lansia)

    @Query("SELECT * FROM ttv_log WHERE lansiaId = :id ORDER BY waktu DESC LIMIT 30") fun ttvLogs(id: Long): Flow<List<TtvLog>>
    @Query("SELECT * FROM ttv_log WHERE id IN (SELECT MAX(id) FROM ttv_log GROUP BY lansiaId)") fun ttvTerbaru(): Flow<List<TtvLog>>
    @Insert suspend fun tambahTtv(t: TtvLog)

    @Query("SELECT * FROM aktivitas_log WHERE lansiaId = :id ORDER BY waktu DESC LIMIT 30") fun aktivitasLogs(id: Long): Flow<List<AktivitasLog>>
    @Insert suspend fun tambahAktivitas(a: AktivitasLog)

    @Query("SELECT * FROM porsi_log WHERE lansiaId = :id ORDER BY waktu DESC LIMIT 30") fun porsiLogs(id: Long): Flow<List<PorsiLog>>
    @Insert suspend fun tambahPorsi(p: PorsiLog)

    @Query("SELECT * FROM obat WHERE lansiaId = :id ORDER BY jam") fun obatLansia(id: Long): Flow<List<Obat>>
    @Query("SELECT * FROM obat WHERE lansiaId = :id") suspend fun obatLansiaSekali(id: Long): List<Obat>
    @Query("SELECT * FROM obat") fun semuaObat(): Flow<List<Obat>>
    @Query("SELECT * FROM obat") suspend fun obatSekali(): List<Obat>
    @Insert suspend fun tambahObat(o: Obat): Long
    @Delete suspend fun hapusObat(o: Obat)

    @Query("SELECT * FROM obat_log WHERE lansiaId = :id ORDER BY waktu DESC LIMIT 50") fun obatLogs(id: Long): Flow<List<ObatLog>>
    @Query("SELECT * FROM obat_log WHERE waktu >= :sejak") fun obatLogSejak(sejak: Long): Flow<List<ObatLog>>
    @Insert suspend fun tambahObatLog(o: ObatLog)

    @Query("SELECT * FROM shift_note WHERE lansiaId = :id ORDER BY waktu DESC LIMIT 50") fun shiftNotes(id: Long): Flow<List<ShiftNote>>
    @Query("SELECT * FROM shift_note ORDER BY waktu DESC LIMIT 5") fun shiftTerbaru(): Flow<List<ShiftNote>>
    @Insert suspend fun tambahShift(s: ShiftNote)

    @Query("SELECT l.* FROM lansia l INNER JOIN favorit_lansia f ON f.lansiaId = l.id ORDER BY l.nama ASC") fun observeFavorites(): Flow<List<Lansia>>
    @Query("SELECT EXISTS(SELECT 1 FROM favorit_lansia WHERE lansiaId = :id)") fun observeIsFavorite(id: Long): Flow<Boolean>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertFavorit(f: FavoritLansia)
    @Query("DELETE FROM favorit_lansia WHERE lansiaId = :id") suspend fun deleteFavoritById(id: Long)
}

private val MIGRASI_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `akun` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `email` TEXT NOT NULL, `nama` TEXT NOT NULL, `salt` TEXT NOT NULL, `hash` TEXT NOT NULL, `peran` TEXT NOT NULL, `lansiaId` INTEGER)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_akun_email` ON `akun` (`email`)")
    }
}

@Database(
    entities = [Lansia::class, TtvLog::class, AktivitasLog::class, PorsiLog::class, Obat::class, ObatLog::class, ShiftNote::class, FavoritLansia::class, Akun::class],
    version = 3, exportSchema = false
)
abstract class RawatDb : RoomDatabase() {
    abstract fun dao(): RawatDao

    companion object {
        @Volatile private var inst: RawatDb? = null
        fun get(ctx: Context): RawatDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, RawatDb::class.java, "rawatsayang.db").addMigrations(MIGRASI_2_3).fallbackToDestructiveMigration().build().also { inst = it }
        }
    }
}
