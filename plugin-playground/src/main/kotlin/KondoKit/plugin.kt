package KondoKit

import KondoKit.Constants.COMBAT_LVL_SPRITE
import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.Helpers.formatNumber
import KondoKit.Helpers.getSpriteId
import KondoKit.Helpers.showAlert
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
import KondoKit.Themes.Theme
import KondoKit.Themes.ThemeType
import KondoKit.Themes.getTheme
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
import plugin.Plugin
import plugin.api.*
import plugin.api.API.*
import plugin.api.FontColor.fromColor
import rt4.*
import rt4.DisplayMode
import rt4.GameShell.canvas
import rt4.GameShell.frame
import rt4.client.js5Archive8
import rt4.client.mainLoadState
import java.awt.*
import java.awt.Font
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
        var BOOST = 1f

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

        @Exposed("Stretched/Scaled Fixed Mode Support")
        var useScaledFixed = false

        const val FIXED_WIDTH = 765
        const val FIXED_HEIGHT = 503
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
        private const val TICK_INTERVAL = 600L
        private var pluginsReloaded = false
        private var loginScreen = 160
        private var lastLogin = ""
        private var initialized = false
        private var lastClickTime = 0L
        private var lastUIOffset = 0
        private var themeName = "RUNELITE"
        private const val HIDDEN_VIEW = "HIDDEN"
        private var altCanvas: AltCanvas? = null
        private val drawActions = mutableListOf<() -> Unit>()

        fun registerDrawAction(action: () -> Unit) {
            synchronized(drawActions) {
                drawActions.add(action)
            }
        }
    }

    override fun Init() {
        // Disable Font AA
        System.setProperty("sun.java2d.opengl", "false")
        System.setProperty("awt.useSystemAAFontSettings", "off")
        System.setProperty("swing.aatext", "false")
    }

    override fun OnLogin() {
        if (lastLogin != "" && lastLogin != Player.usernameInput.toString()) {
            // if we logged in with a new character
            // we need to reset the trackers
            xpTrackerView?.let { resetXPTracker(it) }
        }
        lastLogin = Player.usernameInput.toString()
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

    override fun OnPluginsReloaded(): Boolean {
        if (!initialized) return true
        updateDisplaySettings()
        frame.remove(rightPanelWrapper)
        frame.layout = BorderLayout()
        rightPanelWrapper?.let { frame.add(it, BorderLayout.EAST) }
        frame.revalidate()
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

                if(focusedView == XPTrackerView.VIEW_NAME) {
                    xpTrackerView?.revalidate()
                    xpTrackerView?.repaint()
                }

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
        if (accumulatedTime >= TICK_INTERVAL) {
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

    override fun LateDraw(timeDelta: Long) {
        if (!initialized) return
        if(GameShell.fullScreenFrame != null) {
            DisplayMode.setWindowMode(true, 0, FIXED_WIDTH, FIXED_HEIGHT)
            showAlert("Fullscreen is not supported by KondoKit. Disable the plugin first.",
                "Error",
                JOptionPane.INFORMATION_MESSAGE
            )
            return
        }
        if(!useScaledFixed) return
        if(GetWindowMode() == WindowMode.FIXED){
            moveAltCanvasToFront()
        } else {
            moveCanvasToFront()
        }
        altCanvas?.updateGameImage() // Update the game image as needed
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

    private fun allSpritesLoaded() : Boolean {
        // Check all skill sprites
        try{
            for (i in 0 until 24) {
                if(!js5Archive8.isFileReady(getSpriteId(i))){
                    return false
                }
            }
            val otherIcons = arrayOf(LVL_ICON, MAG_SPRITE, LOOT_ICON, WRENCH_ICON, COMBAT_LVL_SPRITE, BAG_ICON)
            for (icon in otherIcons) {
                if(!js5Archive8.isFileReady(icon)){
                    return false
                }
            }
        } catch (e : Exception){
            return false
        }
        return true
    }

    private fun updateDisplaySettings() {
        val mode = GetWindowMode()
        val currentScrollPaneWidth = if (mainContentPanel.isVisible) NAVBAR_WIDTH + MAIN_CONTENT_WIDTH else NAVBAR_WIDTH
        lastUIOffset = uiOffset

        if(mode != WindowMode.FIXED) {
            destroyAltCanvas()
        } else if (useScaledFixed && altCanvas == null) {
            initAltCanvas()
        }

        when (mode) {
            WindowMode.FIXED -> {
                if (frame.width < FIXED_WIDTH + currentScrollPaneWidth + uiOffset) {
                    frame.setSize(FIXED_WIDTH + currentScrollPaneWidth + uiOffset, frame.height)
                }

                val difference = frame.width - (uiOffset + currentScrollPaneWidth)

                if (useScaledFixed) {
                    GameShell.leftMargin = 0
                    val canvasWidth = difference + uiOffset / 2
                    val canvasHeight = frame.height - canvas.y // Restricting height to frame height

                    altCanvas?.size = Dimension(canvasWidth, canvasHeight)
                    altCanvas?.setLocation(0, canvas.y)
                    canvas.setLocation(0, canvas.y)
                } else {
                    val difference = frame.width - (FIXED_WIDTH + uiOffset + currentScrollPaneWidth)
                    GameShell.leftMargin = difference / 2
                }
            }

            WindowMode.RESIZABLE -> {
                GameShell.canvasWidth = frame.width - (currentScrollPaneWidth + uiOffset)
            }
        }

        rightPanelWrapper?.preferredSize = Dimension(currentScrollPaneWidth, frame.height)
        rightPanelWrapper?.isDoubleBuffered = true
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
        StoreData("kondoScaledFixed", useScaledFixed)
        if(lastUIOffset != uiOffset){
            reloadInterfaces = true
        }
        updateDisplaySettings()
    }

    private fun initAltCanvas(){
        if(GetWindowMode() != WindowMode.FIXED || altCanvas != null) return
        if (frame != null) {
            altCanvas = AltCanvas().apply {
                preferredSize = Dimension(FIXED_WIDTH, FIXED_HEIGHT)
            }
            altCanvas?.let { frame.add(it) }
            moveAltCanvasToFront()
            frame.setComponentZOrder(rightPanelWrapper, 2)
        }
    }

    private fun destroyAltCanvas(){
        if (altCanvas == null) return
        moveCanvasToFront()
        frame.remove(altCanvas)
        altCanvas = null
    }

    private fun moveAltCanvasToFront(){
        if (altCanvas == null) return
        frame.setComponentZOrder(canvas, 2)
        frame.setComponentZOrder(altCanvas, 1)
        frame.setComponentZOrder(rightPanelWrapper, 0)
    }

    private fun moveCanvasToFront(){
        if (altCanvas == null) return
        frame.setComponentZOrder(altCanvas, 2)
        frame.setComponentZOrder(canvas, 1)
        frame.setComponentZOrder(rightPanelWrapper, 0)
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

    private fun restoreSettings(){
        themeName = (GetData("kondoTheme") as? String) ?: "RUNELITE"
        useLiveGEPrices = (GetData("kondoUseRemoteGE") as? Boolean) ?: true
        playerXPMultiplier = (GetData("kondoPlayerXPMultiplier") as? Int) ?: 5
        val osName = System.getProperty("os.name").toLowerCase()
        uiOffset = (GetData("kondoUIOffset") as? Int) ?: if (osName.contains("win")) 16 else 0
        launchMinimized = (GetData("kondoLaunchMinimized") as? Boolean) ?: false
        useScaledFixed = (GetData("kondoScaledFixed") as? Boolean) ?: false
    }

    private fun initKondoUI(){
        DrawText(FontType.LARGE, fromColor(Color(16777215)), TextModifier.CENTER, "KondoKit Loading Sprites...", GameShell.canvasWidth/2, GameShell.canvasHeight/2)
        if(!allSpritesLoaded()) return
        val frame: Frame? = GameShell.frame
        if (frame != null) {
            restoreSettings()
            theme = ThemeType.valueOf(themeName)
            applyTheme(getTheme(theme))
            appliedTheme = theme
            configureLookAndFeel()

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

            val rightPanel = Panel(BorderLayout()).apply {
                add(mainContentPanel, BorderLayout.CENTER)
                add(navPanel, BorderLayout.EAST)
            }

            rightPanelWrapper = JScrollPane(rightPanel).apply {
                preferredSize = Dimension(NAVBAR_WIDTH + MAIN_CONTENT_WIDTH, frame.height)
                background = VIEW_BACKGROUND_COLOR
                border = BorderFactory.createEmptyBorder()
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_NEVER
            }

            frame.layout = BorderLayout()
            rightPanelWrapper?.let {
                frame.add(it, BorderLayout.EAST)
            }

            if(launchMinimized){
                setActiveView(HIDDEN_VIEW)
            } else {
                setActiveView(XPTrackerView.VIEW_NAME)
            }
            initialized = true
            pluginsReloaded = true
        }
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
        updateDisplaySettings()

        // Revalidate and repaint necessary panels
        mainContentPanel.revalidate()
        rightPanelWrapper?.revalidate()
        frame?.revalidate()

        mainContentPanel.repaint()
        rightPanelWrapper?.repaint()
        frame?.repaint()

        focusedView = viewName
    }

    private fun createNavButton(spriteId: Int, viewName: String): JPanel {
        val bufferedImageSprite = getBufferedImageFromSprite(GetSprite(spriteId), NAV_TINT, NAV_GREYSCALE, BOOST)
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
            isOpaque = true

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

    private fun configureLookAndFeel(){
        loadFont()
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")

            // Modify the UI properties to match theme
            UIManager.put("control", VIEW_BACKGROUND_COLOR)
            UIManager.put("info", VIEW_BACKGROUND_COLOR)
            UIManager.put("nimbusBase", WIDGET_COLOR)
            UIManager.put("nimbusBlueGrey", TITLE_BAR_COLOR)

            UIManager.put("nimbusDisabledText", primaryColor)
            UIManager.put("nimbusSelectedText", secondaryColor)
            UIManager.put("text", secondaryColor)

            UIManager.put("nimbusFocus", TITLE_BAR_COLOR)
            UIManager.put("nimbusInfoBlue", POPUP_BACKGROUND)
            UIManager.put("nimbusLightBackground", WIDGET_COLOR)
            UIManager.put("nimbusSelectionBackground", PROGRESS_BAR_FILL)

            UIManager.put("Button.background", WIDGET_COLOR)
            UIManager.put("Button.foreground", secondaryColor)

            UIManager.put("CheckBox.background", VIEW_BACKGROUND_COLOR)
            UIManager.put("CheckBox.foreground", secondaryColor)
            UIManager.put("CheckBox.icon", UIManager.getIcon("CheckBox.icon"))

            UIManager.put("ComboBox.background", WIDGET_COLOR)
            UIManager.put("ComboBox.foreground", secondaryColor)
            UIManager.put("ComboBox.selectionBackground", PROGRESS_BAR_FILL)
            UIManager.put("ComboBox.selectionForeground", primaryColor)
            UIManager.put("ComboBox.buttonBackground", WIDGET_COLOR)

            UIManager.put("Spinner.background", WIDGET_COLOR)
            UIManager.put("Spinner.foreground", secondaryColor)
            UIManager.put("Spinner.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            UIManager.put("TextField.background", WIDGET_COLOR)
            UIManager.put("TextField.foreground", secondaryColor)
            UIManager.put("TextField.caretForeground", secondaryColor)
            UIManager.put("TextField.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            UIManager.put("ScrollBar.thumb", WIDGET_COLOR)
            UIManager.put("ScrollBar.track", VIEW_BACKGROUND_COLOR)
            UIManager.put("ScrollBar.thumbHighlight", TITLE_BAR_COLOR)

            UIManager.put("ProgressBar.foreground", PROGRESS_BAR_FILL)
            UIManager.put("ProgressBar.background", WIDGET_COLOR)
            UIManager.put("ProgressBar.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            UIManager.put("ToolTip.background", VIEW_BACKGROUND_COLOR)
            UIManager.put("ToolTip.foreground", secondaryColor)
            UIManager.put("ToolTip.border", BorderFactory.createLineBorder(TITLE_BAR_COLOR))

            // Update component tree UI to apply the new theme
            SwingUtilities.updateComponentTreeUI(frame)
            frame.background = Color.BLACK
        } catch (e : Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFont(): Font? {
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

    private fun applyTheme(theme: Theme) {
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
        BOOST = theme.boost
    }
}
