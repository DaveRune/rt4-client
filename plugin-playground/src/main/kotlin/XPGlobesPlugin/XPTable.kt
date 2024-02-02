package XPGlobesPlugin

object XPTable {

    // source for the experience table: https://oldschool.runescape.wiki/w/Experience#Experience_table
    private val xpTable = arrayOf(
        0,       83,      174,     276,     388,     512,     650,      801,      969,     1154,
        1358,    1584,    1833,    2107,    2411,    2746,    3115,     3523,     3973,    4470,
        5018,    5624,    6291,    7028,    7842,    8740,    9730,     10824,    12031,   13363,
        14833,   16456,   18247,   20224,   22406,   24815,   27473,    30408,    33648,   37224,
        41171,   45529,   50339,   55649,   61512,   67983,   75127,    83014,    91721,   101333,
        111945,  123660,  136594,  150872,  166636,  184040,  203254,   224466,   247886,  273742,
        302288,  333804,  368599,  407015,  449428,  496254,  547953,   605032,   668051,  737627,
        814445,  899257,  992895,  1096278, 1210421, 1336443, 1475581,  1629200,  1798808, 1986068,
        2192818, 2421087, 2673114, 2951373, 3258594, 3597792, 3972294,  4385776,  4842295, 5346332,
        5902831, 6517253, 7195629, 7944614, 8771558, 9684577, 10692629, 11805606, 13034431
    )

    fun getXpRequiredForLevel(level: Int): Int {
        if (level in 1..xpTable.size) {
            return xpTable[level - 1]
        }
        return 0
    }

    fun getLevelForXp(xp: Int): Pair<Int, Int> {
        var lowIndex = 0
        var highIndex = xpTable.size - 1

        if (xp >= xpTable[highIndex]) {
            return Pair(Constants.MAX_LEVEL, xp - xpTable[highIndex]) // Level is max or above, return the highest level
        }

        while (lowIndex <= highIndex) {
            val midIndex = (lowIndex + highIndex) / 2
            when {
                xp < xpTable[midIndex] -> highIndex = midIndex - 1
                xp >= xpTable[midIndex + 1] -> lowIndex = midIndex + 1
                else -> {
                    val currentLevel = midIndex + 1
                    val xpGained = xp - xpTable[midIndex]
                    return Pair(currentLevel, xpGained)
                }
            }
        }

        return Pair(Constants.INVALID_LEVEL, 0) // If xp is below all defined levels
    }

}
