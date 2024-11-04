package KondoKit

import KondoKit.Helpers.addMouseListenerToAll
import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.XPTrackerView.wrappedWidget
import KondoKit.plugin.Companion.POPUP_BACKGROUND
import KondoKit.plugin.Companion.POPUP_FOREGROUND
import KondoKit.plugin.Companion.TITLE_BAR_COLOR
import KondoKit.plugin.Companion.TOOLTIP_BACKGROUND
import KondoKit.plugin.Companion.TOTAL_XP_WIDGET_SIZE
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import KondoKit.plugin.StateManager.focusedView
import plugin.api.API
import rt4.*
import java.awt.*
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import javax.swing.*
import kotlin.math.ceil

object LootTrackerView {
    private const val SNAPSHOT_LIFESPAN = 10
    const val BAG_ICON = 900
    val npcDeathSnapshots = mutableMapOf<Int, GroundSnapshot>()
    var gePriceMap = loadGEPrices()
    const val VIEW_NAME = "LOOT_TRACKER_VIEW"
    private val lootItemPanels = mutableMapOf<String, MutableMap<Int, Int>>()
    private val npcKillCounts = mutableMapOf<String, Int>()
    private var totalTrackerWidget: XPWidget? = null
    var lastConfirmedKillNpcId = -1
    private var customToolTipWindow: JWindow? = null
    var lootTrackerView: JPanel? = null

