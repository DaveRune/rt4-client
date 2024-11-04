package KondoKit

import KondoKit.plugin.Companion.PROGRESS_BAR_FILL
import KondoKit.plugin.Companion.secondaryColor
import java.awt.Canvas
import java.awt.Color
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics

class ProgressBar(
        private var progress: Double,
        private val barColor: Color,
        private var currentLevel: Int = 0,
        private var nextLevel: Int = 1
) : Canvas() {

    init {
        font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
    }

    override fun paint(g: Graphics) {
        super.paint(g)

        // Draw the filled part of the progress bar
        g.color = barColor
        val width = (progress * this.width / 100).toInt()
        g.fillRect(0, 0, width, this.height)

        // Draw the unfilled part of the progress bar
        g.color = PROGRESS_BAR_FILL
        g.fillRect(width, 0, this.width - width, this.height)

        // Variables for text position
        val textY = this.height / 2 + 6

        // Draw the current level on the far left
        drawTextWithShadow(g, "Lvl. $currentLevel", 5, textY, secondaryColor)

        // Draw the percentage in the middle
        val percentageText = String.format("%.2f%%", progress)
        val percentageWidth = g.fontMetrics.stringWidth(percentageText)
        drawTextWithShadow(g, percentageText, (this.width - percentageWidth) / 2, textY, secondaryColor)

        // Draw the next level on the far right
        val nextLevelText = "Lvl. $nextLevel"
        val nextLevelWidth = g.fontMetrics.stringWidth(nextLevelText)
        drawTextWithShadow(g, nextLevelText, this.width - nextLevelWidth - 5, textY, secondaryColor)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(220, 16) // Force the height to 16px, width can be anything appropriate
    }

    override fun getMinimumSize(): Dimension {
        return Dimension(220, 16) // Force the minimum height to 16px, width can be smaller
    }

    fun updateProgress(newProgress: Double, currentLevel: Int, nextLevel: Int, isVisible : Boolean) {
        this.progress = newProgress
        this.currentLevel = currentLevel
        this.nextLevel = nextLevel
        if(isVisible)
            repaint()
    }

    // Helper function to draw text with a shadow effect
    private fun drawTextWithShadow(g: Graphics, text: String, x: Int, y: Int, textColor: Color) {
        // Draw shadow (black text with -1 x and -1 y offset)
        g.color = Color(0, 0, 0)
        g.drawString(text, x + 1, y + 1)

        // Draw actual text on top
        g.color = textColor
        g.drawString(text, x, y)
    }
}
