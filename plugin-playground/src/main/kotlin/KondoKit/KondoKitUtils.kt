package KondoKit

import java.awt.Color
import java.lang.reflect.Field
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type


/*
  This is used for the runtime editing of plugin variables.
  To expose fields name them starting with `kondoExposed_`
  When they are applied this will trigger an invoke of OnKondoValueUpdated()
  if it is implemented. Check GroundItems plugin for an example.
 */

object KondoKitUtils {
    const val KONDO_PREFIX = "kondoExposed_"

    fun isKondoExposed(field: Field): Boolean {
        return field.name.startsWith(KONDO_PREFIX)
    }

    fun getKondoExposedFields(instance: Any): List<Field> {
        val exposedFields: MutableList<Field> = ArrayList()
        for (field in instance.javaClass.declaredFields) {
            if (isKondoExposed(field)) {
                exposedFields.add(field)
            }
        }
        return exposedFields
    }

    fun convertValue(type: Class<*>, genericType: Type?, value: String): Any {
        return when {
            type == Int::class.java -> value.toInt()
            type == Double::class.java -> value.toDouble()
            type == Boolean::class.java -> value.toBoolean()
            type == Color::class.java -> convertToColor(value)
            type == List::class.java && genericType is ParameterizedType -> {
                val actualTypeArgument = genericType.actualTypeArguments.firstOrNull()
                when {
                    value.isBlank() -> emptyList<Any>() // Handle empty string by returning an empty list
                    actualTypeArgument == Int::class.javaObjectType -> value.trim('[', ']').split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
                    actualTypeArgument == String::class.java -> value.trim('[', ']').split(",").filter { it.isNotBlank() }.map { it.trim() }
                    else -> throw IllegalArgumentException("Unsupported List type: $actualTypeArgument")
                }
            }
            else -> value // Default to String
        }
    }



    fun convertToColor(value: String): Color {
        val color = Color.decode(value) // Assumes value is in format "#RRGGBB" or "0xRRGGBB"
        return color
    }

    fun colorToHex(color: Color): String {
        return "#%02x%02x%02x".format(color.red, color.green, color.blue)
    }

    fun colorToIntArray(color: Color): IntArray {
        return intArrayOf(color.red, color.green, color.blue)
    }

    interface FieldObserver {
        fun onFieldChange(field: Field, newValue: Any?)
    }

    class FieldNotifier(private val plugin: Any) {
        private val observers = mutableListOf<FieldObserver>()

        fun addObserver(observer: FieldObserver) {
            observers.add(observer)
        }

        fun notifyFieldChange(field: Field, newValue: Any?) {
            for (observer in observers) {
                observer.onFieldChange(field, newValue)
            }
        }

        fun setFieldValue(field: Field, value: Any?) {
            field.isAccessible = true
            field.set(plugin, value)
            notifyFieldChange(field, value)

            try {
                val onUpdateMethod = plugin::class.java.getMethod("OnKondoValueUpdated")
                onUpdateMethod.invoke(plugin)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    }

}
