package com.example.appinventario.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CameraUtil {

    /**
     * Genera un Uri seguro mediante FileProvider para que la aplicación de cámara
     * pueda escribir la fotografía en la memoria interna de la app sin exponer rutas directas.
     */
    fun crearUriParaFoto(context: Context, prefijo: String = "FAENA"): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nombreArchivo = "${prefijo}_${timeStamp}.jpg"
        val storageDir = File(context.filesDir, "images").apply {
            if (!exists()) mkdirs()
        }
        val file = File(storageDir, nombreArchivo)
        val authority = "${context.packageName}.fileprovider"

        return FileProvider.getUriForFile(context, authority, file)
    }

    /**
     * Decodifica una imagen desde una URI de FileProvider a un ImageBitmap de Compose
     * aplicando submuestreo seguro para prevenir errores de memoria (OutOfMemoryError).
     */
    fun cargarBitmapDesdeUri(context: Context, uriString: String?, reqWidth: Int = 800, reqHeight: Int = 800): ImageBitmap? {
        if (uriString.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(uriString)

            // Obtener dimensiones sin alojar mapa de bits completo
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            }

            // Calcular factor de submuestreo
            var inSampleSize = 1
            if (boundsOptions.outHeight > reqHeight || boundsOptions.outWidth > reqWidth) {
                val halfHeight = boundsOptions.outHeight / 2
                val halfWidth = boundsOptions.outWidth / 2
                while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                    inSampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, decodeOptions)?.asImageBitmap()
            }
        } catch (_: Exception) {
            null
        }
    }
}
