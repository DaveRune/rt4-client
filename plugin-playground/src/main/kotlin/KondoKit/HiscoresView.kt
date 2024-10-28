package KondoKit

import KondoKit.Constants.COLOR_BACKGROUND_DARK
import KondoKit.Constants.SKILL_DISPLAY_ORDER
import KondoKit.Constants.SKILL_SPRITE_DIMENSION
import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.Helpers.getSpriteId
import KondoKit.Helpers.showToast
import KondoKit.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.plugin.Companion.POPUP_FOREGROUND
import KondoKit.plugin.Companion.TOOLTIP_BACKGROUND
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import KondoKit.plugin.StateManager.focusedView
import com.google.gson.Gson
import plugin.api.API
import rt4.Sprites
import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import javax.swing.*
import javax.swing.border.MatteBorder
import kotlin.math.floor

object Constants {
    // Sprite IDs
    const val COMBAT_LVL_SPRITE = 168
    const val IRONMAN_SPRITE = 4
    const val MAG_SPRITE = 1423
    const val LVL_BAR_SPRITE = 898

    // Dimensions
    val SEARCH_FIELD_DIMENSION = Dimension(230, 30)
    val ICON_DIMENSION_SMALL = Dimension(12, 12)
    val ICON_DIMENSION_LARGE = Dimension(30, 30)
    val HISCORE_PANEL_DIMENSION = Dimension(230, 500)
    val FILTER_PANEL_DIMENSION = Dimension(230, 30)
    val SKILLS_PANEL_DIMENSION = Dimension(230, 290)
    val TOTAL_COMBAT_PANEL_DIMENSION = Dimension(230, 30)
    val SKILL_PANEL_DIMENSION = Dimension(76, 35)
    val IMAGE_CANVAS_DIMENSION = Dimension(20, 20)
    val SKILL_SPRITE_DIMENSION = Dimension(14, 14)
    val NUMBER_LABEL_DIMENSION = Dimension(20, 20)

    // Colors
    val COLOR_BACKGROUND_DARK = WIDGET_COLOR
    val COLOR_BACKGROUND_MEDIUM = VIEW_BACKGROUND_COLOR
    val COLOR_FOREGROUND_LIGHT = POPUP_FOREGROUND

    // Fonts
    val FONT_ARIAL_PLAIN_14 = Font("Arial", Font.PLAIN, 14)
    val FONT_ARIAL_PLAIN_12 = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
    val FONT_ARIAL_BOLD_12 = Font("Arial", Font.BOLD, 12)
    val SKILL_DISPLAY_ORDER = arrayOf(0,3,14,2,16,13,1,15,10,4,17,7,5,12,11,6,9,8,20,18,19,22,21,23)
}

var text: String = ""

object HiscoresView {

    const val VIEW_NAME = "HISCORE_SEARCH_VIEW"
    var hiScoreView: JPanel? = null
    class CustomSearchField(private val hiscoresPanel: JPanel) : Canvas() {

        private var cursorVisible: Boolean = true
        private val gson = Gson()

        private val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(Constants.MAG_SPRITE))
        private val imageCanvas = bufferedImageSprite.let {
            ImageCanvas(it).apply {
                preferredSize = Constants.ICON_DIMENSION_SMALL
                size = preferredSize
                minimumSize = preferredSize
                maximumSize = preferredSize
                fillColor = COLOR_BACKGROUND_DARK
            }
        }

