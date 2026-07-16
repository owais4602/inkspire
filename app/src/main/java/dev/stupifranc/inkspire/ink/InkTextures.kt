package dev.stupifranc.inkspire.ink

import android.content.Context
import android.graphics.Bitmap
import androidx.ink.brush.ExperimentalInkCustomBrushApi
import androidx.ink.brush.StockBrushes
import androidx.ink.brush.StockTextureBitmapStore
import dev.stupifranc.inkspire.core.TextureGen

@OptIn(ExperimentalInkCustomBrushApi::class)
object InkTextures {
    private var store: StockTextureBitmapStore? = null

    fun getStore(context: Context): StockTextureBitmapStore {
        store?.let { return it }
        val s = StockTextureBitmapStore(context.applicationContext.resources)
        s.preloadStockBrushesTextures(StockBrushes.pencilUnstable)
        
        s.addTexture("inkspire:granulation", createBitmap(TextureGen.granulation(256, 42L), 256))
        s.addTexture("inkspire:dryStreaks", createBitmap(TextureGen.dryStreaks(256, 42L), 256))
        s.addTexture("inkspire:paperGrain", createBitmap(TextureGen.paperGrain(256, 42L), 256))
        
        store = s
        return s
    }

    private fun createBitmap(pixels: IntArray, size: Int): Bitmap {
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }
}
