package com.example.appinventario.util

import android.content.Context
import android.net.Uri
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
}
