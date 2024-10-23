package KondoKit

import KondoKit.Constants.COMBAT_LVL_SPRITE
import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.Helpers.formatNumber
import KondoKit.Helpers.getSpriteId
import KondoKit.Helpers.showAlert
import KondoKit.Helpers.showToast
import KondoKit.HiscoresView.createHiscoreSearchView
import KondoKit.HiscoresView.hiScoreView
import KondoKit.LootTrackerView.BAG_ICON
import KondoKit.LootTrackerView.createLootTrackerView
import KondoKit.LootTrackerView.lootTrackerView
import KondoKit.LootTrackerView.npcDeathSnapshots
import KondoKit.LootTrackerView.onPostClientTick
import KondoKit.LootTrackerView.takeGroundSnapshot
import KondoKit.ReflectiveEditorView.addPlugins
import KondoKit.ReflectiveEditorView.createReflectiveEditorView
import KondoKit.ReflectiveEditorView.reflectiveEditorView
import KondoKit.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.XPTrackerView.createXPTrackerView
import KondoKit.XPTrackerView.createXPWidget
import KondoKit.XPTrackerView.initialXP
import KondoKit.XPTrackerView.resetXPTracker
import KondoKit.XPTrackerView.totalXPWidget
import KondoKit.XPTrackerView.updateWidget
import KondoKit.XPTrackerView.wrappedWidget
import KondoKit.XPTrackerView.xpTrackerView
import KondoKit.XPTrackerView.xpWidgets
import KondoKit.plugin.StateManager.focusedView
import com.sun.org.apache.xpath.internal.operations.Bool
import plugin.Plugin
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


@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class Exposed(val description: String = "")

