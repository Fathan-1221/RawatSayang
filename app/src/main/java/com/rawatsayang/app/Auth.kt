package com.rawatsayang.app

import android.app.Application
import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

const val PERAN_PERAWAT = "perawat"
const val PERAN_KELUARGA = "keluarga"

// ---------- ViewModel akun ----------

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = RawatRepository(RawatDb.get(app).dao())
    private val _akun = MutableStateFlow<Akun?>(null)
    val akun: StateFlow<Akun?> = _akun.asStateFlow()

    private fun hex(b: ByteArray) = b.joinToString("") { "%02x".format(it) }

    private fun buatSalt(): String {
        val b = ByteArray(16)
        SecureRandom().nextBytes(b)
        return hex(b)
    }

    private fun hash(sandi: String, salt: String): String {
        val spec = PBEKeySpec(sandi.toCharArray(), salt.toByteArray(), 10_000, 256)
        return hex(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded)
    }

    /** hasil(null) = berhasil, selain itu pesan galat. */
    fun daftar(email: String, nama: String, sandi: String, hasil: (String?) -> Unit) {
        viewModelScope.launch {
            val e = email.trim().lowercase()
            if (repo.akunByEmail(e) != null) {
                hasil("Email ini sudah terdaftar. Silakan masuk.")
                return@launch
            }
            val salt = buatSalt()
            val h = withContext(Dispatchers.Default) { hash(sandi, salt) }
            val baru = Akun(email = e, nama = nama.trim(), salt = salt, hash = h)
            try {
                val id = repo.tambahAkun(baru)
                _akun.value = baru.copy(id = id)
                hasil(null)
            } catch (ex: Exception) {
                hasil("Gagal membuat akun. Coba lagi.")
            }
        }
    }

    fun masuk(email: String, sandi: String, hasil: (String?) -> Unit) {
        viewModelScope.launch {
            val a = repo.akunByEmail(email.trim().lowercase())
            val cocok = a != null && withContext(Dispatchers.Default) {
                MessageDigest.isEqual(hash(sandi, a.salt).toByteArray(), a.hash.toByteArray())
            }
            if (a != null && cocok) {
                _akun.value = a
                hasil(null)
            } else {
                hasil("Email atau kata sandi salah.")
            }
        }
    }

    fun pilihPeran(peran: String, lansiaId: Long?, selesai: () -> Unit) {
        val a = _akun.value ?: return
        val baru = a.copy(peran = peran, lansiaId = lansiaId ?: a.lansiaId)
        viewModelScope.launch {
            repo.ubahAkun(baru)
            _akun.value = baru
            selesai()
        }
    }

    fun keluar() { _akun.value = null }
}

// ---------- Komponen kecil ----------

