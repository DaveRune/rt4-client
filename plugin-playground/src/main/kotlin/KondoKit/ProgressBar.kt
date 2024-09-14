package KondoKit

import java.awt.Canvas
import java.awt.Color
import java.awt.Font
import java.awt.Graphics

class ProgressBar(
    private var progress: Double,
    private val barColor: Color,
    private var currentLevel: Int = 0,
    private var nextLevel: Int = 1
) : Canvas() {

    init {
        font = Font("Arial", Font.PLAIN, 12)
    }

    override fun paint(g: Graphics) {
        super.paint(g)

        // Draw the filled part of the progress bar
        g.color = barColor
        val width = (progress * this.width / 100).toInt()
        g.fillRect(0, 0, width, this.height)

        // Draw the unfilled part of the progress bar
        g.color = Color(100, 100, 100)
        g.fillRect(width, 0, this.width - width, this.height)

        // Draw the current level on the far left
        g.color = Color(255, 255, 255)
        g.drawString("Lvl. $currentLevel", 5, this.height / 2 + 4)

        // Draw the percentage in the middle
        val percentageText = String.format("%.2f%%", progress)
        val percentageWidth = g.fontMetrics.stringWidth(percentageText)
        g.drawString(percentageText, (this.width - percentageWidth) / 2, this.height / 2 + 4)

        // Draw the next level on the far right
        val nextLevelText = "Lvl. $nextLevel"
        val nextLevelWidth = g.fontMetrics.stringWidth(nextLevelText)
        g.drawString(nextLevelText, this.width - nextLevelWidth - 5, this.height / 2 + 4)
    }

    fun updateProgress(newProgress: Double, currentLevel: Int, nextLevel: Int, isVisible : Boolean) {
        this.progress = newProgress
        this.currentLevel = currentLevel
        this.nextLevel = nextLevel
        if(isVisible)
            repaint()
    }
}
