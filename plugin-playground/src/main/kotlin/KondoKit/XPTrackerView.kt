package KondoKit

import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.Helpers.formatNumber
import KondoKit.Helpers.getProgressBarColor
import KondoKit.Helpers.getSpriteId
import KondoKit.Helpers.addMouseListenerToAll
import KondoKit.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.plugin.Companion.IMAGE_SIZE
import KondoKit.plugin.Companion.LVL_ICON
import KondoKit.plugin.Companion.TOTAL_XP_WIDGET_SIZE
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.WIDGET_SIZE
import KondoKit.plugin.Companion.playerXPMultiplier
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import plugin.api.API
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.swing.*

object XPTrackerView {
    private val COMBAT_SKILLS = intArrayOf(0,1,2,3,4)
    val xpWidgets: MutableMap<Int, XPWidget> = HashMap()
    var totalXPWidget: XPWidget? = null
    val initialXP: MutableMap<Int, Int> = HashMap()
    var xpTrackerView: JPanel? = null


    val npcHitpointsMap: Map<Int, Int> = try {
        BufferedReader(InputStreamReader(plugin::class.java.getResourceAsStream("res/npc_hitpoints_map.json"), StandardCharsets.UTF_8))
            .useLines { lines ->
                val json = lines.joinToString("\n")
                val pairs = json.trim().removeSurrounding("{", "}").split(",")
                val map = mutableMapOf<Int, Int>()

                for (pair in pairs) {
                    val keyValue = pair.split(":")
                    val id = keyValue[0].trim().trim('\"').toIntOrNull()
                    val hitpoints = keyValue[1].trim()

                    if (id != null && hitpoints.isNotEmpty()) {
                        map[id] = hitpoints.toIntOrNull() ?: 0
                    }
                }

                map
            }
    } catch (e: Exception) {
        println("XPTracker Error parsing NPC HP: ${e.message}")
        emptyMap()
    }

    fun updateWidget(xpWidget: XPWidget, xp: Int) {
        val (currentLevel, xpGainedSinceLastLevel) = XPTable.getLevelForXp(xp)

        var xpGainedSinceLastUpdate = xp - xpWidget.previousXp
        xpWidget.totalXpGained += xpGainedSinceLastUpdate
        updateTotalXPWidget(xpGainedSinceLastUpdate)

        val progress: Double
        if (currentLevel >= 99) {
            progress = 100.0 // Set progress to 100% if the level is 99 or above
            xpWidget.xpLeftLabel.text = "" // Hide XP Left when level is 99
            xpWidget.actionsRemainingLabel.text = ""
        } else {
            val nextLevelXp = XPTable.getXpRequiredForLevel(currentLevel + 1)
            val xpLeft = nextLevelXp - xp
            progress = xpGainedSinceLastLevel.toDouble() / (nextLevelXp - XPTable.getXpRequiredForLevel(currentLevel)) * 100
            val xpLeftstr = formatNumber(xpLeft)
            xpWidget.xpLeftLabel.text = formatHtmlLabelText("XP Left: ", primaryColor, xpLeftstr, secondaryColor)
            if(COMBAT_SKILLS.contains(xpWidget.skillId)) {
                if(LootTrackerView.lastConfirmedKillNpcId != -1 && npcHitpointsMap.isNotEmpty()) {
                    val npcHP = npcHitpointsMap[LootTrackerView.lastConfirmedKillNpcId]
                    val xpPerKill = when (xpWidget.skillId) {
                        3 -> playerXPMultiplier * (npcHP ?: 1) // Hitpoints
                        else -> playerXPMultiplier * (npcHP ?: 1) * 4 // Combat XP for other skills
                    }
                    val remainingKills = xpLeft / xpPerKill
                    xpWidget.actionsRemainingLabel.text = formatHtmlLabelText("Kills: ", primaryColor, remainingKills.toString(), secondaryColor)
                }
            } else {
                if(xpGainedSinceLastUpdate == 0)
                    xpGainedSinceLastUpdate = 1 // Avoid possible divide by 0
                val remainingActions = (xpLeft / xpGainedSinceLastUpdate).coerceAtLeast(1)
                xpWidget.actionsRemainingLabel.text = formatHtmlLabelText("Actions: ", primaryColor, remainingActions.toString(), secondaryColor)
            }
        }

        val formattedXp = formatNumber(xpWidget.totalXpGained)
        xpWidget.xpGainedLabel.text = formatHtmlLabelText("XP Gained: ", primaryColor, formattedXp, secondaryColor)

        // Update the progress bar with current level, progress, and next level
        xpWidget.progressBar.updateProgress(progress, currentLevel, if (currentLevel < 99) currentLevel + 1 else 99, plugin.StateManager.focusedView == "XP_TRACKER_VIEW")

        xpWidget.previousXp = xp
        if (plugin.StateManager.focusedView == "XP_TRACKER_VIEW")
            xpWidget.container.repaint()
    }