@Composable
private fun FieldAkun(
    label: String, hint: String, nilai: String, onChange: (String) -> Unit,
    tipe: KeyboardType = KeyboardType.Text, sandi: Boolean = false
) {
    var lihat by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row {
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            Text(" *", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Bad)
        }
        OutlinedTextField(
            value = nilai, onValueChange = onChange,
            placeholder = { Text(hint) },
            singleLine = true, shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(keyboardType = tipe),
            visualTransformation = if (sandi && !lihat) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (sandi) {
                { TextButton(onClick = { lihat = !lihat }) { Text(if (lihat) "Sembunyi" else "Lihat") } }
            } else null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun PemisahAtau() {
    val garis = MaterialTheme.colorScheme.outline
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.weight(1f).height(1.dp).background(garis))
        Text("Atau", Modifier.padding(horizontal = 14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.weight(1f).height(1.dp).background(garis))
    }
}

@Composable
private fun TombolGoogle(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.width(10.dp))
        Text("Masuk dengan Google", fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TautanAkun(pertanyaan: String, aksi: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text("$pertanyaan ", style = MaterialTheme.typography.bodyMedium)
        Text(
            aksi, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary, modifier = Modifier.clickable { onClick() }
        )
    }
}

private const val INFO_GOOGLE = "Masuk dengan Google belum diaktifkan di aplikasi ini. Silakan daftar dengan email."

// ---------- Welcome ----------

@Composable
fun WelcomeScreen(onDaftar: () -> Unit, onMasuk: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(GreenDark, Green)))) {
        Column(
            Modifier.weight(1f).fillMaxWidth().statusBarsPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Favorite, null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("RawatSayang", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Pusat kontrol kesehatan harian lansia", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f))
            Spacer(Modifier.height(24.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { ChipFitur("Tanda vital"); ChipFitur("Pengingat obat") }
                ChipFitur("Bukti perawatan")
            }
        }
        Surface(shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), color = MaterialTheme.colorScheme.background) {
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Tanggap saat darurat, tepat dalam perawatan",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
                )
                Text(
                    "Dapatkan panduan langkah-langkah penanganan kondisi umum hingga darurat, lengkap dengan rekomendasi tindakan",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDaftar, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).height(54.dp)) {
                        Text("Daftar", fontWeight = FontWeight.SemiBold)
                    }
                    Button(onClick = onMasuk, shape = RoundedCornerShape(18.dp), modifier = Modifier.weight(1f).height(54.dp)) {
                        Text("Masuk", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChipFitur(teks: String) {
    Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.16f)) {
        Text(teks, Modifier.padding(horizontal = 14.dp, vertical = 8.dp), style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

// ---------- Daftar ----------

@Composable
fun DaftarScreen(auth: AuthViewModel, onBack: () -> Unit, onMasuk: () -> Unit, onBerhasil: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var nama by remember { mutableStateOf("") }
    var sandi by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var sibuk by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarKembali("Daftar", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Buat Akun Anda", Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                "Mulai langkah pertama untuk memberikan perawatan lansia yang lebih baik bersama RawatSayang.",
                Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            FieldAkun("Email", "Masukkan alamat email Anda", email, { email = it }, KeyboardType.Email)
            FieldAkun("Nama", "Masukkan nama Anda", nama, { nama = it })
            FieldAkun("Kata Sandi", "Masukkan kata sandi Anda", sandi, { sandi = it }, KeyboardType.Password, sandi = true)
            Galat(err)
            Tombol("Daftar") {
                if (sibuk) return@Tombol
                if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                    err = "Format email belum benar."
                } else if (nama.isBlank()) {
                    err = "Nama tidak boleh kosong."
                } else if (sandi.length < 6) {
                    err = "Kata sandi minimal 6 karakter."
                } else {
                    err = null
                    sibuk = true
                    auth.daftar(email, nama, sandi) { galat ->
                        sibuk = false
                        if (galat == null) onBerhasil() else err = galat
                    }
                }
            }
            TautanAkun("Sudah mempunyai akun?", "Masuk", onMasuk)
            PemisahAtau()
            TombolGoogle { err = INFO_GOOGLE }
        }
    }
}

// ---------- Masuk ----------

@Composable
fun MasukScreen(auth: AuthViewModel, onBack: () -> Unit, onDaftar: () -> Unit, onBerhasil: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var sandi by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var sibuk by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarKembali("Masuk", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).navigationBarsPadding().imePadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Selamat Datang Kembali", Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                "Masuk untuk melanjutkan pemantauan kesehatan lansia.",
                Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            FieldAkun("Email", "Masukkan alamat email Anda", email, { email = it }, KeyboardType.Email)
            FieldAkun("Kata Sandi", "Masukkan kata sandi Anda", sandi, { sandi = it }, KeyboardType.Password, sandi = true)
            Galat(err)
            Tombol("Masuk") {
                if (sibuk) return@Tombol
                if (email.isBlank() || sandi.isEmpty()) {
                    err = "Isi email dan kata sandi."
                } else {
                    err = null
                    sibuk = true
                    auth.masuk(email, sandi) { galat ->
                        sibuk = false
                        if (galat == null) onBerhasil() else err = galat
                    }
                }
            }
            TautanAkun("Belum punya akun?", "Daftar", onDaftar)
            PemisahAtau()
            TombolGoogle { err = INFO_GOOGLE }
        }
    }
}

