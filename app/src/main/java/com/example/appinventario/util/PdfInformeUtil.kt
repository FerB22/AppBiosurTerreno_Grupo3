package com.example.appinventario.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.appinventario.model.Servicio
import java.io.File
import java.io.FileOutputStream

/**
 * Generador nativo de informes técnicos en formato PDF con evidencia fotográfica («antes» y «después»).
 * Utiliza `android.graphics.pdf.PdfDocument` y expone el archivo de forma segura mediante `FileProvider`.
 */
object PdfInformeUtil {

    private const val ANCHO_A4 = 595
    private const val ALTO_A4 = 842
    private const val MARGEN = 36f

    fun generarPdfYUri(context: Context, servicio: Servicio): Uri {
        val documento = PdfDocument()
        val paginaInfo = PdfDocument.PageInfo.Builder(ANCHO_A4, ALTO_A4, 1).create()
        val pagina = documento.startPage(paginaInfo)
        val canvas = pagina.canvas

        dibujarContenido(context, canvas, servicio)

        documento.finishPage(pagina)

        val archivo = File(context.cacheDir, "Informe_Biosur_Orden_${servicio.id}.pdf")
        FileOutputStream(archivo).use { salida ->
            documento.writeTo(salida)
        }
        documento.close()

        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", archivo)
    }

    private fun dibujarContenido(context: Context, canvas: Canvas, servicio: Servicio) {
        val paintFondo = Paint().apply { color = Color.parseColor("#1B5E20") }
        val paintTitulo = Paint().apply {
            color = Color.WHITE
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintSubtitulo = Paint().apply {
            color = Color.parseColor("#E8F5E9")
            textSize = 11f
            isAntiAlias = true
        }
        val paintSeccion = Paint().apply {
            color = Color.parseColor("#1B5E20")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val paintTexto = TextPaint().apply {
            color = Color.parseColor("#212121")
            textSize = 11f
            isAntiAlias = true
        }
        val paintBorde = Paint().apply {
            color = Color.parseColor("#BDBDBD")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // 1. Encabezado corporativo
        canvas.drawRect(0f, 0f, ANCHO_A4.toFloat(), 84f, paintFondo)
        canvas.drawText("INFORME TÉCNICO DE FAENA — BIOSUR SpA", MARGEN, 36f, paintTitulo)
        canvas.drawText(
            "Orden #${servicio.id}  |  ${servicio.tipoServicio.etiqueta}  |  Estado: ${servicio.estado.etiqueta}",
            MARGEN,
            58f,
            paintSubtitulo
        )

        var y = 112f

        // 2. Antecedentes operativos
        canvas.drawText("1. Antecedentes de la faena", MARGEN, y, paintSeccion)
        y += 18f
        canvas.drawText("Fecha y hora: ${servicio.fecha} (${servicio.horaInicio})", MARGEN, y, paintTexto)
        y += 16f
        canvas.drawText("Supervisor responsable: ${servicio.supervisor}", MARGEN, y, paintTexto)
        y += 16f
        val gpsTexto = if (servicio.latitud != null && servicio.longitud != null) {
            "${servicio.latitud}, ${servicio.longitud}"
        } else {
            "No registrado"
        }
        canvas.drawText("Coordenadas GPS: $gpsTexto", MARGEN, y, paintTexto)
        y += 20f

        // Descripción multilínea
        val anchoTexto = (ANCHO_A4 - MARGEN * 2).toInt()
        val layoutDesc = StaticLayout.Builder
            .obtain("Detalle técnico: ${servicio.descripcion}", 0, "Detalle técnico: ${servicio.descripcion}".length, paintTexto, anchoTexto)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .build()
        canvas.save()
        canvas.translate(MARGEN, y)
        layoutDesc.draw(canvas)
        canvas.restore()
        y += layoutDesc.height + 22f

        // 3. Resumen de horas y costo
        canvas.drawText("2. Valorización técnica estimada", MARGEN, y, paintSeccion)
        y += 18f
        val totalClp = "%,.0f CLP".format(servicio.costoEstimado).replace(',', ' ')
        canvas.drawText("Horas hombre registradas: ${servicio.horasTrabajadas} h", MARGEN, y, paintTexto)
        y += 16f
        val paintTotal = Paint(paintTexto).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        canvas.drawText("Total estimado (mano de obra + recursos): $totalClp", MARGEN, y, paintTotal)
        y += 28f

        // 4. Evidencia fotográfica (Antes y Después)
        canvas.drawText("3. Evidencia fotográfica en terreno", MARGEN, y, paintSeccion)
        y += 14f

        val anchoCaja = (ANCHO_A4 - MARGEN * 2 - 16f) / 2f
        val altoCaja = 210f

        val bmpAntes = CameraUtil.cargarAndroidBitmapDesdeUri(context, servicio.fotoAntesUri, 600, 600)
        val bmpDespues = CameraUtil.cargarAndroidBitmapDesdeUri(context, servicio.fotoDespuesUri, 600, 600)

        dibujarCajaFoto(canvas, "Evidencia inicial (Antes)", bmpAntes, MARGEN, y, anchoCaja, altoCaja, paintTexto, paintBorde)
        dibujarCajaFoto(canvas, "Evidencia final (Después)", bmpDespues, MARGEN + anchoCaja + 16f, y, anchoCaja, altoCaja, paintTexto, paintBorde)

        // 5. Pie de página
        val paintPie = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }
        canvas.drawText("Generado por Biosur Terreno · Respaldo operativo oficial", MARGEN, ALTO_A4 - 24f, paintPie)
    }

    private fun dibujarCajaFoto(
        canvas: Canvas,
        etiqueta: String,
        bitmap: Bitmap?,
        x: Float,
        y: Float,
        ancho: Float,
        alto: Float,
        paintTexto: Paint,
        paintBorde: Paint
    ) {
        canvas.drawText(etiqueta, x, y + 12f, paintTexto)
        val rectCaja = RectF(x, y + 18f, x + ancho, y + 18f + alto)
        canvas.drawRect(rectCaja, paintBorde)

        if (bitmap == null) {
            canvas.drawText("Sin fotografía adjunta", x + 16f, rectCaja.centerY(), paintTexto)
            return
        }

        val escala = minOf(rectCaja.width() / bitmap.width, rectCaja.height() / bitmap.height)
        val w = bitmap.width * escala
        val h = bitmap.height * escala
        val left = rectCaja.centerX() - w / 2f
        val top = rectCaja.centerY() - h / 2f
        val destino = RectF(left, top, left + w, top + h)
        canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), destino, null)
    }
}
