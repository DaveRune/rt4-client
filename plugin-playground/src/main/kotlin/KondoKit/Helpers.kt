package KondoKit

import java.awt.Color
import java.awt.Dimension
import javax.swing.JPanel

object Helpers {
    fun getSpriteId(skillId: Int) : Int {
        return when (skillId) {
            0 -> 197
            1 -> 199
            2 -> 198
            3 -> 203
            4 -> 200
            5 -> 201
            6 -> 202
            7 -> 212
            8 -> 214
            9 -> 208
            10 -> 211
            11 -> 213
            12 -> 207
            13 -> 210
            14 -> 209
            15 -> 205
            16 -> 204
            17 -> 206
            18 -> 216
            19 -> 217
            20 -> 215
            21 -> 220
            22 -> 221
            23 -> 222
            else -> 222
        }
    }

    fun formatHtmlLabelText(text1: String, color1: Color, text2: String, color2: Color): String {
        return "<html><span style='color:rgb(${color1.red},${color1.green},${color1.blue});'>$text1</span>" +
                "<span style='color:rgb(${color2.red},${color2.green},${color2.blue});'>$text2</span></html>"
    }

    fun formatNumber(value: Int): String {
        return when {
            value >= 1_000_000 -> String.format("%.2fM", value / 1_000_000.0)
            value >= 1_000 -> String.format("%.2fK", value / 1_000.0)
            else -> value.toString()
        }
    }

    fun getProgressBarColor(skillId: Int): Color {
        // Straight from runelite
        return when (skillId) {
            0 -> Color(155, 32, 7)    // ATTACK
            1 -> Color(98, 119, 190)  // DEFENCE
            2 -> Color(4, 149, 90)    // STRENGTH
            3 -> Color(131, 126, 126) // HITPOINTS
            4 -> Color(109, 144, 23)  // RANGED
            5 -> Color(159, 147, 35)  // PRAYER
            6 -> Color(50, 80, 193)   // MAGIC
            7 -> Color(112, 35, 134)  // COOKING
            8 -> Color(52, 140, 37)   // WOODCUTTING
            9 -> Color(3, 141, 125)   // FLETCHING
            10 -> Color(106, 132, 164) // FISHING
            11 -> Color(189, 120, 25)  // FIREMAKING
            12 -> Color(151, 110, 77)  // CRAFTING
            13 -> Color(108, 107, 82)  // SMITHING
            14 -> Color(93, 143, 167)  // MINING
            15 -> Color(7, 133, 9)    // HERBLORE
            16 -> Color(58, 60, 137)  // AGILITY
            17 -> Color(108, 52, 87)  // THIEVING
            18 -> Color(100, 100, 100) // SLAYER
            19 -> Color(101, 152, 63)  // FARMING
            20 -> Color(170, 141, 26)  // RUNECRAFT
            21 -> Color(92, 89, 65)   // HUNTER
            22 -> Color(130, 116, 95)  // CONSTRUCTION
            23 -> Color(150, 50, 50)  // Placeholder for any additional skill
            else -> Color(128, 128, 128) // Default grey for unhandled skill IDs
        }
    }

    class Spacer(width: Int = 0, height: Int = 0) : JPanel() {
        init {
            preferredSize = Dimension(width, height)
            maximumSize = preferredSize
            minimumSize = preferredSize
            isOpaque = false
        }
    }
}