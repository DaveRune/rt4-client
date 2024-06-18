package ToggleResizableSD

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import rt4.*
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
        if (!DisplayMode.resizableSD && API.GetData("use-resizable-sd") == true) {
            toggleResize = true
        }
    }
    
    override fun ProcessCommand(commandStr: String, args: Array<out String>?) {
        when(commandStr.toLowerCase()) {
            "::resizablesd" -> {
                toggleResize = true //We could call toggleResizableSd() directly here, but it's not necessary.
            }
        }
    }
    
    fun toggleResizableSd() {
        if (InterfaceList.aClass13_26 == null || client.gameState != 30) {
            return
        }
        toggleResize = false
        DisplayMode.resizableSD = !DisplayMode.resizableSD;
        if(!DisplayMode.resizableSD){
            //Revert to fixed
            API.StoreData("use-resizable-sd", false) //Note: It is important to call StoreData before setWindowMode because setWindowMode causes all plugins to reload.
            DisplayMode.setWindowMode(true, 0, -1, -1)
        } else {
            //Use resizable
            API.StoreData("use-resizable-sd", true) //Note: It is important to call StoreData before setWindowMode because setWindowMode causes all plugins to reload.
            DisplayMode.setWindowMode(true, 0, GameShell.frameWidth, GameShell.frameHeight)
        }
    }
    
    override fun Draw(timeDelta: Long) {
        if (toggleResize) {
            toggleResizableSd()
        }
    }
    
    override fun OnLogout() {
        if (DisplayMode.resizableSD) {
            DisplayMode.resizableSD = false
            //Because resizable SD always uses the "HD" size canvas/window mode (check the in-game Graphics Options with resizeable SD enabled if you don't believe me!), useHD becomes true when logging out, so logging out with resizeSD enabled means "HD" will always be enabled on the login screen after logging out, so we might as well fix the HD flyover by replacing the canvas and setting newMode to 2.
            DisplayMode.setWindowMode(true, 2, GameShell.frameWidth, GameShell.frameHeight)
        }
    }
}