// ---------- Pilih peran ----------

@Composable
private fun KartuPeran(judul: String, sub: String, ikon: ImageVector, aktif: Boolean, onClick: () -> Unit) {
    val primer = MaterialTheme.colorScheme.primary
    Card(
        onClick = onClick, shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (aktif) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            if (aktif) 2.dp else 1.dp,
            if (aktif) primer else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(primer.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(ikon, null, tint = primer, modifier = Modifier.size(32.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(judul, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = primer)
                Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PeranScreen(auth: AuthViewModel, vm: AppViewModel, onBack: () -> Unit, onLanjut: (String, Long) -> Unit) {
    val akun by auth.akun.collectAsState()
    val daftar by vm.daftar.collectAsState()
    val a = akun

    if (a == null) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            TopBarKembali("Pilih peran", onBack)
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Kosong("Sesi berakhir. Silakan masuk kembali.")
                Tombol("Kembali") { onBack() }
            }
        }
        return
    }

    var pilihan by remember(a.id) { mutableStateOf(a.peran.takeIf { it.isNotEmpty() }) }
    var lansiaPilih by remember(a.id) { mutableStateOf<Long?>(null) }
    var err by remember { mutableStateOf<String?>(null) }
    val terhubung = a.lansiaId?.let { id -> daftar.firstOrNull { it.id == id } }

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopBarKembali("Pilih peran", onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Selamat Datang, ${a.nama}!", Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Text(
                "Peran Anda menentukan fitur dan data apa saja yang akan Anda lihat di aplikasi ini.",
                Modifier.fillMaxWidth(), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center
            )
            KartuPeran("Perawat", "Catat, evaluasi, dan kelola perawatan lansia", Icons.Default.Person, pilihan == PERAN_PERAWAT) {
                pilihan = PERAN_PERAWAT; err = null
            }
            KartuPeran("Keluarga", "Pantau status, panduan, dan bukti perawatan", Icons.Default.Favorite, pilihan == PERAN_KELUARGA) {
                pilihan = PERAN_KELUARGA; err = null
            }
            if (pilihan == PERAN_KELUARGA) {
                if (terhubung != null) {
                    StatusBanner(Level.NORMAL, "Terhubung dengan ${terhubung.nama}", "Anda hanya akan melihat data lansia ini.")
                } else {
                    Panel("Anda keluarga dari siapa?", "Pilih lansia yang Anda wakili. Setelah bergabung, Anda hanya bisa melihat data lansia ini.") {
                        if (daftar.isEmpty()) {
                            Text(
                                "Belum ada lansia terdaftar. Minta perawat menambahkan lansia terlebih dahulu.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        daftar.forEach { p ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { lansiaPilih = p.id; err = null },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = lansiaPilih == p.id, onClick = { lansiaPilih = p.id; err = null })
                                Column {
                                    Text(p.nama, fontWeight = FontWeight.Bold)
                                    Text(
                                        "${p.umur} tahun" + if (p.kamar.isNotEmpty()) " · Kamar ${p.kamar}" else "",
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Galat(err)
            Tombol("Gabung Sekarang") {
                val p = pilihan
                val lid = if (p == PERAN_KELUARGA) (terhubung?.id ?: lansiaPilih) else null
                if (p == null) {
                    err = "Pilih peran Anda terlebih dahulu."
                } else if (p == PERAN_KELUARGA && lid == null) {
                    err = "Pilih lansia yang Anda wakili."
                } else {
                    err = null
                    auth.pilihPeran(p, lid) {
                        if (lid != null) vm.pilih(lid)
                        onLanjut(p, lid ?: 0L)
                    }
                }
            }
        }
    }
}
