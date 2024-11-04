package KondoKit

import KondoKit.Helpers.convertValue
import KondoKit.Helpers.showToast
import KondoKit.plugin.Companion.TITLE_BAR_COLOR
import KondoKit.plugin.Companion.TOOLTIP_BACKGROUND
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import KondoKit.plugin.StateManager.focusedView
import plugin.Plugin
import plugin.PluginInfo
import plugin.PluginRepository
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.*
import java.util.Timer
import javax.swing.*
import kotlin.math.ceil

/*
  This is used for the runtime editing of plugin variables.
  To expose fields add the @Exposed annotation.
  When they are applied this will trigger an invoke of OnKondoValueUpdated()
  if it is implemented. Check GroundItems plugin for an example.
 */


object ReflectiveEditorView {
    var reflectiveEditorView: JPanel? = null
    private val loadedPlugins: MutableList<String> = mutableListOf()
    const val VIEW_NAME = "REFLECTIVE_EDITOR_VIEW"
    fun createReflectiveEditorView() {
        val reflectiveEditorPanel = JPanel(BorderLayout())
        reflectiveEditorPanel.background = VIEW_BACKGROUND_COLOR
        reflectiveEditorPanel.add(Box.createVerticalStrut(5))
        reflectiveEditorPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        reflectiveEditorView = reflectiveEditorPanel
        addPlugins(reflectiveEditorView!!)
    }

