package com.rawatsayang.app

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

private fun fileBaru(ctx: Context) = File(ctx.filesDir, "bukti_${System.currentTimeMillis()}.jpg")

private fun salinFoto(ctx: Context, uri: Uri): String? = try {
    val f = fileBaru(ctx)
    ctx.contentResolver.openInputStream(uri)?.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
    f.path
} catch (e: Exception) {
    null
}

@Composable
fun FotoBukti(path: String, modifier: Modifier = Modifier) {
    val bmp = remember(path) {
        BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = 4 })?.asImageBitmap()
    }
    if (bmp != null) {
        Image(bmp, contentDescription = "Foto bukti", modifier = modifier.clip(RoundedCornerShape(16.dp)), contentScale = ContentScale.Crop)
    }
}

@Composable
fun FotoPicker(path: String?, onPath: (String?) -> Unit) {
    val ctx = LocalContext.current
    var kamera by remember { mutableStateOf<File?>(null) }
    val galeri = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPath(salinFoto(ctx, uri))
    }
    val ambil = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = kamera
        if (ok && f != null) onPath(f.path)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Foto bukti (opsional)", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
                onClick = {
                    val f = fileBaru(ctx)
                    kamera = f
                    ambil.launch(FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", f))
                },
                modifier = Modifier.weight(1f)
            ) { Text("Kamera") }
            OutlinedButton(
                onClick = { galeri.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                modifier = Modifier.weight(1f)
            ) { Text("Galeri") }
        }
        if (path != null) {
            FotoBukti(path, Modifier.fillMaxWidth().height(180.dp))
            TextButton(onClick = { onPath(null) }) { Text("Hapus foto") }
        }
    }
}
