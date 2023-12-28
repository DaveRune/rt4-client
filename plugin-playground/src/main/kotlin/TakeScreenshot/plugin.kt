package TakeScreenshot

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

@PluginMeta (
    author = "ipkpjersi",
    description = "Allows you to use CRTL + PRINTSCREEN to take a screenshot.",
    version = 1.0
)
class plugin : Plugin() {
    override fun Init() {
        API.AddKeyboardListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_PRINTSCREEN && e.isControlDown) {
                    API.Screenshot()
                } 
            }
        })
    }
}