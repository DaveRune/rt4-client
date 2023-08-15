package ValUAdd

import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.API.SendMessage
import rt4.Inv
import rt4.ObjTypeList
import kotlin.math.round

@PluginMeta (
    author = "bushtail",
    description = "Quickly tally the value of your inventory with a command.",
    version = 1.0
)
class plugin : Plugin() {
    override fun ProcessCommand(commandStr: String, args: Array<out String>?) {
        when(commandStr.toLowerCase()) {
            "::valuadd" -> {
                var value = 0
                val inventory = Inv.objectContainerCache.get(93) as Inv
                for(i in inventory.objectIds) {
                    if(i != -1) {
                        val obj = ObjTypeList.get(i)
                        value += round(obj.cost * 0.6).toInt()
                    }
                }
                SendMessage("Total HA value of inventory: $value GP")
            }
        }
    }
}