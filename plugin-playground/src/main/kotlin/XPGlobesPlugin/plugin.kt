package XPGlobesPlugin


import rt4.Sprite
import plugin.Plugin
import plugin.annotations.PluginMeta
import plugin.api.*
import java.awt.Color
import java.awt.geom.Arc2D
import java.awt.image.BufferedImage


@PluginMeta(
    author = "Pyrethus",
    description = "Draws experience globes (level progress) on experience gains.",
    version = 0.9
)


class plugin : Plugin() {
    private var xpGlobes = Array(Constants.SKILL_COUNT) { skillId -> XPGlobe(skillId, Constants.INVALID_XP, 0L, null) }
    private var lastGain = 0L
    private var backgroundSprite: Sprite? = null
    private var borderSprite: Sprite? = null


    override fun Draw(deltaTime: Long) {
        if (System.currentTimeMillis() - lastGain >= Constants.GLOBE_LIFETIME)
            return

        var posX = API.GetWindowDimensions().width / 2
        val posY = API.GetWindowDimensions().height / 4

        if (API.GetWindowMode() == WindowMode.FIXED) {
            posX += 60
        }

        API.ClipRect(0, 0, posX * 2, posY * 4)

        // update globes
        val activeGlobes = ArrayList<XPGlobe>()
        for (xpGlobe in xpGlobes) {
            val globeDelta = System.currentTimeMillis() - xpGlobe.timestamp
            if (globeDelta >= Constants.GLOBE_LIFETIME) {
                xpGlobe.timestamp  = 0L // dead
            }

            if (xpGlobe.timestamp != 0L) {
                activeGlobes.add(xpGlobe) // alive
            }
        }

        val maxGlobes = if (API.GetWindowMode() == WindowMode.FIXED) Constants.MAX_GLOBES_SD else Constants.MAX_GLOBES
        val globeCount = if (activeGlobes.size > maxGlobes) maxGlobes else activeGlobes.size
        val (backgroundSize, globeBorder, xpBorder) = getGlobeDimensions()
        val globeSize = backgroundSize + globeBorder * 2 + xpBorder * 2
        val allGlobesWidth = globeCount * (globeSize + Constants.GLOBE_PADDING) - Constants.GLOBE_PADDING
        var globePosX = posX - (allGlobesWidth / 2)

        // render globes
        activeGlobes.take(globeCount).forEach { xpGlobe ->
            drawXpGlobe(xpGlobe, globePosX)
            globePosX += globeSize + Constants.GLOBE_PADDING // Update globePosX for the next globe
        }
    }


    override fun OnXPUpdate(skillId: Int, xp: Int) {
        if (xpGlobes[skillId].xp == Constants.INVALID_XP) {
            xpGlobes[skillId].xp = xp
            return
        }

        if (xp == xpGlobes[skillId].xp) {
            return
        }

        val prevXp = xpGlobes[skillId].xp
        xpGlobes[skillId].xp = xp
        xpGlobes[skillId].timestamp = 0L
        val (prevLevel, _) = XPTable.getLevelForXp(prevXp)
        val (level, gainedXp) = XPTable.getLevelForXp(xp)

        // we do not draw XP globes for level >= MAX_LEVEL
        if (level != Constants.INVALID_LEVEL && level < Constants.MAX_LEVEL) {
            var arcWeight = 1.0
            var arcColor = Constants.GLOBE_XP_ARC_LEVEL_UP_COLOR

            if (level == prevLevel) {
                val levelXpDiff = XPTable.getXpRequiredForLevel(level + 1) - XPTable.getXpRequiredForLevel(level)
                arcWeight = gainedXp.toDouble() / levelXpDiff.toDouble()
                arcColor = Constants.GLOBE_XP_ARC_COLOR
            }

            // remember timestamps
            lastGain = System.currentTimeMillis()
            xpGlobes[skillId].timestamp = lastGain

            val (backgroundSize, globeBorder, xpBorder) = getGlobeDimensions()
            val globeSize = backgroundSize + globeBorder * 2 + xpBorder * 2
            xpGlobes[skillId].arcSprite = createArcSprite(backgroundSize + xpBorder * 2, arcColor, arcWeight)

            if (borderSprite == null) {
                borderSprite = createArcSprite(globeSize, Constants.GLOBE_BORDER_COLOR, 1.0)
            }
            if (backgroundSprite == null) {
                backgroundSprite = createArcSprite(backgroundSize, Constants.GLOBE_BKG_COLOR, 1.0)
            }
        }
    }


    override fun OnLogout() {
        lastGain = 0L
        xpGlobes = Array(Constants.SKILL_COUNT)  { skillId -> XPGlobe(skillId, Constants.INVALID_XP, 0L, null) }
    }


    data class XPGlobe(val skillId: Int, var xp: Int, var timestamp: Long, var arcSprite: Sprite?)


    private fun getGlobeDimensions() : Triple<Int, Int, Int> {
        val backgroundSize = Constants.GLOBE_BKG_SIZE
        val globeBorder = Constants.GLOBE_BORDER_WIDTH
        val xpBorder: Int = Constants.GLOBE_XP_ARC_WIDTH
        return Triple(backgroundSize, globeBorder, xpBorder)
    }


    private fun drawXpGlobe(globe: XPGlobe, posX: Int, posY: Int = Constants.GLOBES_Y_OFFSET) {

        val (backgroundSize, globeBorder, xpBorder) = getGlobeDimensions()
        val totalBorder = globeBorder + xpBorder

        // rendering background
        borderSprite?.render(posX, posY)
        globe.arcSprite?.render(posX + globeBorder, posY + globeBorder)
        backgroundSprite?.render(posX + totalBorder, posY + totalBorder)

        // rendering skill sprite
        val skillSprite = XPSprites.getSpriteForSkill(globe.skillId)

        val spriteWidth = skillSprite?.anInt1860 ?: 0 // sprite width without trimmed pixels
        val spriteHeight = skillSprite?.anInt1866 ?: 0 // sprite height without trimmed pixels
        val xOffset = (backgroundSize - spriteWidth) / 2
        val yOffset = (backgroundSize - spriteHeight) / 2

        val drawX = posX + totalBorder + xOffset
        val drawY = posY + totalBorder + yOffset

        // even if the centering logic is correct, the sprite seems not to be well-centered inside the
        // graphic resource. Manually adjust...
        val (spriteXOffset, spriteYOffset) = XPSprites.getSpriteOffsetForSkill(globe.skillId)

        skillSprite?.render(drawX + spriteXOffset, drawY + spriteYOffset)
    }


    private fun createArcSprite(size: Int, arcColor: Color, weight: Double): Sprite {
        return SpritePNGLoader.getImageIndexedSprite(createArcImage(size, arcColor, weight))
    }


    private fun createArcImage(size: Int, arcColor: Color, weight: Double): BufferedImage {
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        // Enable antialiasing for smoother circle edges
        graphics.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)

        // Set the color for the circle
        graphics.color = arcColor

        // Calculate the angle and starting angle based on the weight
        val angle = weight * 360
        val startAngle = 360 * (0.75 - weight)

        // Draw the portion of the circle
        graphics.fill(Arc2D.Double(0.0, 0.0, size.toDouble(), size.toDouble(), startAngle, angle, Arc2D.PIE))

        // Dispose graphics to release resources
        graphics.dispose()

        return image
    }
}