     fun loadGEPrices(): Map<String, String> {
        return if (plugin.useLiveGEPrices) {
            try {
                println("LootTracker: Loading Remote GE Prices")
                val url = URL("https://cdn.2009scape.org/gedata/latest.json")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:89.0) Gecko/20100101 Firefox/89.0")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val content = inputStream.bufferedReader().use(BufferedReader::readText)

                    val items = content.trim().removeSurrounding("[", "]").split("},").map { it.trim() + "}" }
                    val gePrices = mutableMapOf<String, String>()

                    for (item in items) {
                        val pairs = item.removeSurrounding("{", "}").split(",")
                        val itemId = pairs.find { it.trim().startsWith("\"item_id\"") }?.split(":")?.get(1)?.trim()?.trim('\"')
                        val value = pairs.find { it.trim().startsWith("\"value\"") }?.split(":")?.get(1)?.trim()?.trim('\"')
                        if (itemId != null && value != null) {
                            gePrices[itemId] = value
                        }
                    }

                    gePrices
                } else {
                    emptyMap()
                }
            } catch (e: Exception) {
                emptyMap()
            }
        } else {
            try {
                println("LootTracker: Loading Local GE Prices")
                BufferedReader(InputStreamReader(plugin::class.java.getResourceAsStream("res/item_configs.json"), StandardCharsets.UTF_8))
                    .useLines { lines ->
                        val json = lines.joinToString("\n")
                        val items = json.trim().removeSurrounding("[", "]").split("},").map { it.trim() + "}" }
                        val gePrices = mutableMapOf<String, String>()

                        for (item in items) {
                            val pairs = item.removeSurrounding("{", "}").split(",")
                            val id = pairs.find { it.trim().startsWith("\"id\"") }?.split(":")?.get(1)?.trim()?.trim('\"')
                            val grandExchangePrice = pairs.find { it.trim().startsWith("\"grand_exchange_price\"") }?.split(":")?.get(1)?.trim()?.trim('\"')
                            if (id != null && grandExchangePrice != null) {
                                gePrices[id] = grandExchangePrice
                            }
                        }

                        gePrices
                    }
            } catch (e: Exception) {
                emptyMap()
            }
        }
    }



    fun createLootTrackerView() {
        lootTrackerView = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS) // Use BoxLayout on Y axis to stack widgets vertically
            background = VIEW_BACKGROUND_COLOR
            add(Box.createVerticalStrut(5))
            totalTrackerWidget = createTotalLootWidget()

            val wrapped = wrappedWidget(totalTrackerWidget!!.container)
            val popupMenu = resetLootTrackerMenu()

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
            addMouseListenerToAll(wrapped,rightClickListener)
            wrapped.addMouseListener(rightClickListener)
            add(wrapped)
            add(Box.createVerticalStrut(8))
            revalidate()
            if(focusedView == VIEW_NAME)
                repaint()
        }
    }

    private fun updateTotalKills() {
        totalTrackerWidget?.let {
            it.totalXpGained += 1
            it.xpGainedLabel.text = formatHtmlLabelText("Total Count: ", primaryColor, it.totalXpGained.toString(), secondaryColor)
            it.xpGainedLabel.repaint()
        }
    }

    private fun updateTotalValue(newVal: Int) {
        totalTrackerWidget?.let {
            it.previousXp += newVal
            it.xpPerHourLabel.text = formatHtmlLabelText("Total Value: ", primaryColor, formatValue(it.previousXp) + " gp", secondaryColor)
            it.container.repaint()
        }
    }

    private fun createTotalLootWidget(): XPWidget {
        val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(BAG_ICON))
        val l1 = createLabel(formatHtmlLabelText("Total Value: ", primaryColor, "0" + " gp", secondaryColor))
        val l2 = createLabel(formatHtmlLabelText("Total Count: ", primaryColor, "0", secondaryColor))
        return XPWidget(
            skillId = -1,
            container = createWidgetPanel(bufferedImageSprite,l2,l1),
            xpGainedLabel = l2,
            xpLeftLabel = JLabel(),
            actionsRemainingLabel = JLabel(),
            xpPerHourLabel = l1,
            progressBar = ProgressBar(0.0, Color(0,0,0)), // unused.
            totalXpGained = 0,
            startTime = System.currentTimeMillis(),
            previousXp = 0
        )
    }

    private fun createWidgetPanel(bufferedImageSprite: BufferedImage, l1 : JLabel, l2 : JLabel): Panel {
        val imageCanvas = ImageCanvas(bufferedImageSprite).apply {
            preferredSize = Dimension(bufferedImageSprite.width, bufferedImageSprite.height)
            minimumSize = preferredSize
            maximumSize = preferredSize
            size = preferredSize
            background = WIDGET_COLOR
        }

        val imageContainer = Panel(BorderLayout()).apply {
            background = WIDGET_COLOR
            add(imageCanvas, BorderLayout.NORTH)
        }

        return Panel(BorderLayout(5, 0)).apply {
            background = WIDGET_COLOR
            preferredSize = TOTAL_XP_WIDGET_SIZE
            add(imageContainer, BorderLayout.WEST)
            add(createTextPanel(l1,l2), BorderLayout.CENTER)
        }
    }

    private fun createTextPanel(l1 : JLabel, l2: JLabel): Panel {
        return Panel(GridLayout(2, 1, 5, 0)).apply {
            background = WIDGET_COLOR
            add(l1)
            add(l2)
        }
    }

    private fun createLabel(text: String): JLabel {
        return JLabel(text).apply {
            font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
            horizontalAlignment = JLabel.LEFT
        }
    }

    private fun addItemToLootPanel(lootTrackerView: JPanel, drop: Item, npcName: String) {
        findLootItemsPanel(lootTrackerView, npcName)?.let { lootPanel ->
            val itemQuantities = lootItemPanels.getOrPut(npcName) { mutableMapOf() }
            val newQuantity = itemQuantities.merge(drop.id, drop.quantity, Int::plus) ?: drop.quantity

            lootPanel.components.filterIsInstance<JPanel>().find { it.getClientProperty("itemId") == drop.id }
                ?.let { updateItemPanelIcon(it, drop.id, newQuantity) }
                ?: addNewItemToPanel(lootPanel, drop, newQuantity)

            // Recalculate lootPanel size based on the number of unique items.
            val totalItems = lootItemPanels[npcName]?.size ?: 0
            val rowsNeeded = ceil(totalItems / 6.0).toInt()
            val lootPanelHeight = rowsNeeded * (40)

            val size = Dimension(lootPanel.width,lootPanelHeight+32)
            lootPanel.parent.preferredSize = size
            lootPanel.parent.minimumSize = size
            lootPanel.parent.maximumSize = size
            lootPanel.parent.revalidate()
            lootPanel.parent.repaint()
            lootPanel.revalidate()

            if(focusedView == VIEW_NAME)
                lootPanel.repaint()
        }
    }

    private fun addNewItemToPanel(lootPanel: JPanel, drop: Item, newQuantity: Int) {
        val itemPanel = createItemPanel(drop.id, newQuantity)
        lootPanel.add(itemPanel)
    }

    private fun createItemPanel(itemId: Int, quantity: Int): JPanel {
        val bufferedImageSprite = getBufferedImageFromSprite(API.GetObjSprite(itemId, quantity, true, 1, 3153952))

        // Create the panel for the item
        val itemPanel = FixedSizePanel(Dimension(36, 32)).apply {
            preferredSize = Dimension(36, 32)
            background = WIDGET_COLOR
            minimumSize = preferredSize
            maximumSize = preferredSize

            val imageCanvas = ImageCanvas(bufferedImageSprite).apply {
                preferredSize = Dimension(36, 32)
                background = WIDGET_COLOR
                minimumSize = preferredSize
                maximumSize = preferredSize
            }

            // Add the imageCanvas to the panel
            add(imageCanvas, BorderLayout.CENTER)

            // Put the itemId as a property for reference
            putClientProperty("itemId", itemId)

            // Add mouse listener for custom hover text
            imageCanvas.addMouseListener(object : MouseAdapter() {
                override fun mouseEntered(e: MouseEvent) {
                    // Show custom tooltip when the mouse enters the component
                    showCustomToolTip(e.point, itemId,quantity,imageCanvas)
                }

                override fun mouseExited(e: MouseEvent) {
                    // Hide tooltip when mouse exits
                    hideCustomToolTip()
                }
            })
        }

        return itemPanel
    }

    // Function to show the custom tooltip
    fun showCustomToolTip(location: Point, itemId: Int, quantity: Int, parentComponent: ImageCanvas) {
        val itemDef = ObjTypeList.get(itemId)
        val gePricePerItem = gePriceMap[itemDef.id.toString()]?.toInt() ?: 0
        val totalGePrice = gePricePerItem * quantity
        val totalHaPrice = itemDef.cost * quantity
        val geText = if (quantity > 1) " (${formatValue(gePricePerItem)} ea)" else ""
        val haText = if (quantity > 1) " (${formatValue(itemDef.cost)} ea)" else ""
        val bgColor = Helpers.colorToHex(TOOLTIP_BACKGROUND)
        val textColor = Helpers.colorToHex(secondaryColor)
        val text = "<html><div style='color: "+textColor+"; background-color: "+bgColor+"; padding: 3px;'>" +
                "${itemDef.name} x $quantity<br>" +
                "GE: ${formatValue(totalGePrice)} ${geText}<br>" +
                "HA: ${formatValue(totalHaPrice)} ${haText}</div></html>"

        val _font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
        if (customToolTipWindow == null) {
            customToolTipWindow = JWindow().apply {
                contentPane = JLabel(text).apply {
                    border = BorderFactory.createLineBorder(Color.BLACK)
                    isOpaque = true
                    background = TOOLTIP_BACKGROUND
                    foreground = Color.WHITE
                    font = _font
                }
                pack()
            }
        }

        // Calculate the tooltip location relative to the parent component
        val screenLocation = parentComponent.locationOnScreen
        customToolTipWindow!!.setLocation(screenLocation.x + location.x, screenLocation.y + location.y + 20)
        customToolTipWindow!!.isVisible = true
    }

    // Function to hide the custom tooltip
    fun hideCustomToolTip() {
        customToolTipWindow?.isVisible = false
        customToolTipWindow = null  // Nullify the global instance
    }


    private fun updateItemPanelIcon(panel: JPanel, itemId: Int, quantity: Int) {
        panel.removeAll()
        panel.add(createItemPanel(itemId, quantity).components[0], BorderLayout.CENTER)
        panel.revalidate()
        if(focusedView == VIEW_NAME)
            panel.repaint()
    }

    private fun updateKillCountLabel(lootTrackerPanel: JPanel, npcName: String) {
        lootTrackerPanel.components.filterIsInstance<JPanel>().forEach { childFramePanel ->
            (childFramePanel.components.firstOrNull { it is JPanel } as? JPanel)?.components
                ?.filterIsInstance<JLabel>()?.find { it.name == "killCountLabel_$npcName" }
                ?.apply {
                    text = formatHtmlLabelText(npcName, secondaryColor, " x ${npcKillCounts[npcName]}", primaryColor)
                    revalidate()
                    repaint()
                }
        }
    }

    private fun updateValueLabel(lootTrackerPanel: JPanel, valueOfNewDrops: String, npcName: String) {
        lootTrackerPanel.components.filterIsInstance<JPanel>().forEach { childFramePanel ->
            (childFramePanel.components.firstOrNull { it is JPanel } as? JPanel)?.components
                ?.filterIsInstance<JLabel>()?.find { it.name == "valueLabel_$npcName" }
                ?.apply {
                    val newValue = (getClientProperty("val") as? Int ?: 0) + valueOfNewDrops.toInt()
                    text = "${formatValue(newValue)} gp"
                    foreground = primaryColor
                    putClientProperty("val", newValue)
                    revalidate()
                    if(focusedView == VIEW_NAME)
                        repaint()
                }
        }
    }

    private fun formatValue(value: Int): String {
        return when {
            value >= 1_000_000 -> "%.1fM".format(value / 1_000_000.0)
            value >= 10_000 -> "%.1fK".format(value / 1_000.0)
            else -> DecimalFormat("#,###").format(value)
        }
    }

    private fun findLootItemsPanel(container: Container, npcName: String): JPanel? {
        return container.components.filterIsInstance<JPanel>().firstOrNull { it.name == "LOOT_ITEMS_$npcName" }
            ?: container.components.filterIsInstance<Container>().mapNotNull { findLootItemsPanel(it, npcName) }.firstOrNull()
    }


    fun onPostClientTick(lootTrackerView: JPanel) {
        val toRemove = mutableListOf<Int>()

        npcDeathSnapshots.entries.forEach { (npcId, snapshot) ->
            val postDeathSnapshot = takeGroundSnapshot(Pair(snapshot.location.first, snapshot.location.second))
            val newDrops = postDeathSnapshot.subtract(snapshot.items)

            if (newDrops.isNotEmpty()) {
                val npcName = NpcTypeList.get(npcId).name
                lastConfirmedKillNpcId = npcId
                handleNewDrops(npcName.toString(), newDrops, lootTrackerView)
                toRemove.add(npcId)
            } else if (snapshot.age >= SNAPSHOT_LIFESPAN) {
                toRemove.add(npcId)
            } else {
                snapshot.age++
            }
        }

        toRemove.forEach { npcDeathSnapshots.remove(it) }
    }


    fun takeGroundSnapshot(location: Pair<Int, Int>): Set<Item> {
        return getGroundItemsAt(location.first, location.second).toSet()
    }

    private fun getGroundItemsAt(x: Int, z: Int): List<Item> {
        val objstacknodeLL = SceneGraph.objStacks[Player.plane][x][z] ?: return emptyList()
        val items = mutableListOf<Item>()
        var itemNode = objstacknodeLL.head() as ObjStackNode?
        while (itemNode != null) {
            items.add(Item(itemNode.value.type, itemNode.value.amount))
            itemNode = objstacknodeLL.next() as ObjStackNode?
        }
        return items
    }

    private fun handleNewDrops(npcName: String, newDrops: Set<Item>, lootTrackerView: JPanel) {
        findLootItemsPanel(lootTrackerView, npcName)?.let {
        } ?: run {
            lootTrackerView.add(createLootFrame(npcName))
            lootTrackerView.add(Box.createVerticalStrut(8))
            lootTrackerView.revalidate()
            if(focusedView == VIEW_NAME)
                lootTrackerView.repaint()
        }

        npcKillCounts[npcName] = npcKillCounts.getOrDefault(npcName, 0) + 1
        updateKillCountLabel(lootTrackerView, npcName)
        updateTotalKills()
        newDrops.forEach { drop ->
            val geValue = (gePriceMap[drop.id.toString()]?.toInt() ?: 0) * drop.quantity
            updateValueLabel(lootTrackerView, geValue.toString(), npcName)
            plugin.registerDrawAction {  addItemToLootPanel(lootTrackerView, drop, npcName) }
            updateTotalValue(geValue)
        }
    }

    private fun createLootFrame(npcName: String): JPanel {
        val childFramePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = WIDGET_COLOR
            minimumSize = Dimension(230, 0)
            maximumSize = Dimension(230, 700)
            name = "HELLO_WORLD"
        }

        val labelPanel = JPanel(BorderLayout()).apply {
            background = TITLE_BAR_COLOR
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            maximumSize = Dimension(230, 24)
            minimumSize = maximumSize
            preferredSize = maximumSize
        }

        val killCount = npcKillCounts.getOrPut(npcName) { 0 }
        val countLabel = JLabel(formatHtmlLabelText(npcName, secondaryColor, " x $killCount", primaryColor)).apply {
            foreground = secondaryColor
            font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
            horizontalAlignment = JLabel.LEFT
            name = "killCountLabel_$npcName"
        }

        val valueLabel = JLabel("0 gp").apply {
            foreground = secondaryColor
            font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
            horizontalAlignment = JLabel.RIGHT
            name = "valueLabel_$npcName"
        }

        labelPanel.add(countLabel, BorderLayout.WEST)
        labelPanel.add(valueLabel, BorderLayout.EAST)

        // Panel to hold loot items, using GridLayout to manage rows and columns.
        val lootPanel = JPanel().apply {
            background = WIDGET_COLOR
            border = BorderFactory.createLineBorder(WIDGET_COLOR, 5)
            name = "LOOT_ITEMS_$npcName"
            layout = GridLayout(0, 6, 1, 1) // 6 columns, rows will be added dynamically
        }

        lootItemPanels[npcName] = mutableMapOf()

        childFramePanel.add(labelPanel)
        childFramePanel.add(lootPanel)

        val popupMenu = removeLootFrameMenu(childFramePanel, npcName)

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

        labelPanel.addMouseListener(rightClickListener)
        return childFramePanel
    }

    private fun removeLootFrameMenu(toRemove: JPanel, npcName: String): JPopupMenu {
        // Create a popup menu
        val popupMenu = JPopupMenu()
        val rFont = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)

        popupMenu.background = POPUP_BACKGROUND

        // Create menu items with custom font and colors
        val menuItem1 = JMenuItem("Remove").apply {
            font = rFont  // Set custom font
            background = POPUP_BACKGROUND // Dark background for item
            foreground = POPUP_FOREGROUND // Light text color for item
        }
        popupMenu.add(menuItem1)
        menuItem1.addActionListener {
            lootItemPanels[npcName]?.clear()
            npcKillCounts[npcName] = 0
            lootTrackerView?.let { parent ->
                val components = parent.components
                val toRemoveIndex = components.indexOf(toRemove)
                if (toRemoveIndex >= 0 && toRemoveIndex < components.size - 1) {
                    val nextComponent = components[toRemoveIndex + 1]
                    if (nextComponent is Box.Filler) {
                        // Nasty way to remove the Box.createVerticalStrut(8) after
                        // the lootpanel.
                        parent.remove(nextComponent)
                    }
                }
                parent.remove(toRemove)
                parent.revalidate()
                parent.repaint()
            }
        }
        return popupMenu
    }


    private fun resetLootTrackerMenu(): JPopupMenu {
        // Create a popup menu
        val popupMenu = JPopupMenu()
        val rFont = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)

        popupMenu.background = POPUP_BACKGROUND

        // Create menu items with custom font and colors
        val menuItem1 = JMenuItem("Reset Loot Tracker").apply {
            font = rFont  // Set custom font
            background = POPUP_BACKGROUND  // Dark background for item
            foreground = POPUP_FOREGROUND // Light text color for item
        }
        popupMenu.add(menuItem1)
        menuItem1.addActionListener {
            plugin.registerDrawAction {
                resetLootTracker()
            }
        }
        return popupMenu
    }

    private fun resetLootTracker(){
        lootTrackerView?.removeAll()
        npcKillCounts.clear()
        lootItemPanels.clear()
        totalTrackerWidget = createTotalLootWidget()

        val wrapped = wrappedWidget(totalTrackerWidget!!.container)
        val _popupMenu = resetLootTrackerMenu()

        // Create a custom MouseListener
        val rightClickListener = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    _popupMenu.show(e.component, e.x, e.y)
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    _popupMenu.show(e.component, e.x, e.y)
                }
            }
        }
        addMouseListenerToAll(wrapped,rightClickListener)
        wrapped.addMouseListener(rightClickListener)
        lootTrackerView?.add(Box.createVerticalStrut(5))
        lootTrackerView?.add(wrapped)
        lootTrackerView?.add(Box.createVerticalStrut(8))
        lootTrackerView?.revalidate()
        lootTrackerView?.repaint()
    }

    class FixedSizePanel(private val fixedSize: Dimension) : JPanel() {
        override fun getPreferredSize(): Dimension {
            return fixedSize
        }

        override fun getMinimumSize(): Dimension {
            return fixedSize
        }

        override fun getMaximumSize(): Dimension {
            return fixedSize
        }
    }

    data class GroundSnapshot(val items: Set<Item>, val location: Pair<Int, Int>, var age: Int)
    data class Item(val id: Int, val quantity: Int)
}