    fun addPlugins(reflectiveEditorView: JPanel) {
        reflectiveEditorView.removeAll() // clear previous
        loadedPlugins.clear()
        try {
            val loadedPluginsField = PluginRepository::class.java.getDeclaredField("loadedPlugins")
            loadedPluginsField.isAccessible = true
            val loadedPlugins = loadedPluginsField.get(null) as HashMap<*, *>

            for ((pluginInfo, plugin) in loadedPlugins) {
                addPluginToEditor(reflectiveEditorView, pluginInfo as PluginInfo, plugin as Plugin)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Add a centered box for plugins that have no exposed fields
        if (loadedPlugins.isNotEmpty()) {
            val noExposedPanel = JPanel(BorderLayout())
            noExposedPanel.background = VIEW_BACKGROUND_COLOR
            noExposedPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

            val label = JLabel("Loaded Plugins without Exposed Fields", SwingConstants.CENTER)
            label.font = Font("RuneScape Small", Font.PLAIN, 16)
            label.foreground = primaryColor
            noExposedPanel.add(label, BorderLayout.NORTH)

            val pluginsList = JList(loadedPlugins.toTypedArray())
            pluginsList.background = WIDGET_COLOR
            pluginsList.foreground = secondaryColor
            pluginsList.font = Font("RuneScape Small", Font.PLAIN, 16)

            // Wrap the JList in a JScrollPane with a fixed height
            val maxScrollPaneHeight = 200
            val scrollPane = JScrollPane(pluginsList).apply {
                verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
                horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            }

            // Create a wrapper panel with BoxLayout to constrain the scroll pane
            val scrollPaneWrapper = JPanel().apply {
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                add(scrollPane)
            }

            noExposedPanel.add(scrollPaneWrapper, BorderLayout.CENTER)

            // Center the panel within the reflectiveEditorView
            val centeredPanel = JPanel().apply {
                preferredSize = Dimension(240, maxScrollPaneHeight)
                maximumSize = preferredSize
                minimumSize = preferredSize
            }
            centeredPanel.layout = BoxLayout(centeredPanel, BoxLayout.Y_AXIS)
            centeredPanel.add(Box.createVerticalGlue())
            centeredPanel.add(noExposedPanel)
            centeredPanel.add(Box.createVerticalGlue())

            reflectiveEditorView.add(Box.createVerticalStrut(10))
            reflectiveEditorView.add(centeredPanel)
        }


        reflectiveEditorView.revalidate()
        if(focusedView == VIEW_NAME)
            reflectiveEditorView.repaint()
    }

    private fun addPluginToEditor(reflectiveEditorView: JPanel, pluginInfo : PluginInfo, plugin: Plugin) {
        reflectiveEditorView.layout = BoxLayout(reflectiveEditorView, BoxLayout.Y_AXIS)

        val fieldNotifier = Helpers.FieldNotifier(plugin)
        val exposedFields = plugin.javaClass.declaredFields.filter { field ->
            field.annotations.any { annotation ->
                annotation.annotationClass.simpleName == "Exposed"
            }
        }

        if (exposedFields.isNotEmpty()) {

            val packageName = plugin.javaClass.`package`.name
            val version = pluginInfo.version
            val labelPanel = JPanel(BorderLayout())
            labelPanel.maximumSize = Dimension(Int.MAX_VALUE, 30)
            labelPanel.background = VIEW_BACKGROUND_COLOR
            labelPanel.border = BorderFactory.createEmptyBorder(5, 0, 0, 0)

            val label = JLabel("$packageName v$version", SwingConstants.CENTER)
            label.foreground = primaryColor
            label.font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
            labelPanel.add(label, BorderLayout.CENTER)
            label.isOpaque = true
            label.background = TITLE_BAR_COLOR
            reflectiveEditorView.add(labelPanel)

            for (field in exposedFields) {
                field.isAccessible = true

                // Get the "Exposed" annotation specifically and retrieve its description, if available
                val exposedAnnotation = field.annotations.firstOrNull { annotation ->
                    annotation.annotationClass.simpleName == "Exposed"
                }

                val description = exposedAnnotation?.let { annotation ->
                    try {
                        val descriptionField = annotation.annotationClass.java.getMethod("description")
                        descriptionField.invoke(annotation) as String
                    } catch (e: NoSuchMethodException) {
                        "" // No description method, return empty string
                    }
                } ?: ""

                val fieldPanel = JPanel()
                fieldPanel.layout = GridBagLayout()
                fieldPanel.background = WIDGET_COLOR
                fieldPanel.foreground = secondaryColor
                fieldPanel.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)
                fieldPanel.maximumSize = Dimension(Int.MAX_VALUE, 40)

                val gbc = GridBagConstraints()
                gbc.insets = Insets(0, 5, 0, 5)

                val label = JLabel(field.name.capitalize())
                label.foreground = secondaryColor
                gbc.gridx = 0
                gbc.gridy = 0
                gbc.weightx = 0.0
                label.font = Font("RuneScape Small", Font.TRUETYPE_FONT, 16)
                gbc.anchor = GridBagConstraints.WEST
                fieldPanel.add(label, gbc)

                // Create appropriate input component based on field type
                val inputComponent: JComponent = when {
                    field.type == Boolean::class.javaPrimitiveType || field.type == java.lang.Boolean::class.java -> JCheckBox().apply {
                        isSelected = field.get(plugin) as Boolean
                    }

                    field.type.isEnum -> JComboBox((field.type.enumConstants as Array<Enum<*>>)).apply {
                        selectedItem = field.get(plugin)
                    }

                    field.type == Int::class.javaPrimitiveType || field.type == Integer::class.java -> JSpinner(SpinnerNumberModel(field.get(plugin) as Int, Int.MIN_VALUE, Int.MAX_VALUE, 1))
                    field.type == Float::class.javaPrimitiveType || field.type == Double::class.javaPrimitiveType || field.type == java.lang.Float::class.java || field.type == java.lang.Double::class.java -> JSpinner(SpinnerNumberModel((field.get(plugin) as Number).toDouble(), -Double.MAX_VALUE, Double.MAX_VALUE, 0.1))
                    else -> JTextField(field.get(plugin)?.toString() ?: "")
                }

                // Add mouse listener to the label only if a description is available
                if (description.isNotBlank()) {
                    label.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                    label.addMouseListener(object : MouseAdapter() {
                        override fun mouseEntered(e: MouseEvent) {
                            showCustomToolTip(description, label)
                        }

                        override fun mouseExited(e: MouseEvent) {
                            customToolTipWindow?.isVisible = false
                        }
                    })
                }

                gbc.gridx = 1
                gbc.gridy = 0
                gbc.weightx = 1.0
                gbc.fill = GridBagConstraints.HORIZONTAL
                fieldPanel.add(inputComponent, gbc)

                val applyButton = JButton("\u2714").apply {
                    maximumSize = Dimension(Int.MAX_VALUE, 8)
                }
                gbc.gridx = 2
                gbc.gridy = 0
                gbc.weightx = 0.0
                gbc.fill = GridBagConstraints.NONE
                applyButton.addActionListener {
                    try {
                        val newValue = when (inputComponent) {
                            is JCheckBox -> inputComponent.isSelected
                            is JComboBox<*> -> inputComponent.selectedItem
                            is JSpinner -> inputComponent.value
                            is JTextField -> convertValue(field.type, field.genericType, inputComponent.text)
                            else -> throw IllegalArgumentException("Unsupported input component type")
                        }
                        fieldNotifier.setFieldValue(field, newValue)
                        showToast(
                                reflectiveEditorView,
                                "${field.name} updated successfully!"
                        )
                    } catch (e: Exception) {
                        showToast(
                                reflectiveEditorView,
                                "Failed to update ${field.name}: ${e.message}",
                                JOptionPane.ERROR_MESSAGE
                        )
                    }
                }

                fieldPanel.add(applyButton, gbc)
                reflectiveEditorView.add(fieldPanel)

                // Track field changes in real-time and update UI
                var previousValue = field.get(plugin)?.toString()
                val timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        val currentValue = field.get(plugin)?.toString()
                        if (currentValue != previousValue) {
                            previousValue = currentValue
                            SwingUtilities.invokeLater {
                                // Update the inputComponent based on the new value
                                when (inputComponent) {
                                    is JCheckBox -> inputComponent.isSelected = field.get(plugin) as Boolean
                                    is JComboBox<*> -> inputComponent.selectedItem = field.get(plugin)
                                    is JSpinner -> inputComponent.value = field.get(plugin)
                                    is JTextField -> inputComponent.text = field.get(plugin)?.toString() ?: ""
                                }
                            }
                        }
                    }
                }, 0, 1000) // Poll every 1000 milliseconds (1 second)
            }

