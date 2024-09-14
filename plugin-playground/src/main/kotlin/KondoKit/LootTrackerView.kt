package KondoKit

import KondoKit.Helpers.formatHtmlLabelText
import KondoKit.SpriteToBufferedImage.getBufferedImageFromSprite
import KondoKit.XPTrackerView.wrappedWidget
import KondoKit.plugin.Companion.TOTAL_XP_WIDGET_SIZE
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import plugin.api.API
import rt4.NpcTypeList
import rt4.ObjStackNode
import rt4.Player
import rt4.SceneGraph
import java.awt.*
import java.awt.image.BufferedImage
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import javax.swing.*

object LootTrackerView {
    private const val SNAPSHOT_LIFESPAN = 10
    const val BAG_ICON = 900;
    val npcDeathSnapshots = mutableMapOf<Int, GroundSnapshot>()
    var gePriceMap = loadGEPrices()
    private val lootItemPanels = mutableMapOf<String, MutableMap<Int, Int>>()
    private val npcKillCounts = mutableMapOf<String, Int>()
    private var totalTrackerWidget: XPWidget? = null
    var lastConfirmedKillNpcId = -1;

     fun loadGEPrices(): Map<String, String> {
        return if (plugin.kondoExposed_useLiveGEPrices) {
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
                BufferedReader(InputStreamReader(plugin::class.java.getResourceAsStream("item_configs.json"), StandardCharsets.UTF_8))
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



    fun createLootTrackerView(): JPanel {
        return JPanel().apply {
            layout = FlowLayout(FlowLayout.CENTER, 0, 5)
            background = VIEW_BACKGROUND_COLOR
            preferredSize = Dimension(270, 700)
            maximumSize = Dimension(270, 700)
            minimumSize = Dimension(270, 700)
            add(Box.createVerticalStrut(5))
            totalTrackerWidget = createTotalLootWidget()
            add(wrappedWidget(totalTrackerWidget!!.panel))
            add(Helpers.Spacer(height = 15))
            revalidate()
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
            it.panel.repaint()
        }
    }

    private fun createTotalLootWidget(): XPWidget {
        val bufferedImageSprite = getBufferedImageFromSprite(API.GetSprite(BAG_ICON))
        val l1 = createLabel(formatHtmlLabelText("Total Value: ", primaryColor, "0" + " gp", secondaryColor))
        val l2 = createLabel(formatHtmlLabelText("Total Count: ", primaryColor, "0", secondaryColor))
        return XPWidget(
            skillId = -1,
            panel = createWidgetPanel(bufferedImageSprite,l2,l1),
            xpGainedLabel = l2,
            xpLeftLabel = JLabel(),
            actionsRemainingLabel = JLabel(),
            xpPerHourLabel = l1,
            progressBar = ProgressBar(0.0, Color(150, 50, 50)),
            totalXpGained = 0,
            startTime = System.currentTimeMillis(),
            previousXp = 0
        )
    }

    private fun createWidgetPanel(bufferedImageSprite: BufferedImage, l1 : JLabel, l2 : JLabel): Panel {
        val imageCanvas = ImageCanvas(bufferedImageSprite).apply {
            size = Dimension(width, height)
            background = WIDGET_COLOR
        }

        val imageContainer = Panel(FlowLayout()).apply {
            background = WIDGET_COLOR
            add(imageCanvas)
        }

        return Panel(BorderLayout(5, 5)).apply {
            background = WIDGET_COLOR
            preferredSize = TOTAL_XP_WIDGET_SIZE
            add(imageContainer, BorderLayout.WEST)
            add(createTextPanel(l1,l2), BorderLayout.CENTER)
        }
    }

    private fun createTextPanel(l1 : JLabel, l2: JLabel): Panel {
        return Panel(GridLayout(2, 1, 5, 5)).apply {
            background = WIDGET_COLOR
            add(l1)
            add(l2)
        }
    }

    private fun createLabel(text: String): JLabel {
        return JLabel(text).apply {
            font = Font("Arial", Font.PLAIN, 11)
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
            val rowsNeeded = Math.ceil(totalItems / 6.0).toInt()
            val lootPanelHeight = rowsNeeded * 36 + (rowsNeeded - 1)
            lootPanel.preferredSize = Dimension(270, lootPanelHeight+10)

            lootPanel.revalidate()
            lootPanel.repaint()
        }
    }

    private fun addNewItemToPanel(lootPanel: JPanel, drop: Item, newQuantity: Int) {
        val itemPanel = createItemPanel(drop.id, newQuantity)
        lootPanel.add(itemPanel)
    }

    private fun createItemPanel(itemId: Int, quantity: Int): JPanel {
        val bufferedImageSprite = getBufferedImageFromSprite(API.GetObjSprite(itemId, quantity, true, 0, 0))
        return FixedSizePanel(Dimension(36, 32)).apply {
            preferredSize = Dimension(36, 32)
            background = WIDGET_COLOR
            minimumSize = preferredSize
            maximumSize = preferredSize
            add(ImageCanvas(bufferedImageSprite).apply {
                preferredSize = Dimension(36, 32)
                background = WIDGET_COLOR
                minimumSize = preferredSize
                maximumSize = preferredSize
           }, BorderLayout.CENTER)
            putClientProperty("itemId", itemId)
        }
    }

    private fun updateItemPanelIcon(panel: JPanel, itemId: Int, quantity: Int) {
        panel.removeAll()
        panel.add(createItemPanel(itemId, quantity).components[0], BorderLayout.CENTER)
        panel.revalidate()
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
                    putClientProperty("val", newValue)
                    revalidate()
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
                lastConfirmedKillNpcId = npcId;
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
            // Panel doesn't exist, so create and add it
            lootTrackerView.add(createLootFrame(npcName))
            lootTrackerView.add(Helpers.Spacer(height = 15))
            lootTrackerView.revalidate()
            lootTrackerView.repaint()
        }

        npcKillCounts[npcName] = npcKillCounts.getOrDefault(npcName, 0) + 1
        updateKillCountLabel(lootTrackerView, npcName)
        updateTotalKills()
        newDrops.forEach { drop ->
            val geValue = (gePriceMap[drop.id.toString()]?.toInt() ?: 0) * drop.quantity
            updateValueLabel(lootTrackerView, geValue.toString(), npcName)
            addItemToLootPanel(lootTrackerView, drop, npcName)
            updateTotalValue(geValue)
        }
    }

    private fun createLootFrame(npcName: String): JPanel {
        val childFramePanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            background = WIDGET_COLOR
            minimumSize = Dimension(270, 0)
            maximumSize = Dimension(270, 700)
        }

        val labelPanel = JPanel(BorderLayout()).apply {
            background = Color(21, 21, 21)
            border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
            maximumSize = Dimension(270, 24)
            minimumSize = maximumSize
            preferredSize = maximumSize
        }

        val killCount = npcKillCounts.getOrPut(npcName) { 0 }
        val countLabel = JLabel(formatHtmlLabelText(npcName, secondaryColor, " x $killCount", primaryColor)).apply {
            foreground = Color(200, 200, 200)
            font = Font("Arial", Font.PLAIN, 12)
            horizontalAlignment = JLabel.LEFT
            name = "killCountLabel_$npcName"
        }

        val valueLabel = JLabel("0 gp").apply {
            foreground = Color(200, 200, 200)
            font = Font("Arial", Font.PLAIN, 12)
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

        // Determine number of items and adjust size of lootPanel
        val totalItems = lootItemPanels[npcName]?.size ?: 0
        val rowsNeeded = Math.ceil(totalItems / 6.0).toInt()
        val lootPanelHeight = rowsNeeded * 36 + (rowsNeeded - 1) // Height per row = 36 + spacing
        lootPanel.preferredSize = Dimension(270, lootPanelHeight+10)

        childFramePanel.add(labelPanel)
        childFramePanel.add(lootPanel)
        childFramePanel.add(lootPanel)

        return childFramePanel
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
