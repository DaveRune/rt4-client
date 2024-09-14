package KondoKit

import java.awt.Canvas
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics
import java.awt.image.BufferedImage

class ImageCanvas(private val image: BufferedImage) : Canvas() {

    init {
        // Manually set the alpha value to 255 (fully opaque) only for pixels that are not fully transparent
        val width = image.width
        val height = image.height
        for (y in 0 until height) {
            for (x in 0 until width) {
                // Retrieve the current pixel color
                val color = image.getRGB(x, y)

                // Check if the pixel is not fully transparent (i.e., color is not 0)
                if (color != 0) {
                    // Ensure the alpha is set to 255 (fully opaque)
                    val newColor = (color and 0x00FFFFFF) or (0xFF shl 24)

                    // Set the pixel with the updated color
                    image.setRGB(x, y, newColor)
                }
            }
        }
    }


    override fun paint(g: Graphics) {
        super.paint(g)
        g.color = Color(27, 27, 27)
        g.fillRect(0, 0, width, height)
        g.drawImage(image, 0, 0, width, height, this)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(image.width, image.height)
    }
}