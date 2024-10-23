package KondoKit

import rt4.GlIndexedSprite
import rt4.GlSprite
import rt4.SoftwareIndexedSprite
import rt4.SoftwareSprite
import java.awt.Color
import java.awt.image.BufferedImage

// Define interfaces for common sprite types
interface BaseSprite {
    val width: Int
    val height: Int
}

interface IndexedSprite : BaseSprite {
    val pixels: ByteArray
    val palette: IntArray
}

interface NonIndexedSprite : BaseSprite {
    val pixels: IntArray?
}

// Adapter functions for existing sprite types
fun adaptSoftwareSprite(sprite: SoftwareSprite): NonIndexedSprite {
    return object : NonIndexedSprite {
        override val width: Int = sprite.width
        override val height: Int = sprite.height
        override val pixels: IntArray? = sprite.pixels
    }
}

fun adaptSoftwareIndexedSprite(sprite: SoftwareIndexedSprite): IndexedSprite {
    return object : IndexedSprite {
        override val width: Int = sprite.width
        override val height: Int = sprite.height
        override val pixels: ByteArray = sprite.pixels
        override val palette: IntArray = sprite.pallet
    }
}

fun adaptGlSprite(sprite: GlSprite): NonIndexedSprite {
    return object : NonIndexedSprite {
        override val width: Int = sprite.width
        override val height: Int = sprite.height
        override val pixels: IntArray? = sprite.pixels
    }
}

fun adaptGlIndexedSprite(sprite: GlIndexedSprite): IndexedSprite {
    return object : IndexedSprite {
        override val width: Int = sprite.width
        override val height: Int = sprite.height
        override val pixels: ByteArray = sprite.pixels
        override val palette: IntArray = sprite.pallet
    }
}

object SpriteToBufferedImage {
    /**
     * Converts a BaseSprite into a BufferedImage.
     *
     * Handles both indexed and non-indexed sprites, with optional tinting and grayscale.
     *
     * @param sprite The sprite to be converted.
     * @param tint An optional Color to tint the image.
     * @param grayscale If true, converts the image to grayscale.
     * @return The BufferedImage created from the sprite.
     */
    fun convertToBufferedImage(sprite: BaseSprite, tint: Color? = null, grayscale: Boolean = false): BufferedImage {
        val width = sprite.width
        val height = sprite.height
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        when (sprite) {
            is IndexedSprite -> {
                val pixels = sprite.pixels
                val palette = sprite.palette

                // Manually set pixels using the palette
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        val index = pixels[y * width + x].toInt() and 0xFF
                        var color = palette[index]

                        // Apply grayscale or tint if provided
                        val finalColor = if (grayscale) {
                            applyGrayscale(Color(color, true))
                        } else if (tint != null) {
                            applyTint(Color(color, true), tint)
                        } else {
                            Color(color, true)
                        }

                        image.setRGB(x, y, finalColor.rgb)
                    }
                }
            }
            is NonIndexedSprite -> {
                val pixels = sprite.pixels ?: return image // Handle null case for GlSprite

                // Manually set pixels directly
                for (y in 0 until height) {
                    for (x in 0 until width) {
                        var color = pixels[y * width + x]

                        // Apply grayscale or tint if provided
                        val finalColor = if (grayscale) {
                            applyGrayscale(Color(color, true))
                        } else if (tint != null) {
                            applyTint(Color(color, true), tint)
                        } else {
                            Color(color, true)
                        }

                        image.setRGB(x, y, finalColor.rgb)
                    }
                }
            }
        }

        return image
    }

    /**
     * Applies a tint to a given color using the tint's alpha value to control the intensity.
     *
     * @param original The original color.
     * @param tint The tint color to be applied, with alpha controlling the intensity.
     * @return The tinted color.
     */
    fun applyTint(original: Color, tint: Color): Color {
        val alpha = tint.alpha / 255.0 // Normalize alpha to range [0, 1]
        val invAlpha = 1.0 - alpha

        // Blend the tint with the original color based on the alpha
        val r = (original.red * invAlpha + tint.red * alpha).toInt().coerceIn(0, 255)
        val g = (original.green * invAlpha + tint.green * alpha).toInt().coerceIn(0, 255)
        val b = (original.blue * invAlpha + tint.blue * alpha).toInt().coerceIn(0, 255)

        return Color(r, g, b, original.alpha)
    }


    /**
     * Converts a color to grayscale.
     *
     * @param original The original color.
     * @return The grayscale version of the color.
     */
    fun applyGrayscale(original: Color): Color {
        // Calculate the grayscale value using the luminosity method
        val grayValue = (0.3 * original.red + 0.59 * original.green + 0.11 * original.blue).toInt()
        return Color(grayValue, grayValue, grayValue, original.alpha)
    }

    /**
     * Converts an unknown sprite object into a BufferedImage if it matches a known sprite type.
     *
     * @param sprite The sprite object to be converted.
     * @param tint An optional Color to tint the image.
     * @param grayscale If true, converts the image to grayscale.
     * @return The BufferedImage created from the sprite or a default image if unsupported.
     */
    fun getBufferedImageFromSprite(sprite: Any?, tint: Color? = null, grayscale: Boolean = false): BufferedImage {
        return when (sprite) {
            is SoftwareSprite -> convertToBufferedImage(adaptSoftwareSprite(sprite), tint, grayscale)
            is SoftwareIndexedSprite -> convertToBufferedImage(adaptSoftwareIndexedSprite(sprite), tint, grayscale)
            is GlSprite -> convertToBufferedImage(adaptGlSprite(sprite), tint, grayscale)
            is GlIndexedSprite -> convertToBufferedImage(adaptGlIndexedSprite(sprite), tint, grayscale)
            else -> BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB) // Default empty image for unsupported types
        }
    }
}