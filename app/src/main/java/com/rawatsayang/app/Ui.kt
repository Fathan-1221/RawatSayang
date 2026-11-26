package com.rawatsayang.app

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun String.angka(): Double? = replace(',', '.').trim().toDoubleOrNull()
fun Double.f1(): String = "%.1f".format(this)
fun Long.tgl(): String = SimpleDateFormat("d MMM, HH:mm", Locale("id", "ID")).format(Date(this))

fun Level.warna(): Color = when (this) { Level.NORMAL -> Ok; Level.WASPADA -> Warn; Level.KRITIS -> Bad }
fun Level.ikon(): ImageVector = when (this) {
    Level.NORMAL -> Icons.Default.CheckCircle
    Level.WASPADA -> Icons.Default.Info
    Level.KRITIS -> Icons.Default.Warning
}

@Composable
fun Dot(level: Level) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(level.warna()))
}

@Composable
fun StatusBanner(level: Level, judul: String, isi: String = "", poin: List<String> = emptyList()) {
    val c = level.warna()
    Card(
        colors = CardDefaults.cardColors(containerColor = c.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(22.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(c), contentAlignment = Alignment.Center) {
                    Icon(level.ikon(), contentDescription = null, tint = Color.White)
                }
                Text(judul, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c)
            }
            if (isi.isNotEmpty()) Text(isi, style = MaterialTheme.typography.bodyMedium)
            poin.forEach {
                Text("• $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun Panel(judul: String, sub: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(judul, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (sub != null) Text(sub, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
fun Field(label: String, satuan: String, nilai: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = nilai, onValueChange = onChange,
        label = { Text(label) }, suffix = { Text(satuan) },
        singleLine = true, shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun TeksField(label: String, nilai: String, onChange: (String) -> Unit, baris: Int = 1) {
    OutlinedTextField(
        value = nilai, onValueChange = onChange, label = { Text(label) },
        singleLine = baris == 1, minLines = baris, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
fun Tombol(teks: String, onClick: () -> Unit) {
    Button(onClick = onClick, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Text(teks, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun Galat(pesan: String?) {
    if (pesan != null) Text(pesan, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
fun Tile(label: String, nilai: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primaryContainer).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(nilai, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
    }
}

@Composable
fun Kosong(pesan: String) {
    Panel("Belum ada data") {
        Text(pesan, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun Riwayat(judul: String, baris: List<Triple<String, String, Level>>) {
    if (baris.isEmpty()) return
    Panel(judul) {
        baris.forEach { (waktu, teks, level) ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.padding(top = 5.dp)) { Dot(level) }
                Column {
                    Text(teks, style = MaterialTheme.typography.bodyMedium)
                    Text(waktu, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun KartuCatatan(judul: String, waktu: String, isi: String, foto: String?) {
    Panel(judul, waktu) {
        if (isi.isNotEmpty()) Text(isi, style = MaterialTheme.typography.bodyMedium)
        if (foto != null) FotoBukti(foto, Modifier.fillMaxWidth().height(180.dp))
    }
}

@Composable
fun LineChart(data: List<Int>, modifier: Modifier = Modifier) {
    val garis = MaterialTheme.colorScheme.primary
    val kisi = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().weight(1f)) {
            if (data.size < 2) return@Canvas
            val lo = (data.min() - 6).toFloat()
            val hi = (data.max() + 6).toFloat()
            val stepX = size.width / (data.size - 1)
            fun y(v: Int) = size.height - (v - lo) / (hi - lo) * size.height
            for (i in 0..3) {
                val gy = size.height * i / 3f
                drawLine(kisi, Offset(0f, gy), Offset(size.width, gy), 2f)
            }
            val path = Path().apply {
                data.forEachIndexed { i, v -> if (i == 0) moveTo(0f, y(v)) else lineTo(stepX * i, y(v)) }
            }
            val isi = Path().apply {
                addPath(path)
                lineTo(size.width, size.height)
                lineTo(0f, size.height)
                close()
            }
            drawPath(isi, Brush.verticalGradient(listOf(garis.copy(alpha = 0.28f), Color.Transparent)))
            drawPath(path, garis, style = Stroke(width = 6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            data.forEachIndexed { i, v -> drawCircle(garis, 9f, Offset(stepX * i, y(v))) }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            data.indices.forEach {
                Text("H${it + 1}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ---------- Header & navigasi ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Header(sub: String, judul: String, vm: AppViewModel, onKeluar: () -> Unit, bisaPilih: Boolean = true) {
    val daftar by vm.daftar.collectAsState()
    val terpilih by vm.terpilih.collectAsState()
    var buka by remember { mutableStateOf(false) }
    val permukaan = MaterialTheme.colorScheme.surface
    Column(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(GreenDark, Green)), RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
            .statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(sub, style = MaterialTheme.typography.labelLarge, color = Color.White.copy(alpha = 0.75f))
                Text(judul, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            }
            TextButton(onClick = onKeluar) { Text("Keluar", color = Color.White) }
        }
        if (bisaPilih) {
            ExposedDropdownMenuBox(expanded = buka, onExpandedChange = { buka = it }) {
                OutlinedTextField(
                    value = terpilih?.nama ?: "Belum ada lansia", onValueChange = {}, readOnly = true,
                    label = { Text("Pilih lansia") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = buka) },
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = permukaan, unfocusedContainerColor = permukaan),
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
        } else {
            Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.14f), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text("Anda memantau", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.75f))
                    Text(terpilih?.nama ?: "-", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun BarMenu(menu: List<Pair<String, ImageVector>>, terpilih: Int, onPilih: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        menu.forEachIndexed { i, (label, ikon) ->
            NavigationBarItem(
                selected = terpilih == i, onClick = { onPilih(i) },
                icon = { Icon(ikon, contentDescription = label) },
                label = { Text(label) }
            )
        }
    }
}
