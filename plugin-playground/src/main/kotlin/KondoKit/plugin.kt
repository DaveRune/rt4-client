package KondoKit

import KondoKit.Constants.COMBAT_LVL_SPRITE
import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.Helpers.formatNumber
import KondoKit.Helpers.getSpriteId
import KondoKit.HiscoresView.createHiscoreSearchView
import KondoKit.LootTrackerView.BAG_ICON
import KondoKit.LootTrackerView.createLootTrackerView
import KondoKit.LootTrackerView.npcDeathSnapshots
import KondoKit.LootTrackerView.onPostClientTick
import KondoKit.LootTrackerView.takeGroundSnapshot
import KondoKit.ReflectiveEditorView.addPlugins
import KondoKit.ReflectiveEditorView.createReflectiveEditorView
import KondoKit.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.XPTrackerView.createTotalXPWidget
import KondoKit.XPTrackerView.createXPTrackerView
import KondoKit.XPTrackerView.createXPWidget
import KondoKit.XPTrackerView.updateWidget
import KondoKit.XPTrackerView.wrappedWidget
import KondoKit.plugin.StateManager.initialXP
import KondoKit.plugin.StateManager.totalXPWidget
import KondoKit.plugin.StateManager.xpWidgets
import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import plugin.api.API.*
import plugin.api.FontColor.fromColor
import rt4.GameShell
import rt4.GameShell.frame
import rt4.GlRenderer
import rt4.InterfaceList
import rt4.Player
import rt4.client.js5Archive8
import rt4.client.mainLoadState
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

@PluginMeta(
    author = "downthecrop",
    description = "A plugin that adds a right-side panel with custom widgets and navigation.",
    version = 1.0
)

class plugin : Plugin() {
    companion object {
        val WIDGET_SIZE = Dimension(270, 55)
        val TOTAL_XP_WIDGET_SIZE = Dimension(270, 30)
        val IMAGE_SIZE = Dimension(20, 20)
        val WIDGET_COLOR = Color(27, 27, 27)
        val VIEW_BACKGROUND_COLOR = Color(37, 37, 37)
        val primaryColor = Color(129, 129, 129) // Color for "XP Gained:"
        val secondaryColor = Color(226, 226, 226)   // Color for "0"
        var kondoExposed_useLiveGEPrices = true
        var kondoExposed_playerXPMultiplier = 5
        const val FIXED_WIDTH = 782
        const val SCROLLPANE_WIDTH = 340
        private const val WRENCH_ICON = 907
        private const val LVL_ICON = 898
        private const val LOOT_ICON = 777
        private const val MAG_SPRITE = 1423
        private lateinit var cardLayout: CardLayout
        private lateinit var mainContentPanel: Panel
        private var scrollPane: JScrollPane? = null
        private var hiScoreView: JPanel? = null
        private var reflectiveEditorView: JPanel? = null
        private var lootTrackerView: JPanel? = null
        private var xpTrackerView: JPanel? = null
        private var accumulatedTime = 0L
        private const val tickInterval = 600L
        private var pluginsReloaded = false
        private var loginScreen = 160;
        private var lastLogin = ""
        private var initialized = false;
    }

    fun allSpritesLoaded() : Boolean {
        // Check all skill sprites
        try{
            for (i in 0 until 24) {
                if(!js5Archive8.isFileReady(getSpriteId(i))){
                    return false;
                }
            }
            val otherIcons = arrayOf(LVL_ICON, MAG_SPRITE, LOOT_ICON, WRENCH_ICON, COMBAT_LVL_SPRITE, BAG_ICON);
            for (icon in otherIcons) {
                if(!js5Archive8.isFileReady(icon)){
                    return false;
                }
            }
        } catch (e : Exception){
            return false;
        }
        return true;
    }

    override fun OnLogin() {
        if (lastLogin != "" && lastLogin != Player.usernameInput.toString()) {
            // if we logged in with a new character
            // we need to reset the trackers
            xpTrackerView?.removeAll()
            totalXPWidget = createTotalXPWidget()
            xpTrackerView?.add(Box.createVerticalStrut(5))
            xpTrackerView?.add(wrappedWidget(totalXPWidget!!.panel))
            xpTrackerView?.add(Box.createVerticalStrut(5))
            initialXP.clear()
            xpWidgets.clear()

            xpTrackerView?.revalidate()
            if (StateManager.focusedView == "XP_TRACKER_VIEW")
                xpTrackerView?.repaint()
        }
        lastLogin = Player.usernameInput.toString()
    }

    private fun UpdateDisplaySettings() {
        val mode = GetWindowMode()
        when (mode) {
            WindowMode.FIXED -> {
                if (frame.width < FIXED_WIDTH + SCROLLPANE_WIDTH) {
                    frame.setSize(FIXED_WIDTH + SCROLLPANE_WIDTH, frame.height)
                }
                val difference = frame.width - (FIXED_WIDTH + SCROLLPANE_WIDTH)
                GameShell.leftMargin = difference / 2
            }
            WindowMode.RESIZABLE -> {
                GameShell.canvasWidth -= SCROLLPANE_WIDTH
            }
        }
        scrollPane?.revalidate()
        scrollPane?.repaint()
    }

