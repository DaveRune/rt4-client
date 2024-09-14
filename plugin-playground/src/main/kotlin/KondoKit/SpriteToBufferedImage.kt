package KondoKit

import rt4.GlIndexedSprite
import rt4.GlSprite
import rt4.SoftwareIndexedSprite
import rt4.SoftwareSprite
import java.awt.image.BufferedImage

object SpriteToBufferedImage {
    /**
     * Converts a SoftwareSprite back into a BufferedImage.
     *
     * @param sprite The sprite to be converted.
     * @return The BufferedImage created from the sprite.
     */
    fun convertToBufferedImage(sprite: SoftwareSprite): BufferedImage {
        val width = sprite.width
        val height = sprite.height
        val pixels = sprite.pixels

        // Create a BufferedImage with ARGB color model
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // Manually set pixels and print the pixel data
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                image.setRGB(x, y, pixel)
            }
        }
        return image
    }

    fun convertToBufferedImage(sprite: SoftwareIndexedSprite): BufferedImage {
        val width = sprite.width
        val height = sprite.height
        val pixels = sprite.pixels // byte[]
        val palette = sprite.pallet

        // Create a BufferedImage with ARGB color model
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // Manually set pixels using the palette
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Get the index from the sprite's pixel array
                val index = pixels[y * width + x].toInt() and 0xFF
                // Map the index to a color in the palette
                val color = palette[index]
                // Set the ARGB color in the BufferedImage
                image.setRGB(x, y, color)
            }
        }
        return image
    }

    fun convertToBufferedImage(sprite: GlIndexedSprite): BufferedImage {
        val width = sprite.width
        val height = sprite.height
        val pixels = sprite.pixels // byte[]
        val palette = sprite.pallet

        // Create a BufferedImage with ARGB color model
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // Manually set pixels using the palette
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Get the index from the sprite's pixel array
                val index = pixels[y * width + x].toInt() and 0xFF
                // Map the index to a color in the palette
                val color = palette[index]
                // Set the ARGB color in the BufferedImage
                image.setRGB(x, y, color)
            }
        }
        return image
    }


    fun convertToBufferedImage(sprite: GlSprite): BufferedImage {
        val width = sprite.width
        val height = sprite.height
        val pixels = sprite.pixels

        // Create a BufferedImage with ARGB color model
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        if(pixels == null) {
            return image
        }

        // Manually set pixels and print the pixel data
        for (y in 0 until height) {
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                image.setRGB(x, y, pixel)
            }
        }
        return image
    }

    fun getBufferedImageFromSprite(sprite: Any?) : BufferedImage {
        return when(sprite){
            is GlSprite -> convertToBufferedImage(sprite)
            is SoftwareSprite -> convertToBufferedImage(sprite)
            is SoftwareIndexedSprite -> convertToBufferedImage(sprite)
            is GlIndexedSprite -> convertToBufferedImage(sprite)
            else -> BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)
        }
    }
}