package ToggleResizableSD

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import rt4.DisplayMode
import rt4.GameShell
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

@PluginMeta (
    author = "ipkpjersi",
    description = "Allows you to use F12 to toggle resizable SD.",
    version = 1.0
)
class plugin : Plugin() {
    var toggleResize = false
    override fun Init() {
        API.AddKeyboardListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_F12) {
                    toggleResize = true
                }
            }
        })
    }
    
    override fun ProcessCommand(commandStr: String, args: Array<out String>?) {
        when(commandStr.toLowerCase()) {
            "::resizablesd" -> {
                toggleResize = true //We could call toggleResizableSd() directly here, but it's not necessary.
            }
        }
    }
    
    fun toggleResizableSd() {
        DisplayMode.resizableSD = !DisplayMode.resizableSD;
        if(!DisplayMode.resizableSD){
            //Revert to fixed
            DisplayMode.setWindowMode(true, 0, -1, -1)
        } else {
            //Use resizable
            DisplayMode.setWindowMode(true, 0, GameShell.frameWidth, GameShell.frameHeight)
        }
    }
    
    override fun Draw(timeDelta: Long) {
        if (toggleResize) {
            toggleResizableSd()
            toggleResize = false
        }
    }
}