            if (exposedFields.isNotEmpty()) {
                reflectiveEditorView.add(Box.createVerticalStrut(5))
            }
        }
        else {
            loadedPlugins.add(plugin.javaClass.`package`.name)
        }
    }

    var customToolTipWindow: JWindow? = null

    fun showCustomToolTip(text: String, component: JComponent) {
        val _font = Font("RuneScape Small", Font.PLAIN, 16)
        val maxWidth = 150
        val lineHeight = 16

        // Create a dummy JLabel to get FontMetrics for the font used in the tooltip
        val dummyLabel = JLabel()
        dummyLabel.font = _font
        val fontMetrics = dummyLabel.getFontMetrics(_font)

        // Calculate the approximate width of the text
        val textWidth = fontMetrics.stringWidth(text)

        // Calculate the number of lines required based on the text width and max tooltip width
        val numberOfLines = ceil(textWidth.toDouble() / maxWidth).toInt()

        // Calculate the required height of the tooltip
        val requiredHeight = numberOfLines * lineHeight + 6 // Adding some padding

        if (customToolTipWindow == null) {
            customToolTipWindow = JWindow().apply {
                val bgColor = Helpers.colorToHex(TOOLTIP_BACKGROUND)
                val textColor = Helpers.colorToHex(secondaryColor)
                contentPane = JLabel("<html><div style='color: $textColor; background-color: $bgColor; padding: 3px; word-break: break-all;'>$text</div></html>").apply {
                    border = BorderFactory.createLineBorder(Color.BLACK)
                    isOpaque = true
                    background = TOOLTIP_BACKGROUND
                    foreground = Color.WHITE
                    font = _font
                    maximumSize = Dimension(maxWidth, Int.MAX_VALUE)
                    preferredSize = Dimension(maxWidth, requiredHeight)
                }
                pack()
            }
        } else {
            // Update the tooltip text
            val label = customToolTipWindow!!.contentPane as JLabel
            val bgColor = Helpers.colorToHex(TOOLTIP_BACKGROUND)
            val textColor = Helpers.colorToHex(secondaryColor)
            label.text = "<html><div style='color: $textColor; background-color: $bgColor; padding: 3px; word-break: break-all;'>$text</div></html>"
            label.preferredSize = Dimension(maxWidth, requiredHeight)
            customToolTipWindow!!.pack()
        }

        // Position the tooltip near the component
        val locationOnScreen = component.locationOnScreen
        customToolTipWindow!!.setLocation(locationOnScreen.x, locationOnScreen.y + 15)
        customToolTipWindow!!.isVisible = true
    }
}