class plugin : Plugin() {
    companion object {
        val WIDGET_SIZE = Dimension(220, 50)
        val TOTAL_XP_WIDGET_SIZE = Dimension(220, 30)
        val IMAGE_SIZE = Dimension(25, 23)

        // Default Theme Colors
        var WIDGET_COLOR = Color(30, 30, 30)
        var TITLE_BAR_COLOR = Color(21, 21, 21)
        var VIEW_BACKGROUND_COLOR = Color(40, 40, 40)
        var primaryColor = Color(165, 165, 165)   // Color for "XP Gained:"
        var secondaryColor = Color(255, 255, 255) // Color for "0"
        var POPUP_BACKGROUND = Color(45, 45, 45)
        var POPUP_FOREGROUND = Color(220, 220, 220)
        var TOOLTIP_BACKGROUND = Color(50,50,50)
        var SCROLL_BAR_COLOR = Color(64, 64, 64)
        var PROGRESS_BAR_FILL = Color(61, 56, 49)
        var NAV_TINT: Color? = null
        var NAV_GREYSCALE = false

        var appliedTheme = ThemeType.RUNELITE

        @Exposed("Theme colors for KondoKit, requires a relaunch to apply.")
        var theme = ThemeType.RUNELITE

        @Exposed("Default: true, Use Local JSON or the prices from the Live/Stable server API")
        var useLiveGEPrices = true

        @Exposed("Used to calculate Combat Actions until next level.")
        var playerXPMultiplier = 5

        @Exposed("Start minimized/collapsed by default")
        var launchMinimized = false

        @Exposed("Default 16 on Windows, 0 Linux/macOS. If Kondo is not " +
                "perfectly snapped to the edge of the game due to window chrome you can update this to fix it")
        var uiOffset = 0

        private const val FIXED_WIDTH = 765
        private const val NAVBAR_WIDTH = 30
        private const val MAIN_CONTENT_WIDTH = 242
        private const val WRENCH_ICON = 907
        private const val LOOT_ICON = 777
        private const val MAG_SPRITE = 1423
        const val LVL_ICON = 898
        private lateinit var cardLayout: CardLayout
        private lateinit var mainContentPanel: JPanel
        private var rightPanelWrapper: JScrollPane? = null
        private var accumulatedTime = 0L
        private var reloadInterfaces = false
        private const val tickInterval = 600L
        private var pluginsReloaded = false
        private var loginScreen = 160
        private var lastLogin = ""
        private var initialized = false;
        private var lastClickTime = 0L
        private var lastUIOffset = 0
        private const val HIDDEN_VIEW = "HIDDEN";
        private val drawActions = mutableListOf<() -> Unit>()

        fun registerDrawAction(action: () -> Unit) {
            synchronized(drawActions) {
                drawActions.add(action)
            }
        }
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
            xpTrackerView?.let { resetXPTracker(it) }
        }
        lastLogin = Player.usernameInput.toString()
    }

    override fun Init() {
        // Disable Font AA
        System.setProperty("sun.java2d.opengl", "false");
        System.setProperty("awt.useSystemAAFontSettings", "off");
        System.setProperty("swing.aatext", "false");
    }

    private fun UpdateDisplaySettings() {
        val mode = GetWindowMode()
        val currentScrollPaneWidth = if (mainContentPanel.isVisible) NAVBAR_WIDTH + MAIN_CONTENT_WIDTH else NAVBAR_WIDTH
        lastUIOffset = uiOffset
        when (mode) {
            WindowMode.FIXED -> {
                if (frame.width < FIXED_WIDTH + currentScrollPaneWidth + uiOffset) {
                    frame.setSize(FIXED_WIDTH + currentScrollPaneWidth + uiOffset, frame.height)
                }

                val difference = frame.width - (FIXED_WIDTH + uiOffset + currentScrollPaneWidth)
                GameShell.leftMargin = difference / 2
            }
            WindowMode.RESIZABLE -> {
                GameShell.canvasWidth = frame.width - (currentScrollPaneWidth + uiOffset)
            }
        }
        rightPanelWrapper?.preferredSize = Dimension(currentScrollPaneWidth, frame.height)
        rightPanelWrapper?.revalidate()
        rightPanelWrapper?.repaint()
    }

    fun OnKondoValueUpdated(){
        StoreData("kondoUseRemoteGE", useLiveGEPrices)
        StoreData("kondoTheme", theme.toString())
        if(appliedTheme != theme) {
            showAlert(
                "KondoKit Theme changes require a relaunch.",
                "KondoKit",
                JOptionPane.INFORMATION_MESSAGE
            )
        }
        StoreData("kondoPlayerXPMultiplier", playerXPMultiplier)
        LootTrackerView.gePriceMap = LootTrackerView.loadGEPrices()
        StoreData("kondoLaunchMinimized", launchMinimized)
        StoreData("kondoUIOffset", uiOffset)
        if(lastUIOffset != uiOffset){
            UpdateDisplaySettings()
            reloadInterfaces = true
        }
    }

    override fun OnMiniMenuCreate(currentEntries: Array<out MiniMenuEntry>?) {
        if (currentEntries != null) {
            for ((index, entry) in currentEntries.withIndex()) {
                if (entry.type == MiniMenuType.PLAYER && index == currentEntries.size - 1) {
                    val input = entry.subject
                    // Trim spaces, clean up tags, and remove the level info
                    val cleanedInput = input
                            .trim() // Remove any leading/trailing spaces
                            .replace(Regex("<col=[0-9a-fA-F]{6}>"), "") // Remove color tags
                            .replace(Regex("<img=\\d+>"), "") // Remove image tags
                            .replace(Regex("\\(level: \\d+\\)"), "") // Remove level text e.g. (level: 44)
                            .trim() // Trim again to remove extra spaces after removing level text

                    // Proceed with the full cleaned username
                    InsertMiniMenuEntry("Lookup", entry.subject, searchHiscore(cleanedInput))
                }
            }
        }
    }

    private fun searchHiscore(username: String): Runnable {
        return Runnable {
            setActiveView(HiscoresView.VIEW_NAME)
            val customSearchField = hiScoreView?.let { HiscoresView.CustomSearchField(it) }

            customSearchField?.searchPlayer(username) ?: run {
                println("searchView is null or CustomSearchField creation failed.")
            }
        }
    }

    override fun OnPluginsReloaded(): Boolean {
        if (!initialized) return true

        UpdateDisplaySettings()

        frame.remove(rightPanelWrapper)
        frame.layout = BorderLayout()
        frame.add(rightPanelWrapper, BorderLayout.EAST)

        frame.revalidate()
        frame.repaint()
        pluginsReloaded = true
        reloadInterfaces = true
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

            xpTrackerView?.add(wrappedWidget(xpWidget.container))
            xpTrackerView?.add(Box.createVerticalStrut(5))

            xpTrackerView?.revalidate()
            if(focusedView == XPTrackerView.VIEW_NAME)
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
            reflectiveEditorView?.let { addPlugins(it) }
            pluginsReloaded = false
        }

        if (reloadInterfaces){
            InterfaceList.method3712(true) // Gets the resize working correctly
            reloadInterfaces = false
        }

        accumulatedTime += timeDelta
        if (accumulatedTime >= tickInterval) {
            lootTrackerView?.let { onPostClientTick(it) }
            accumulatedTime = 0L
        }

        // Draw synced actions (that require to be done between glBegin and glEnd)
        if (drawActions.isNotEmpty()) {
            synchronized(drawActions) {
                val actionsCopy = drawActions.toList()
                drawActions.clear()
                for (action in actionsCopy) {
                    action()
                }
            }
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

            loadFont()
            val themeIndex = (GetData("kondoTheme") as? String) ?: "RUNELITE"
            theme = ThemeType.valueOf(themeIndex)
            applyTheme(getTheme(theme))
            appliedTheme = theme

            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")

                // Modify the UI properties for a dark theme
                UIManager.put("control", Color(50, 50, 50)) // Default background for most controls
                UIManager.put("info", Color(50, 50, 50))
                UIManager.put("nimbusBase", Color(35, 35, 35)) // Base color for Nimbus L&F
                UIManager.put("nimbusAlertYellow", Color(255, 220, 35))
                UIManager.put("nimbusDisabledText", Color(100, 100, 100))
                UIManager.put("nimbusFocus", Color(115, 164, 209))
                UIManager.put("nimbusGreen", Color(176, 179, 50))
                UIManager.put("nimbusInfoBlue", Color(66, 139, 221))
                UIManager.put("nimbusLightBackground", Color(35, 35, 35)) // Background of text fields, etc.
                UIManager.put("nimbusOrange", Color(191, 98, 4))
                UIManager.put("nimbusRed", Color(169, 46, 34))
                UIManager.put("nimbusSelectedText", Color(255, 255, 255))
                UIManager.put("nimbusSelectionBackground", Color(75, 110, 175)) // Selection background
                UIManager.put("text", Color(230, 230, 230)) // General text color

                // Update component tree UI to apply the new theme
                SwingUtilities.updateComponentTreeUI(GameShell.frame)
            } catch (e : Exception) {
                e.printStackTrace()
            }

            // Restore saved values
            useLiveGEPrices = (GetData("kondoUseRemoteGE") as? Boolean) ?: true
            playerXPMultiplier = (GetData("kondoPlayerXPMultiplier") as? Int) ?: 5
            val osName = System.getProperty("os.name").toLowerCase()
            uiOffset = (GetData("kondoUIOffset") as? Int) ?: if (osName.contains("win")) 16 else 0
            launchMinimized = (GetData("kondoLaunchMinimized") as? Boolean) ?: false

            cardLayout = CardLayout()
            mainContentPanel = JPanel(cardLayout).apply {
                border = BorderFactory.createEmptyBorder(0, 0, 0, 0)  // Removes any default border or padding
                background = VIEW_BACKGROUND_COLOR
                preferredSize = Dimension(MAIN_CONTENT_WIDTH, frame.height)
                isOpaque = true
            }

            // Register Views
            createXPTrackerView()
            createHiscoreSearchView()
            createLootTrackerView()
            createReflectiveEditorView()

            mainContentPanel.add(ScrollablePanel(xpTrackerView!!), XPTrackerView.VIEW_NAME)
            mainContentPanel.add(ScrollablePanel(hiScoreView!!), HiscoresView.VIEW_NAME)
            mainContentPanel.add(ScrollablePanel(lootTrackerView!!), LootTrackerView.VIEW_NAME)
            mainContentPanel.add(ScrollablePanel(reflectiveEditorView!!), ReflectiveEditorView.VIEW_NAME)

            val navPanel = Panel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                background = WIDGET_COLOR
                preferredSize = Dimension(NAVBAR_WIDTH, frame.height)
            }

            navPanel.add(createNavButton(LVL_ICON, XPTrackerView.VIEW_NAME))
            navPanel.add(createNavButton(MAG_SPRITE, HiscoresView.VIEW_NAME))
            navPanel.add(createNavButton(LOOT_ICON, LootTrackerView.VIEW_NAME))
            navPanel.add(createNavButton(WRENCH_ICON, ReflectiveEditorView.VIEW_NAME))

            var rightPanel = Panel(BorderLayout()).apply {
                add(mainContentPanel, BorderLayout.CENTER)
                add(navPanel, BorderLayout.EAST)
            }

            rightPanelWrapper = JScrollPane(rightPanel).apply {
                preferredSize = Dimension(NAVBAR_WIDTH + MAIN_CONTENT_WIDTH, frame.height)
                background = VIEW_BACKGROUND_COLOR
                border = BorderFactory.createEmptyBorder()  // Removes the border completely
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
            }

            frame.layout = BorderLayout()
            rightPanelWrapper?.let { frame.add(it, BorderLayout.EAST) }

            if(!launchMinimized){
                setActiveView(XPTrackerView.VIEW_NAME)
            } else {
                setActiveView(HIDDEN_VIEW)
            }
            initialized = true
            pluginsReloaded = true
        }
    }

    override fun Update() {

        val widgets = xpWidgets.values
        val totalXP = totalXPWidget

        widgets.forEach { xpWidget ->
            val elapsedTime = (System.currentTimeMillis() - xpWidget.startTime) / 1000.0 / 60.0 / 60.0
            val xpPerHour = if (elapsedTime > 0) (xpWidget.totalXpGained / elapsedTime).toInt() else 0
            val formattedXpPerHour = formatNumber(xpPerHour)
            xpWidget.xpPerHourLabel.text =
                    formatHtmlLabelText("XP /hr: ", primaryColor, formattedXpPerHour, secondaryColor)
            xpWidget.container.repaint()
        }

        totalXP?.let { totalXPWidget ->
            val elapsedTime = (System.currentTimeMillis() - totalXPWidget.startTime) / 1000.0 / 60.0 / 60.0
            val totalXPPerHour = if (elapsedTime > 0) (totalXPWidget.totalXpGained / elapsedTime).toInt() else 0
            val formattedTotalXpPerHour = formatNumber(totalXPPerHour)
            totalXPWidget.xpPerHourLabel.text =
                    formatHtmlLabelText("XP /hr: ", primaryColor, formattedTotalXpPerHour, secondaryColor)
            totalXPWidget.container.repaint()
        }
    }

    override fun OnKillingBlowNPC(npcID: Int, x: Int, z: Int) {
        val preDeathSnapshot = takeGroundSnapshot(Pair(x,z))
        npcDeathSnapshots[npcID] = LootTrackerView.GroundSnapshot(preDeathSnapshot, Pair(x, z), 0)
    }

    private fun setActiveView(viewName: String) {
        // Handle the visibility of the main content panel
        if (viewName == HIDDEN_VIEW) {
            mainContentPanel.isVisible = false
        } else {
            if (!mainContentPanel.isVisible) {
                mainContentPanel.isVisible = true
            }
            cardLayout.show(mainContentPanel, viewName)
        }

        reloadInterfaces = true
        UpdateDisplaySettings()

        // Revalidate and repaint necessary panels
        mainContentPanel.revalidate()
        mainContentPanel.repaint()
        rightPanelWrapper?.revalidate()
        rightPanelWrapper?.repaint()
        frame?.revalidate()
        frame?.repaint()

        focusedView = viewName
    }

    private fun createNavButton(spriteId: Int, viewName: String): JPanel {
        val bufferedImageSprite = getBufferedImageFromSprite(GetSprite(spriteId), NAV_TINT, NAV_GREYSCALE)
        val buttonSize = Dimension(NAVBAR_WIDTH, 32)
        val imageSize = Dimension((bufferedImageSprite.width / 1.2f).toInt(), (bufferedImageSprite.height / 1.2f).toInt())
        val cooldownDuration = 100L

        val actionListener = ActionListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < cooldownDuration) {
                return@ActionListener
            }
            lastClickTime = currentTime

            if (focusedView == viewName) {
                setActiveView("HIDDEN")
            } else {
                setActiveView(viewName)
            }
        }

        // ImageCanvas with forced size
        val imageCanvas = ImageCanvas(bufferedImageSprite).apply {
            background = WIDGET_COLOR
            preferredSize = imageSize
            maximumSize = imageSize
            minimumSize = imageSize
        }

        // Wrapping the ImageCanvas in another JPanel to prevent stretching
        val imageCanvasWrapper = JPanel().apply {
            layout = GridBagLayout() // Keeps the layout of the wrapped panel minimal
            preferredSize = imageSize
            maximumSize = imageSize
            minimumSize = imageSize
            isOpaque = false // No background for the wrapper
            add(imageCanvas) // Adding ImageCanvas directly, layout won't stretch it
        }

        val panelButton = JPanel().apply {
            layout = GridBagLayout()
            preferredSize = buttonSize
            maximumSize = buttonSize
            minimumSize = buttonSize
            background = WIDGET_COLOR
            isOpaque = true // Ensure background is painted

            val gbc = GridBagConstraints().apply {
                anchor = GridBagConstraints.CENTER
                fill = GridBagConstraints.NONE // Prevents stretching
            }

            add(imageCanvasWrapper, gbc)

            // Hover and click behavior
            val hoverListener = object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent?) {
                    background = WIDGET_COLOR.darker()
                    imageCanvas.fillColor = WIDGET_COLOR.darker()
                    imageCanvas.repaint()
                    repaint()
                }

                override fun mouseExited(e: MouseEvent?) {
                    background = WIDGET_COLOR
                    imageCanvas.fillColor = WIDGET_COLOR
                    imageCanvas.repaint()
                    repaint()
                }

                override fun mouseClicked(e: MouseEvent?) {
                    actionListener.actionPerformed(null)
                }
            }

            addMouseListener(hoverListener)
            imageCanvas.addMouseListener(hoverListener)
        }

        return panelButton
    }


    fun loadFont(): Font? {
        val fontStream = plugin::class.java.getResourceAsStream("res/runescape_small.ttf")
        return if (fontStream != null) {
            try {
                val font = Font.createFont(Font.TRUETYPE_FONT, fontStream)
                val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
                ge.registerFont(font) // Register the font in the graphics environment
                font
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            println("Font not found!")
            null
        }
    }

    object StateManager {
        var focusedView: String = ""
    }

    fun applyTheme(theme: Theme) {
        WIDGET_COLOR = theme.widgetColor
        TITLE_BAR_COLOR = theme.titleBarColor
        VIEW_BACKGROUND_COLOR = theme.viewBackgroundColor
        primaryColor = theme.primaryColor
        secondaryColor = theme.secondaryColor
        POPUP_BACKGROUND = theme.popupBackground
        POPUP_FOREGROUND = theme.popupForeground
        TOOLTIP_BACKGROUND = theme.tooltipBackground
        SCROLL_BAR_COLOR = theme.scrollBarColor
        PROGRESS_BAR_FILL = theme.progressBarFill
        NAV_TINT = theme.navTint
        NAV_GREYSCALE = theme.navGreyScale
    }

    enum class ThemeType {
        RUNELITE,
        DARKER_RUNELITE,
        SARADOMIN,
        ORION,
    }

    fun getTheme(themeType: ThemeType): Theme {
        return when (themeType) {
            ThemeType.RUNELITE -> Theme(
                widgetColor = Color(30, 30, 30),
                titleBarColor = Color(21, 21, 21),
                viewBackgroundColor = Color(40, 40, 40),
                primaryColor = Color(165, 165, 165),
                secondaryColor = Color(255, 255, 255),
                popupBackground = Color(45, 45, 45),
                popupForeground = Color(220, 220, 220),
                tooltipBackground = Color(50, 50, 50),
                scrollBarColor = Color(64, 64, 64),
                progressBarFill = Color(61, 56, 49),
                navTint = null,
                navGreyScale = false
            )
            ThemeType.DARKER_RUNELITE -> Theme(
                widgetColor = Color(15, 15, 15),           // Darker widget backgrounds
                titleBarColor = Color(10, 10, 10),         // Even darker title bar
                viewBackgroundColor = Color(20, 20, 20),   // Very dark background for the view
                primaryColor = Color(140, 140, 140),       // Darker shade for primary text
                secondaryColor = Color(210, 210, 210),     // Slightly muted white for secondary text
                popupBackground = Color(25, 25, 25),       // Very dark popup backgrounds
                popupForeground = Color(200, 200, 200),    // Slightly muted light gray for popup text
                tooltipBackground = Color(30, 30, 30),     // Darker tooltips background
                scrollBarColor = Color(40, 40, 40),        // Darker scroll bar
                progressBarFill = Color(45, 40, 35),       // Darker progress bar fill
                navTint = null, //Color(20, 20, 20, 60),                            // No specific tint applied
                navGreyScale = false                       // Navigation retains color (if applicable)
            )
            ThemeType.ORION -> Theme(
                widgetColor = Color(50, 50, 50),           // Darker gray for widget backgrounds
                titleBarColor = Color(35, 35, 35),         // Very dark gray for the title bar
                viewBackgroundColor = Color(60, 60, 60),   // Medium-dark gray for view background
                primaryColor = Color(180, 180, 180),       // Lighter gray for primary text or highlights
                secondaryColor = Color(210, 210, 210),     // Light gray for secondary text
                popupBackground = Color(45, 45, 45),       // Dark gray for popup backgrounds
                popupForeground = Color(230, 230, 230),    // Light gray for popup text
                tooltipBackground = Color(55, 55, 55),     // Slightly darker gray for tooltips
                scrollBarColor = Color(75, 75, 75),        // Dark gray for scroll bars
                progressBarFill = Color(100, 100, 100),    // Medium gray for progress bar fill
                navTint = null,
                navGreyScale = true
            )
            ThemeType.SARADOMIN -> Theme(
                widgetColor = Color(75, 60, 45),
                titleBarColor = Color(60, 48, 36),
                viewBackgroundColor = Color(95, 76, 58),
                primaryColor = Color(180, 150, 120),
                secondaryColor = Color(230, 210, 190),
                popupBackground = Color(70, 56, 42),
                popupForeground = Color(220, 200, 180),
                tooltipBackground = Color(80, 65, 50),
                scrollBarColor = Color(100, 85, 70),
                progressBarFill = Color(130, 110, 90),
                navTint = null,
                navGreyScale = false
            )
        }
    }


    data class Theme(
            val widgetColor: Color,
            val titleBarColor: Color,
            val viewBackgroundColor: Color,
            val primaryColor: Color,
            val secondaryColor: Color,
            val popupBackground: Color,
            val popupForeground: Color,
            val tooltipBackground: Color,
            val scrollBarColor: Color,
            val progressBarFill: Color,
            val navTint: Color?,
            val navGreyScale: Boolean
    )
}
