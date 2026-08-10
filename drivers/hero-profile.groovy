/**
 * Hero Profile - Hubitat C-7
 * Hero identity, XP, coins, Hero Hearts, and dashboard attributes.
 */

metadata {
    definition(name: "Hero Profile", namespace: "tp546", author: "Tom Prendergast") {
        capability "Initialize"
        capability "Refresh"

        attribute "heroName", "string"
        attribute "heroIcon", "string"
        attribute "heroColor", "string"
        attribute "xp", "number"
        attribute "level", "number"
        attribute "xpToNextLevel", "number"
        attribute "levelProgress", "number"
        attribute "coins", "number"
        attribute "heroHearts", "number"
        attribute "rank", "string"
        attribute "status", "string"
        attribute "pendingApprovals", "number"
        attribute "completedToday", "number"
        attribute "currentStreak", "number"
        attribute "heroSummary", "string"
        attribute "dashboardJson", "string"

        command "setHeroName", [[name: "Name", type: "STRING"]]
        command "setHeroIcon", [[name: "Icon", type: "STRING"]]
        command "setHeroColor", [[name: "Color", type: "STRING"]]
        command "setXp", [[name: "XP", type: "NUMBER"]]
        command "awardXp", [[name: "Amount", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "setCoins", [[name: "Coins", type: "NUMBER"]]
        command "awardCoins", [[name: "Amount", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "deductCoins", [[name: "Amount", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "setHeroHearts", [[name: "Hero Hearts", type: "NUMBER"]]
        command "awardHeroHeart", [[name: "Reason", type: "STRING"]]
        command "setPendingApprovals", [[name: "Count", type: "NUMBER"]]
        command "setCompletedToday", [[name: "Count", type: "NUMBER"]]
        command "setCurrentStreak", [[name: "Days", type: "NUMBER"]]
        command "resetProgress"
        command "refreshDashboard"
    }

    preferences {
        input name: "enableLogging", type: "bool", title: "Enable logging", defaultValue: true, required: false
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    if (state.xp == null) state.xp = 0
    if (state.coins == null) state.coins = 0
    if (state.heroHearts == null) state.heroHearts = 0
    if (state.pendingApprovals == null) state.pendingApprovals = 0
    if (state.completedToday == null) state.completedToday = 0
    if (state.currentStreak == null) state.currentStreak = 0
    if (state.heroName == null) state.heroName = device.label ?: device.name ?: "Hero"
    if (state.heroIcon == null) state.heroIcon = "🦸"
    if (state.heroColor == null) state.heroColor = "blue"
    updateAllAttributes()
}

def refresh() {
    updateAllAttributes()
}

def refreshDashboard() {
    updateAllAttributes()
}

def setHeroName(String name) {
    if (name?.trim()) state.heroName = name.trim()
    updateAllAttributes()
}

def setHeroIcon(String icon) {
    if (icon?.trim()) state.heroIcon = icon.trim()
    updateAllAttributes()
}

def setHeroColor(String color) {
    if (color?.trim()) state.heroColor = color.trim()
    updateAllAttributes()
}

def setXp(Number amount) {
    state.xp = Math.max(0, numberValue(amount))
    updateAllAttributes()
}

def awardXp(Number amount, String reason = "XP Award") {
    Integer value = numberValue(amount)
    if (value > 0) state.xp = numberValue(state.xp) + value
    updateAllAttributes()
    logInfo("${getHeroName()} earned ${value} XP: ${reason}")
}

def setCoins(Number amount) {
    state.coins = Math.max(0, numberValue(amount))
    updateAllAttributes()
}

def awardCoins(Number amount, String reason = "Coin Award") {
    Integer value = numberValue(amount)
    if (value > 0) state.coins = numberValue(state.coins) + value
    updateAllAttributes()
    logInfo("${getHeroName()} earned ${value} coins: ${reason}")
}

def deductCoins(Number amount, String reason = "Behavior") {
    Integer value = numberValue(amount)
    if (value > 0) state.coins = Math.max(0, numberValue(state.coins) - value)
    updateAllAttributes()
    logInfo("${getHeroName()} lost ${value} coins: ${reason}")
}

def setHeroHearts(Number amount) {
    state.heroHearts = Math.max(0, numberValue(amount))
    updateAllAttributes()
}

def awardHeroHeart(String reason = "Hero Heart") {
    state.heroHearts = numberValue(state.heroHearts) + 1
    updateAllAttributes()
    logInfo("${getHeroName()} earned a Hero Heart: ${reason}")
}

def setPendingApprovals(Number count) {
    state.pendingApprovals = Math.max(0, numberValue(count))
    updateAllAttributes()
}

def setCompletedToday(Number count) {
    state.completedToday = Math.max(0, numberValue(count))
    updateAllAttributes()
}

def setCurrentStreak(Number days) {
    state.currentStreak = Math.max(0, numberValue(days))
    updateAllAttributes()
}

def resetProgress() {
    state.xp = 0
    state.coins = 0
    state.heroHearts = 0
    state.pendingApprovals = 0
    state.completedToday = 0
    state.currentStreak = 0
    updateAllAttributes()
    logWarn("${getHeroName()} progress reset.")
}

def updateAllAttributes() {
    Integer xpValue = numberValue(state.xp)
    Integer levelValue = calculateLevel(xpValue)
    Integer currentLevelXp = (levelValue - 1) * 100
    Integer progressXp = Math.max(0, xpValue - currentLevelXp)
    Integer progressPercent = Math.min(100, Math.round((progressXp * 100.0) / 100))
    Integer remaining = Math.max(0, (levelValue * 100) - xpValue)

    sendEvent(name: "heroName", value: getHeroName())
    sendEvent(name: "heroIcon", value: state.heroIcon ?: "🦸")
    sendEvent(name: "heroColor", value: state.heroColor ?: "blue")
    sendEvent(name: "xp", value: xpValue, unit: "XP")
    sendEvent(name: "level", value: levelValue)
    sendEvent(name: "xpToNextLevel", value: remaining, unit: "XP")
    sendEvent(name: "levelProgress", value: progressPercent, unit: "%")
    sendEvent(name: "coins", value: numberValue(state.coins), unit: "coins")
    sendEvent(name: "heroHearts", value: numberValue(state.heroHearts))
    sendEvent(name: "rank", value: calculateRank(levelValue))
    sendEvent(name: "status", value: calculateStatus())
    sendEvent(name: "pendingApprovals", value: numberValue(state.pendingApprovals))
    sendEvent(name: "completedToday", value: numberValue(state.completedToday))
    sendEvent(name: "currentStreak", value: numberValue(state.currentStreak), unit: "days")

    Map summary = [
        name: getHeroName(),
        icon: state.heroIcon ?: "🦸",
        color: state.heroColor ?: "blue",
        xp: xpValue,
        level: levelValue,
        coins: numberValue(state.coins),
        heroHearts: numberValue(state.heroHearts),
        rank: calculateRank(levelValue),
        pendingApprovals: numberValue(state.pendingApprovals),
        completedToday: numberValue(state.completedToday),
        currentStreak: numberValue(state.currentStreak),
        status: calculateStatus()
    ]

    sendEvent(name: "heroSummary", value: "${summary.icon} ${summary.name} • Level ${summary.level} • ${summary.coins} coins • ${summary.xp} XP")
    sendEvent(name: "dashboardJson", value: groovy.json.JsonOutput.toJson(summary))
}

def calculateLevel(Integer xpValue) {
    return Math.floor(xpValue / 100) + 1
}

def calculateRank(Integer level) {
    if (level >= 20) return "Hero Legend"
    if (level >= 15) return "Super Hero"
    if (level >= 10) return "Champion"
    if (level >= 5) return "Hero"
    if (level >= 3) return "Sidekick"
    return "Rookie Hero"
}

def calculateStatus() {
    Integer pending = numberValue(state.pendingApprovals)
    Integer completed = numberValue(state.completedToday)
    if (pending > 0) return "${pending} mission approval${pending == 1 ? '' : 's'} pending"
    if (completed > 0) return "${completed} mission${completed == 1 ? '' : 's'} complete today"
    return "Ready for action"
}

def getHeroName() {
    return state.heroName ?: device.label ?: device.name ?: "Hero"
}

def numberValue(def value) {
    try {
        return value == null ? 0 : value as Integer
    } catch (Exception e) {
        return 0
    }
}

def logInfo(String message) {
    if (settings.enableLogging != false) log.info "Hero Profile: ${message}"
}

def logWarn(String message) {
    log.warn "Hero Profile: ${message}"
}