    private fun updateTotalXPWidget(xpGainedSinceLastUpdate: Int) {
        val totalXPWidget = totalXPWidget ?: return
        totalXPWidget.totalXpGained += xpGainedSinceLastUpdate
        val formattedXp = formatNumber(totalXPWidget.totalXpGained)
        totalXPWidget.xpGainedLabel.text = formatHtmlLabelText("Gained: ", primaryColor, formattedXp, secondaryColor)
        if (plugin.StateManager.focusedView == "XP_TRACKER_VIEW")
            totalXPWidget.container.repaint()
    }


    fun resetXPTracker(xpTrackerView : JPanel){

        // Redo logic here
        xpTrackerView.removeAll()
        val popupMenu = createResetMenu()

        // Create a custom MouseListener
        val rightClickListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popupMenu.show(e.component, e.x, e.y)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popupMenu.show(e.component, e.x, e.y)
                }
            }
        }

        // Create the XP widget
        totalXPWidget = createTotalXPWidget()
        val wrapped = wrappedWidget(totalXPWidget!!.container)
        addMouseListenerToAll(wrapped,rightClickListener)
        wrapped.addMouseListener(rightClickListener)
        xpTrackerView.add(Box.createVerticalStrut(5))
        xpTrackerView.add(wrapped)
        xpTrackerView.add(Box.createVerticalStrut(5))

        initialXP.clear()
        xpWidgets.clear()

        xpTrackerView.revalidate()
        if (plugin.StateManager.focusedView == "XP_TRACKER_VIEW")
            xpTrackerView.repaint()
    }

     fun createTotalXPWidget(): XPWidget {
        val widgetPanel = Panel().apply {
            layout = BorderLayout(5, 5)
            background = WIDGET_COLOR
            preferredSize = TOTAL_XP_WIDGET_SIZE
            maximumSize = TOTAL_XP_WIDGET_SIZE
            minimumSize = TOTAL_XP_WIDGET_SIZE
        }

        val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(LVL_ICON))

        val imageContainer = Panel(FlowLayout()).apply {
            preferredSize = IMAGE_SIZE
            maximumSize = IMAGE_SIZE
            minimumSize = IMAGE_SIZE
            size = IMAGE_SIZE
        }

        bufferedImageSprite.let { image ->
            val imageCanvas = ImageCanvas(image).apply {
                preferredSize = Dimension(bufferedImageSprite.width, bufferedImageSprite.height)
                maximumSize = preferredSize
                minimumSize = preferredSize
                size = preferredSize
            }

            imageContainer.add(imageCanvas)
            imageContainer.size = Dimension(bufferedImageSprite.width, bufferedImageSprite.height)

            imageContainer.revalidate()
            if(plugin.StateManager.focusedView == "XP_TRACKER_VIEW")
                imageContainer.repaint()
        }

        val textPanel = Panel().apply {
            layout = GridLayout(2, 1, 5, 0)
        }

        val font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)

        val xpGainedLabel = JLabel(
            formatHtmlLabelText("Gained: ", primaryColor, "0", secondaryColor)
        ).apply {
            this.horizontalAlignment = JLabel.LEFT
            this.font = font
        }

        val xpPerHourLabel = JLabel(
            formatHtmlLabelText("XP /hr: ", primaryColor, "0", secondaryColor)
        ).apply {
            this.horizontalAlignment = JLabel.LEFT
            this.font = font
        }

        textPanel.add(xpGainedLabel)
        textPanel.add(xpPerHourLabel)

        widgetPanel.add(imageContainer, BorderLayout.WEST)
        widgetPanel.add(textPanel, BorderLayout.CENTER)

        return XPWidget(
            skillId = -1,
            container = widgetPanel,
            xpGainedLabel = xpGainedLabel,
            xpLeftLabel = JLabel(formatHtmlLabelText("XP Left: ", primaryColor, "0", secondaryColor)).apply {
                this.horizontalAlignment = JLabel.LEFT
                this.font = font
            },
            xpPerHourLabel = xpPerHourLabel,
            progressBar = ProgressBar(0.0, Color(150, 50, 50)),
            totalXpGained = 0,
            startTime = System.currentTimeMillis(),
            previousXp = 0,
            actionsRemainingLabel = JLabel(),
        )
    }


    fun createXPTrackerView(){
        val widgetViewPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = VIEW_BACKGROUND_COLOR
        }

        val popupMenu = createResetMenu()

        // Create a custom MouseListener
        val rightClickListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popupMenu.show(e.component, e.x, e.y)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    popupMenu.show(e.component, e.x, e.y)
                }
            }
        }

        // Create the XP widget
        totalXPWidget = createTotalXPWidget()
        val wrapped = wrappedWidget(totalXPWidget!!.container)
        addMouseListenerToAll(wrapped,rightClickListener)
        wrapped.addMouseListener(rightClickListener)
        widgetViewPanel.add(Box.createVerticalStrut(5))
        widgetViewPanel.add(wrapped)
        widgetViewPanel.add(Box.createVerticalStrut(5))

        xpTrackerView = widgetViewPanel
    }


    fun createResetMenu(): JPopupMenu {
        // Create a popup menu
        val popupMenu = JPopupMenu()

        val rFont = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)

        popupMenu.background = Color(45, 45, 45)

        // Create menu items with custom font and colors
        val menuItem1 = JMenuItem("Reset Tracker").apply {
            font = rFont  // Set custom font
            background = Color(45, 45, 45)  // Dark background for item
            foreground = Color(220, 220, 220) // Light text color for item
        }

        val menuItem2 = JMenuItem("Option 2").apply {
            font = rFont
            background = Color(45, 45, 45)
            foreground = Color(220, 220, 220)
        }

        val menuItem3 = JMenuItem("Option 3").apply {
            font = rFont
            background = Color(45, 45, 45)
            foreground = Color(220, 220, 220)
        }

        // Add menu items to the popup menu
        popupMenu.add(menuItem1)
        //popupMenu.add(menuItem2)
        //popupMenu.add(menuItem3)

        // Add action listeners to each menu item (optional)
        menuItem1.addActionListener { resetXPTracker(xpTrackerView!!) }
        //menuItem2.addActionListener { println("Option 2 selected") }
        //menuItem3.addActionListener { println("Option 3 selected") }

        return popupMenu
    }


    fun createXPWidget(skillId: Int, previousXp: Int): XPWidget {
        val widgetPanel = Panel().apply {
            layout = BorderLayout(5, 5)
            background = WIDGET_COLOR
            preferredSize = WIDGET_SIZE
            maximumSize = WIDGET_SIZE
            minimumSize = WIDGET_SIZE
        }

        val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(getSpriteId(skillId)))
        val imageContainer = Panel(FlowLayout()).apply {
            background = WIDGET_COLOR
            preferredSize = IMAGE_SIZE
            maximumSize = IMAGE_SIZE
            minimumSize = IMAGE_SIZE
            size = IMAGE_SIZE
        }

        bufferedImageSprite.let { image ->
            val imageCanvas = ImageCanvas(image).apply {
                background = WIDGET_COLOR
                preferredSize = Dimension(image.width, image.height)
                maximumSize = Dimension(image.width, image.height)
                minimumSize = Dimension(image.width, image.height)
                size = Dimension(image.width, image.height)  // Explicitly set the size
            }

            imageContainer.add(imageCanvas)
            imageContainer.size = Dimension(image.width, image.height) // Ensure container respects the image size

            imageContainer.revalidate()
            if(plugin.StateManager.focusedView == "XP_TRACKER_VIEW")
                imageContainer.repaint()
        }

        val textPanel = Panel().apply {
            layout = GridLayout(2, 2, 5, 0)
            background = WIDGET_COLOR
        }

        val font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)

        val xpGainedLabel = JLabel(
                formatHtmlLabelText("XP Gained: ", primaryColor, "0", secondaryColor)
        ).apply {
            this.horizontalAlignment = JLabel.LEFT
            this.font = font
        }

        val xpLeftLabel = JLabel(
            formatHtmlLabelText("XP Left: ", primaryColor, "0K", secondaryColor)
        ).apply {
            this.horizontalAlignment = JLabel.LEFT
            this.font = font
        }

        val xpPerHourLabel = JLabel(
            formatHtmlLabelText("XP /hr: ", primaryColor, "0", secondaryColor)
        ).apply {
            this.horizontalAlignment = JLabel.LEFT
            this.font = font
        }

        val killsLabel = JLabel(
            formatHtmlLabelText("Kills: ", primaryColor, "0", secondaryColor)
        ).apply {
            this.horizontalAlignment = JLabel.LEFT
            this.font = font
        }

        val levelPanel = Panel().apply {
            layout = BorderLayout(5, 0)
            background = Color(43, 43, 43)
        }

        val progressBarPanel = ProgressBar(0.0, getProgressBarColor(skillId)).apply {
            preferredSize = Dimension(160, 22)
        }

        levelPanel.add(progressBarPanel, BorderLayout.CENTER)

        textPanel.add(xpGainedLabel)
        textPanel.add(xpLeftLabel)
        textPanel.add(xpPerHourLabel)
        textPanel.add(killsLabel)

        widgetPanel.add(imageContainer, BorderLayout.WEST)
        widgetPanel.add(textPanel, BorderLayout.CENTER)
        widgetPanel.add(levelPanel, BorderLayout.SOUTH)

        widgetPanel.revalidate()
        if(plugin.StateManager.focusedView == "XP_TRACKER_VIEW")
            widgetPanel.repaint()

        return XPWidget(
            skillId = skillId,
            container = widgetPanel,
            xpGainedLabel = xpGainedLabel,
            xpLeftLabel = xpLeftLabel,
            xpPerHourLabel = xpPerHourLabel,
            progressBar = progressBarPanel,
            totalXpGained = 0,
            actionsRemainingLabel = killsLabel,
            startTime = System.currentTimeMillis(),
            previousXp = previousXp
        )
    }

    fun wrappedWidget(component: Component, padding: Int = 7): Container {
        val outerPanelSize = Dimension(
                component.preferredSize.width + 2 * padding,
                component.preferredSize.height + 2 * padding
        )
        val outerPanel = JPanel(GridBagLayout()).apply {
            background = WIDGET_COLOR
            preferredSize = outerPanelSize
            maximumSize = outerPanelSize
            minimumSize = outerPanelSize
        }
        val innerPanel = JPanel(BorderLayout()).apply {
            background = WIDGET_COLOR
            preferredSize = component.preferredSize
            maximumSize = component.preferredSize
            minimumSize = component.preferredSize
            add(component, BorderLayout.CENTER)
        }
        val gbc = GridBagConstraints().apply {
            anchor = GridBagConstraints.CENTER
        }
        outerPanel.add(innerPanel, gbc)
        return outerPanel
    }
}



data class XPWidget(
        val container: Container,
        val skillId: Int,
        val xpGainedLabel: JLabel,
        val xpLeftLabel: JLabel,
        val xpPerHourLabel: JLabel,
        val actionsRemainingLabel: JLabel,
        val progressBar: ProgressBar,
        var totalXpGained: Int = 0,
        var startTime: Long = System.currentTimeMillis(),
        var previousXp: Int = 0
)