package ToggleResizableSD

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API
import rt4.*
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent

class plugin : Plugin() {
    var toggleResize = false
    var wantHd = false //Setting wantHd to true hides the black screen on logout (when resize SD is enabled), by enabling HD on logout 
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
        var osNameLowerCase: String = ""
        var osName: String
    
        try {
            osName = System.getProperty("os.name")
        } catch (e: Exception) {
            osName = "Unknown"
        }
    
        osNameLowerCase = osName.toLowerCase()
        if (!osNameLowerCase.startsWith("mac")) {
            wantHd = true
        }
        if (API.GetData("want-hd") == false) {
            wantHd = false
        }
    }
    
    override fun ProcessCommand(commandStr: String, args: Array<out String>?) {
        when(commandStr.toLowerCase()) {
            "::toggleresizablesd", "::resizablesd", "::togglersd", "::rsd" -> {
                toggleResize = true //We could call toggleResizableSd() directly here, but it's not necessary.
            }
            "::toggleresizablesdhd", "::resizablesdhd", "::togglersdhd", "::rsdhd", -> {
                wantHd = !wantHd
                API.StoreData("want-hd", wantHd)
                API.SendMessage("You have turned login screen HD " + (if (wantHd) "on" else "off"))
            }
        }
    }
    
    fun toggleResizableSd() {
        //We only want to toggle resizable SD when we are logged in and the lobby/welcome interface is not open.
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
        if (DisplayMode.resizableSD && wantHd) {
            //Because resizable SD always uses the "HD" size canvas/window mode (check the in-game Graphics Options with resizeable SD enabled if you don't believe me!), useHD becomes true when logging out, so logging out with resizeSD enabled means "HD" will always be enabled on the login screen after logging out, so we might as well fix the HD flyover by setting resizableSD to false first, and then calling setWindowMode to replace the canvas and set newMode to 2.
            DisplayMode.resizableSD = false
            DisplayMode.setWindowMode(true, 2, GameShell.frameWidth, GameShell.frameHeight)
        }
    }
}