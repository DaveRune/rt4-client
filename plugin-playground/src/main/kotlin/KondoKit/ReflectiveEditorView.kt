package KondoKit

import KondoKit.KondoKitUtils.convertValue
import KondoKit.plugin.Companion.VIEW_BACKGROUND_COLOR
import KondoKit.plugin.Companion.WIDGET_COLOR
import KondoKit.plugin.Companion.primaryColor
import KondoKit.plugin.Companion.secondaryColor
import plugin.Plugin
import plugin.PluginRepository
import java.awt.*
import java.lang.reflect.Field
import java.util.*
import java.util.Timer
import javax.swing.*

object ReflectiveEditorView {
    fun createReflectiveEditorView(): JPanel {
        val reflectiveEditorPanel = JPanel(BorderLayout())
        reflectiveEditorPanel.background = VIEW_BACKGROUND_COLOR
        reflectiveEditorPanel.add(Box.createVerticalStrut(5))
        reflectiveEditorPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
        return reflectiveEditorPanel
    }

    fun addPlugins(reflectiveEditorView: JPanel) {
        try {
            val loadedPluginsField = PluginRepository::class.java.getDeclaredField("loadedPlugins")
            loadedPluginsField.isAccessible = true
            val loadedPlugins = loadedPluginsField.get(null) as HashMap<*, *>

            for ((_, plugin) in loadedPlugins) {
                addPluginToEditor(reflectiveEditorView, plugin as Plugin)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        reflectiveEditorView.revalidate()
        reflectiveEditorView.repaint()
    }

    private fun addPluginToEditor(reflectiveEditorView: JPanel, plugin: Any) {
        reflectiveEditorView.layout = BoxLayout(reflectiveEditorView, BoxLayout.Y_AXIS)

        val fieldNotifier = KondoKitUtils.FieldNotifier(plugin)
        val exposedFields = KondoKitUtils.getKondoExposedFields(plugin)

        if (exposedFields.isNotEmpty()) {
            val packageName = plugin.javaClass.`package`.name
            val labelPanel = JPanel(BorderLayout())
            labelPanel.maximumSize = Dimension(Int.MAX_VALUE, 30) // Adjust height to be minimal
            labelPanel.background = VIEW_BACKGROUND_COLOR // Ensure it matches the overall background
            labelPanel.border = BorderFactory.createEmptyBorder(5, 0, 5, 0)

            val label = JLabel("$packageName", SwingConstants.CENTER)
            label.foreground = primaryColor
            label.font = Font("Arial", Font.BOLD, 14)
            labelPanel.add(label, BorderLayout.CENTER)
            reflectiveEditorView.add(labelPanel)
        }

        for (field in exposedFields) {
            field.isAccessible = true

            val fieldPanel = JPanel()
            fieldPanel.layout = GridBagLayout()
            fieldPanel.background = WIDGET_COLOR // Match the background for minimal borders
            fieldPanel.foreground = secondaryColor
            fieldPanel.border = BorderFactory.createEmptyBorder(5, 0, 5, 0) // No visible border, just spacing
            fieldPanel.maximumSize = Dimension(Int.MAX_VALUE, 40)

            val gbc = GridBagConstraints()
            gbc.insets = Insets(0, 5, 0, 5) // Less padding, more minimal spacing

            val label = JLabel(field.name.removePrefix(KondoKitUtils.KONDO_PREFIX).capitalize())
            label.foreground = secondaryColor
            gbc.gridx = 0
            gbc.gridy = 0
            gbc.weightx = 0.0
            gbc.anchor = GridBagConstraints.WEST
            fieldPanel.add(label, gbc)

            val textField = JTextField(field.get(plugin)?.toString() ?: "")
            textField.background = VIEW_BACKGROUND_COLOR
            textField.foreground = secondaryColor
            textField.border = BorderFactory.createLineBorder(WIDGET_COLOR, 1) // Subtle border
            gbc.gridx = 1
            gbc.gridy = 0
            gbc.weightx = 1.0
            gbc.fill = GridBagConstraints.HORIZONTAL
            fieldPanel.add(textField, gbc)

            val applyButton = JButton("Apply")
            applyButton.background = primaryColor
            applyButton.foreground = VIEW_BACKGROUND_COLOR
            applyButton.border = BorderFactory.createLineBorder(primaryColor, 1)
            gbc.gridx = 2
            gbc.gridy = 0
            gbc.weightx = 0.0
            gbc.fill = GridBagConstraints.NONE
            applyButton.addActionListener {
                try {
                    val newValue = convertValue(field.type, field.genericType, textField.text)
                    fieldNotifier.setFieldValue(field, newValue)
                    JOptionPane.showMessageDialog(
                        null,
                        "${field.name.removePrefix(KondoKitUtils.KONDO_PREFIX)} updated successfully!"
                    )
                } catch (e: Exception) {
                    JOptionPane.showMessageDialog(
                        null,
                        "Failed to update ${field.name.removePrefix(KondoKitUtils.KONDO_PREFIX)}: ${e.message}",
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                    )
                }
            }

            fieldPanel.add(applyButton, gbc)
            reflectiveEditorView.add(fieldPanel)

            var previousValue = field.get(plugin)?.toString()
            val timer = Timer()
            timer.schedule(object : TimerTask() {
                override fun run() {
                    val currentValue = field.get(plugin)?.toString()
                    if (currentValue != previousValue) {
                        previousValue = currentValue
                        SwingUtilities.invokeLater {
                            fieldNotifier.notifyFieldChange(field, currentValue)
                        }
                    }
                }
            }, 0, 1000)

            fieldNotifier.addObserver(object : KondoKitUtils.FieldObserver {
                override fun onFieldChange(field: Field, newValue: Any?) {
                    if (field.name.removePrefix(KondoKitUtils.KONDO_PREFIX).equals(label.text, ignoreCase = true)) {
                        textField.text = newValue?.toString() ?: ""
                        textField.revalidate()
                        textField.repaint()
                    }
                }
            })
        }
        if (exposedFields.isNotEmpty()) {
            reflectiveEditorView.add(Box.createVerticalStrut(10))
        }
    }
}