        init {
            preferredSize = Constants.SEARCH_FIELD_DIMENSION
            background = Constants.COLOR_BACKGROUND_DARK
            foreground = Constants.COLOR_FOREGROUND_LIGHT
            font = Constants.FONT_ARIAL_PLAIN_14
            minimumSize = preferredSize
            maximumSize = preferredSize

            addKeyListener(object : KeyAdapter() {
                override fun keyTyped(e: KeyEvent) {
                    // Prevent null character from being typed on Ctrl+A & Ctrl+V
                    if (e.isControlDown && (e.keyChar == '\u0001' || e.keyChar == '\u0016')) {
                        e.consume()
                        return
                    }
                    if (e.keyChar == '\b') {
                        if (text.isNotEmpty()) {
                            text = text.dropLast(1)
                        }
                    } else if (e.keyChar == '\n') {
                        searchPlayer(text)
                    } else {
                        text += e.keyChar
                    }
                    SwingUtilities.invokeLater {
                        repaint()
                    }
                }
                override fun keyPressed(e: KeyEvent) {
                    if (e.isControlDown) {
                        when (e.keyCode) {
                            KeyEvent.VK_A -> {
                                text = ""
                                repaint()
                            }
                            KeyEvent.VK_V -> {
                                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                                val pasteText = clipboard.getData(DataFlavor.stringFlavor) as String
                                text += pasteText
                                SwingUtilities.invokeLater {
                                    repaint()
                                }
                            }
                        }
                    }
                }
            })

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.x > width - 20 && e.y < 20) {
                        text = ""
                        SwingUtilities.invokeLater {
                            repaint()
                        }
                    }
                }
            })

            Timer(500) {
                cursorVisible = !cursorVisible
                if(focusedView == VIEW_NAME)
                    SwingUtilities.invokeLater {
                        repaint()
                    }
            }.start()
        }

        override fun paint(g: Graphics) {
            super.paint(g)
            g.color = foreground
            g.font = font

            val fm = g.fontMetrics
            val cursorX = fm.stringWidth(text) + 30

            imageCanvas.let { canvas ->
                val imgG = g.create(5, 5, canvas.width, canvas.height)
                canvas.paint(imgG)
                imgG.dispose()
            }

            g.drawString(text, 30, 20)

            if (cursorVisible && hasFocus()) {
                g.drawLine(cursorX, 5, cursorX, 25)
            }

            if (text.isNotEmpty()) {
                g.color = Color.RED
                g.drawString("x", width - 20, 20)
            }
        }

        fun searchPlayer(username: String) {
            text = username.replace(" ", "_")
            val apiUrl = "http://api.2009scape.org:3000/hiscores/playerSkills/1/${text.toLowerCase()}"

            updateHiscoresView(null, "Searching...")

            Thread {
                try {
                    val url = URL(apiUrl)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"

                    // If a request take longer than 5 seconds timeout.
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val reader = BufferedReader(InputStreamReader(connection.inputStream))
                        val response = reader.use { it.readText() }
                        reader.close()

                        SwingUtilities.invokeLater {
                            updatePlayerData(response, username)
                        }
                    } else {
                        SwingUtilities.invokeLater {
                            showToast(hiscoresPanel, "Player not found!", JOptionPane.ERROR_MESSAGE)
                        }
                    }
                } catch (e: SocketTimeoutException) {
                    SwingUtilities.invokeLater {
                        showToast(hiscoresPanel, "Request timed out", JOptionPane.ERROR_MESSAGE)
                    }
                } catch (e: Exception) {
                    // Handle other errors
                    SwingUtilities.invokeLater {
                        showToast(hiscoresPanel, "Error fetching data!", JOptionPane.ERROR_MESSAGE)
                    }
                }
            }.start()
        }


        private fun updatePlayerData(jsonResponse: String, username: String) {
            val hiscoresResponse = gson.fromJson(jsonResponse, HiscoresResponse::class.java)
            updateHiscoresView(hiscoresResponse, username)
        }

        private fun updateHiscoresView(data: HiscoresResponse?, username: String) {
            val playerNameLabel = findComponentByName(hiscoresPanel, "playerNameLabel") as? JPanel
            playerNameLabel?.removeAll() // Clear previous components
            var nameLabel = JLabel(formatHtmlLabelText(username, secondaryColor, "", primaryColor), JLabel.CENTER).apply {
                font = Constants.FONT_ARIAL_BOLD_12
                foreground = Constants.COLOR_FOREGROUND_LIGHT
                border = BorderFactory.createEmptyBorder(0, 6, 0, 0) // Top, Left, Bottom, Right padding
            }
            playerNameLabel?.add(nameLabel)
            playerNameLabel?.revalidate()
            playerNameLabel?.repaint()

            if(data == null) return

            playerNameLabel?.removeAll()

            val ironMode = data.info.iron_mode

            if (ironMode != "0") {
                val ironmanBufferedImage = getBufferedImageFromSprite(Sprites.nameIcons[Constants.IRONMAN_SPRITE + ironMode.toInt() - 1])
                val imageCanvas = ironmanBufferedImage.let {
                    ImageCanvas(it).apply {
                        preferredSize = Constants.IMAGE_CANVAS_DIMENSION
                        size = Constants.IMAGE_CANVAS_DIMENSION
                    }
                }

                playerNameLabel?.add(imageCanvas)
            }

            val exp_multiplier = data.info.exp_multiplier
            nameLabel = JLabel(formatHtmlLabelText(username, secondaryColor, " (${exp_multiplier}x)", primaryColor), JLabel.CENTER).apply {
                font = Constants.FONT_ARIAL_BOLD_12
                foreground = Constants.COLOR_FOREGROUND_LIGHT
                border = BorderFactory.createEmptyBorder(0, 6, 0, 0) // Top, Left, Bottom, Right padding
            }


            playerNameLabel?.add(nameLabel)

            playerNameLabel?.revalidate()
            playerNameLabel?.repaint()

            // Update skill labels
            data.skills.forEachIndexed { index, skill ->
                val labelName = "skillLabel_$index"
                val numberLabel = findComponentByName(hiscoresPanel, labelName) as? JLabel
                numberLabel?.text = skill.static
            }

            updateTotalAndCombatLevel(data.skills, true)

            hiscoresPanel.revalidate()
            hiscoresPanel.repaint()
        }

        private fun updateTotalAndCombatLevel(skills: List<Skill>, isMemberWorld: Boolean) {
            val totalLevel = skills.sumBy { it.static.toInt() }
            val totalLevelLabel = findComponentByName(hiscoresPanel, "totalLevelLabel") as? JLabel
            totalLevelLabel?.text = totalLevel.toString()

            val attack = skills.find { it.id == "0" }?.static?.toInt() ?: 1
            val defence = skills.find { it.id == "1" }?.static?.toInt() ?: 1
            val strength = skills.find { it.id == "2" }?.static?.toInt() ?: 1
            val hitpoints = skills.find { it.id == "3" }?.static?.toInt() ?: 1
            val ranged = skills.find { it.id == "4" }?.static?.toInt() ?: 1
            val prayer = skills.find { it.id == "5" }?.static?.toInt() ?: 1
            val magic = skills.find { it.id == "6" }?.static?.toInt() ?: 1
            val summoning = skills.find { it.id == "23" }?.static?.toInt() ?: 1

            val combatLevel = calculateCombatLevel(attack, defence, strength, hitpoints, prayer, ranged, magic, summoning, true)
            val combatLevelLabel = findComponentByName(hiscoresPanel, "combatLevelLabel") as? JLabel
            combatLevelLabel?.text = combatLevel.toString()
        }

        private fun calculateCombatLevel(
            attack: Int,
            defence: Int,
            strength: Int,
            hitpoints: Int,
            prayer: Int,
            ranged: Int,
            magic: Int,
            summoning: Int,
            isMemberWorld: Boolean
        ): Double {
            val base = (defence + hitpoints + floor(prayer.toDouble() / 2)) * 0.25
            val melee = (attack + strength) * 0.325
            val range = floor(ranged * 1.5) * 0.325
            val mage = floor(magic * 1.5) * 0.325
            val maxCombatType = maxOf(melee, range, mage)

            val summoningFactor = if (isMemberWorld) floor(summoning.toDouble() / 8) else 0.0
            return Math.round((base + maxCombatType + summoningFactor) * 1000.0) / 1000.0
        }

        private fun findComponentByName(container: Container, name: String): Component? {
            for (component in container.components) {
                if (name == component.name) {
                    return component
                }
                if (component is Container) {
                    val child = findComponentByName(component, name)
                    if (child != null) {
                        return child
                    }
                }
            }
            return null
        }
    }

    fun createHiscoreSearchView() {
        val hiscorePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            name = VIEW_NAME
            background = Constants.COLOR_BACKGROUND_MEDIUM
            preferredSize = Constants.HISCORE_PANEL_DIMENSION
            maximumSize = preferredSize
            minimumSize = preferredSize
        }

        val customSearchField = CustomSearchField(hiscorePanel)

        val searchFieldWrapper = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.X_AXIS)
            background = Constants.COLOR_BACKGROUND_MEDIUM
            preferredSize = Constants.SEARCH_FIELD_DIMENSION
            maximumSize = preferredSize
            minimumSize = preferredSize
            alignmentX = Component.CENTER_ALIGNMENT
            add(customSearchField)
        }

        val searchPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = Constants.COLOR_BACKGROUND_MEDIUM
            add(searchFieldWrapper)
        }

        hiscorePanel.add(Box.createVerticalStrut(10))
        hiscorePanel.add(searchPanel)
        hiscorePanel.add(Box.createVerticalStrut(10))

        val playerNamePanel = JPanel().apply {
            layout = GridBagLayout() // This will center the JLabel both vertically and horizontally
            background = TOOLTIP_BACKGROUND.darker()
            preferredSize = Constants.FILTER_PANEL_DIMENSION
            maximumSize = preferredSize
            minimumSize = preferredSize
            name = "playerNameLabel"
        }

        hiscorePanel.add(playerNamePanel)
        hiscorePanel.add(Box.createVerticalStrut(10))

        val skillsPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            background = Constants.COLOR_BACKGROUND_MEDIUM
            preferredSize = Constants.SKILLS_PANEL_DIMENSION
            maximumSize = preferredSize
            minimumSize = preferredSize
        }

        for (i in SKILL_DISPLAY_ORDER) {
            val skillPanel = JPanel().apply {
                layout = BorderLayout()
                background = COLOR_BACKGROUND_DARK
                preferredSize = Constants.SKILL_PANEL_DIMENSION
                maximumSize = preferredSize
                minimumSize = preferredSize
                border = MatteBorder(5, 0, 0, 0, COLOR_BACKGROUND_DARK)
            }

            val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(getSpriteId(i)))

            val imageCanvas = bufferedImageSprite.let {
                ImageCanvas(it).apply {
                    preferredSize = SKILL_SPRITE_DIMENSION
                    size = SKILL_SPRITE_DIMENSION
                    fillColor = COLOR_BACKGROUND_DARK
                }
            }

            val numberLabel = JLabel("", JLabel.RIGHT).apply {
                name = "skillLabel_$i"
                foreground = Constants.COLOR_FOREGROUND_LIGHT
                font = Constants.FONT_ARIAL_PLAIN_12
                preferredSize = Constants.NUMBER_LABEL_DIMENSION
                minimumSize = Constants.NUMBER_LABEL_DIMENSION
            }

            val imageContainer = JPanel(FlowLayout(FlowLayout.CENTER, 5, 0)).apply {
                background = COLOR_BACKGROUND_DARK
                add(imageCanvas)
                add(numberLabel)
            }

            skillPanel.add(imageContainer, BorderLayout.CENTER)
            skillsPanel.add(skillPanel)
        }

        hiscorePanel.add(skillsPanel)

        val totalCombatPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            background = COLOR_BACKGROUND_DARK
            preferredSize = Constants.TOTAL_COMBAT_PANEL_DIMENSION
            maximumSize = preferredSize
            minimumSize = preferredSize
        }

        val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(Constants.LVL_BAR_SPRITE))

        val totalLevelIcon = ImageCanvas(bufferedImageSprite).apply {
            fillColor = COLOR_BACKGROUND_DARK
            preferredSize = Constants.ICON_DIMENSION_LARGE
            size = Constants.ICON_DIMENSION_LARGE
        }

        val totalLevelLabel = JLabel("").apply {
            name = "totalLevelLabel"
            foreground = Constants.COLOR_FOREGROUND_LIGHT
            font = Constants.FONT_ARIAL_BOLD_12
            horizontalAlignment = JLabel.LEFT
            iconTextGap = 10
        }

        val totalLevelPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = Constants.COLOR_BACKGROUND_DARK
            add(totalLevelIcon)
            add(totalLevelLabel)
        }

        val bufferedImageSprite2 = getBufferedImageFromSprite(API.GetSprite(Constants.COMBAT_LVL_SPRITE))

        val combatLevelIcon = ImageCanvas(bufferedImageSprite2).apply {
            fillColor = COLOR_BACKGROUND_DARK
            preferredSize = Constants.ICON_DIMENSION_LARGE
            size = Constants.ICON_DIMENSION_LARGE
        }

        val combatLevelLabel = JLabel("").apply {
            name = "combatLevelLabel"
            foreground = Constants.COLOR_FOREGROUND_LIGHT
            font = Constants.FONT_ARIAL_BOLD_12
            horizontalAlignment = JLabel.LEFT
            iconTextGap = 10
        }

        val combatLevelPanel = JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            background = COLOR_BACKGROUND_DARK
            add(combatLevelIcon)
            add(combatLevelLabel)
        }

        totalCombatPanel.add(totalLevelPanel)
        totalCombatPanel.add(combatLevelPanel)
        hiscorePanel.add(totalCombatPanel)
        hiscorePanel.add(Box.createVerticalStrut(10))

        hiScoreView = hiscorePanel
    }

    data class HiscoresResponse(
        val info: PlayerInfo,
        val skills: List<Skill>
    )

    data class PlayerInfo(
        val exp_multiplier: String,
        val iron_mode: String
    )

    data class Skill(
        val id: String,
        val dynamic: String,
        val experience: String,
        val static: String
    )
}