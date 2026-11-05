package com.example.fileshifter.data.source

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter

/** Genera codigos QR y de barras 100% offline con ZXing. */
object GeneradorCodigos {

    fun qr(texto: String, lado: Int = 720): Bitmap = dibujar(
        MultiFormatWriter().encode(texto, BarcodeFormat.QR_CODE, lado, lado, mapOf(EncodeHintType.MARGIN to 1))
    )

    fun barras(texto: String, ancho: Int = 900, alto: Int = 320): Bitmap = dibujar(
        MultiFormatWriter().encode(texto, BarcodeFormat.CODE_128, ancho, alto, mapOf(EncodeHintType.MARGIN to 4))
    )

    private fun dibujar(matriz: com.google.zxing.common.BitMatrix): Bitmap {
        val ancho = matriz.width
        val alto = matriz.height
        val pixeles = IntArray(ancho * alto)
        for (y in 0 until alto) {
            for (x in 0 until ancho) {
                pixeles[y * ancho + x] = if (matriz[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixeles, 0, ancho, 0, 0, ancho, alto)
        }
    }
}