    fun OnKondoValueUpdated(){
        StoreData("kondoUseRemoteGE", kondoExposed_useLiveGEPrices)
        StoreData("kondoPlayerXPMultiplier", kondoExposed_playerXPMultiplier)
        LootTrackerView.gePriceMap = LootTrackerView.loadGEPrices()
    }

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        if (currentEntries != null) {
            for ((index, entry) in currentEntries.withIndex()) {
                if (entry.type == MiniMenuType.PLAYER && index == currentEntries.size - 1) {
                    val input = entry.subject
                    val username = input
                        .replace(Regex("<col=[0-9a-fA-F]{6}>"), "")
                        .replace(Regex("<img=\\d+>"), "")
                        .split(" ") // Split by spaces
                        .first() // Take the first part, which is the username
                    InsertMiniMenuEntry("Lookup", entry.subject, searchHiscore(username.replace(" ","_")))
                }
            }
        }
    }

    private fun searchHiscore(username: String): Runnable {
        return Runnable {
            cardLayout.show(mainContentPanel, "HISCORE_SEARCH_VIEW")
            StateManager.focusedView = "HISCORE_SEARCH_VIEW"
            val customSearchField = hiScoreView?.let { HiscoresView.CustomSearchField(it) }

            customSearchField?.searchPlayer(username) ?: run {
                println("searchView is null or CustomSearchField creation failed.")
            }
            hiScoreView?.repaint()
        }
    }


    override fun OnPluginsReloaded(): Boolean {
        if (!initialized) return true

        UpdateDisplaySettings()

        frame.remove(scrollPane)
        frame.layout = BorderLayout()
        frame.add(scrollPane, BorderLayout.EAST)

        // Clear or regenerate the reflectiveEditorView
        reflectiveEditorView?.removeAll()
        reflectiveEditorView?.revalidate()
        if(StateManager.focusedView == "REFLECTIVE_EDITOR_VIEW")
            reflectiveEditorView?.repaint()

        frame.revalidate()
        frame.repaint()
        pluginsReloaded = true
        return true
    }


    override fun OnXPUpdate(skillId: Int, xp: Int) {
        if (!initialXP.containsKey(skillId)) {
            initialXP[skillId] = xp
            return
        }

        var xpWidget = xpWidgets[skillId]

        if (xpWidget != null) {
            updateWidget(xpWidget, xp)
        } else {
            val previousXp = initialXP[skillId] ?: xp
            if (xp == initialXP[skillId]) return

            xpWidget = createXPWidget(skillId, previousXp)
            xpWidgets[skillId] = xpWidget

            xpTrackerView?.add(wrappedWidget(xpWidget.panel))
            xpTrackerView?.add(Box.createVerticalStrut(5))

            xpTrackerView?.revalidate()
            if(StateManager.focusedView == "XP_TRACKER_VIEW")
                xpTrackerView?.repaint()

            updateWidget(xpWidget, xp)
        }
    }

    override fun Draw(timeDelta: Long) {
        if (GlRenderer.enabled && GlRenderer.canvasWidth != GameShell.canvasWidth) {
            GlRenderer.canvasWidth = GameShell.canvasWidth
            GlRenderer.setViewportBounds(0, 0, GameShell.canvasWidth, GameShell.canvasHeight)
        }

        if (pluginsReloaded) {
            InterfaceList.method3712(true) // Gets the resize working correctly
            reflectiveEditorView?.let { addPlugins(it) }
            pluginsReloaded = false
        }

        accumulatedTime += timeDelta
        if (accumulatedTime >= tickInterval) {
            lootTrackerView?.let { onPostClientTick(it) }
            accumulatedTime = 0L
        }


        // Init in the draw call so we know we are between glBegin and glEnd for HD
        if(!initialized && mainLoadState >= loginScreen) {
            initKondoUI()
        }

    }

    private fun initKondoUI(){
        DrawText(FontType.LARGE, fromColor(Color(16777215)), TextModifier.CENTER, "KondoKit Loading Sprites...", GameShell.canvasWidth/2, GameShell.canvasHeight/2)
        if(!allSpritesLoaded()) return;
        val frame: Frame? = GameShell.frame
        if (frame != null) {
            kondoExposed_useLiveGEPrices = (GetData("kondoUseRemoteGE") as? Boolean) ?: true
            kondoExposed_playerXPMultiplier = (GetData("kondoPlayerXPMultiplier") as? Int) ?: 5
            cardLayout = CardLayout()
            mainContentPanel = Panel(cardLayout)
            mainContentPanel.background = VIEW_BACKGROUND_COLOR

            xpTrackerView = createXPTrackerView()
            hiScoreView = createHiscoreSearchView()
            lootTrackerView = createLootTrackerView()
            reflectiveEditorView = createReflectiveEditorView()
            mainContentPanel.add(xpTrackerView, "XP_TRACKER_VIEW")
            mainContentPanel.add(hiScoreView, "HISCORE_SEARCH_VIEW")
            mainContentPanel.add(lootTrackerView, "LOOT_TRACKER_VIEW")
            mainContentPanel.add(reflectiveEditorView, "REFLECTIVE_EDITOR_VIEW")

            val navPanel = Panel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = WIDGET_COLOR
                preferredSize = Dimension(42, frame.height)
            }

            navPanel.add(createNavButton(LVL_ICON, "XP_TRACKER_VIEW"))
            navPanel.add(createNavButton(MAG_SPRITE, "HISCORE_SEARCH_VIEW"))
            navPanel.add(createNavButton(LOOT_ICON, "LOOT_TRACKER_VIEW"))
            navPanel.add(createNavButton(WRENCH_ICON, "REFLECTIVE_EDITOR_VIEW"))

            val rightPanel = Panel(BorderLayout()).apply {
                add(mainContentPanel, BorderLayout.CENTER)
                add(navPanel, BorderLayout.EAST)
            }

            scrollPane = JScrollPane(rightPanel).apply {
                preferredSize = Dimension(SCROLLPANE_WIDTH, frame.height)
                background = VIEW_BACKGROUND_COLOR
                border = BorderFactory.createEmptyBorder()  // Removes the border completely
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
            }

            frame.layout = BorderLayout()
            scrollPane?.let { frame.add(it, BorderLayout.EAST) }

            frame.revalidate()
            frame.repaint()

            StateManager.focusedView = "XP_TRACKER_VIEW"
            initialized = true
            pluginsReloaded = true
            UpdateDisplaySettings()
        }
    }

    override fun Update() {
        xpWidgets.values.forEach { xpWidget ->
            val elapsedTime = (System.currentTimeMillis() - xpWidget.startTime) / 1000.0 / 60.0 / 60.0
            val xpPerHour = if (elapsedTime > 0) (xpWidget.totalXpGained / elapsedTime).toInt() else 0
            val formattedXpPerHour = formatNumber(xpPerHour)
            xpWidget.xpPerHourLabel.text =
                formatHtmlLabelText("XP /hr: ", primaryColor, formattedXpPerHour, secondaryColor)
            xpWidget.panel.repaint()
        }

        totalXPWidget?.let { totalXPWidget ->
            val elapsedTime = (System.currentTimeMillis() - totalXPWidget.startTime) / 1000.0 / 60.0 / 60.0
            val totalXPPerHour = if (elapsedTime > 0) (totalXPWidget.totalXpGained / elapsedTime).toInt() else 0
            val formattedTotalXpPerHour = formatNumber(totalXPPerHour)
            totalXPWidget.xpPerHourLabel.text =
                formatHtmlLabelText("XP /hr: ", primaryColor, formattedTotalXpPerHour, secondaryColor)
            totalXPWidget.panel.repaint()
        }
    }


    override fun OnKillingBlowNPC(npcID: Int, x: Int, z: Int) {
        val preDeathSnapshot = takeGroundSnapshot(Pair(x,z))
        npcDeathSnapshots[npcID] = LootTrackerView.GroundSnapshot(preDeathSnapshot, Pair(x, z), 0)
    }


    private fun createNavButton(spriteId: Int, viewName: String): JButton {
        val bufferedImageSprite = getBufferedImageFromSprite(GetSprite(spriteId))
        val buttonSize = Dimension(42, 42)
        val imageSize = Dimension(bufferedImageSprite.width, bufferedImageSprite.height)

        val actionListener = ActionListener {
            cardLayout.show(mainContentPanel, viewName)
            StateManager.focusedView = viewName
        }

        val imageCanvas = ImageCanvas(bufferedImageSprite).apply {
            background = WIDGET_COLOR
            preferredSize = imageSize
            maximumSize = imageSize
            minimumSize = imageSize
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent?) {
                    actionListener.actionPerformed(null)
                }
            })
        }

        val button = JButton().apply {
            layout = GridBagLayout()
            preferredSize = buttonSize
            maximumSize = buttonSize
            minimumSize = buttonSize
            background = WIDGET_COLOR
            isFocusPainted = false
            isBorderPainted = false

            val gbc = GridBagConstraints().apply {
                anchor = GridBagConstraints.CENTER
            }

            add(imageCanvas, gbc)
            addActionListener(actionListener)
        }

        return button
    }

    object StateManager {
        val initialXP: MutableMap<Int, Int> = HashMap()
        val xpWidgets: MutableMap<Int, XPWidget> = HashMap()
        var totalXPWidget: XPWidget? = null
        var focusedView: String = ""
    }
}
