/**
 * Hero Profile
 *
 * Represents one child in the Hero HQ reward system.
 *
 * v0.1.0
 *
 * Designed for:
 *   - Hubitat C-7
 *   - SharpTools
 *   - Fully Kiosk dashboards
 *
 * The parent Hero HQ app creates and manages these devices.
 */

metadata {
    definition(
        name: "Hero Profile",
        namespace: "tp546",
        author: "Tom Prendergast",
        importUrl: "https://raw.githubusercontent.com/tp546/hero-academy/main/drivers/hero-profile.groovy"
    ) {
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

        command "setHeroName", [
            [name: "Name", type: "STRING"]
        ]

        command "setHeroIcon", [
            [name: "Icon", type: "STRING"]
        ]

        command "setHeroColor", [
            [name: "Color", type: "STRING"]
        ]

        command "setXp", [
            [name: "XP", type: "NUMBER"]
        ]

        command "awardXp", [
            [name: "Amount", type: "NUMBER"],
            [name: "Reason", type: "STRING"]
        ]

        command "setCoins", [
            [name: "Coins", type: "NUMBER"]
        ]

        command "awardCoins", [
            [name: "Amount", type: "NUMBER"],
            [name: "Reason", type: "STRING"]
        ]

        command "deductCoins", [
            [name: "Amount", type: "NUMBER"],
            [name: "Reason", type: "STRING"]
        ]

        command "setHeroHearts", [
            [name: "Hero Hearts", type: "NUMBER"]
        ]

        command "awardHeroHeart", [
            [name: "Reason", type: "STRING"]
        ]

        command "setPendingApprovals", [
            [name: "Count", type: "NUMBER"]
        ]

        command "setCompletedToday", [
            [name: "Count", type: "NUMBER"]
        ]

        command "setCurrentStreak", [
            [name: "Days", type: "NUMBER"]
        ]

        command "resetProgress"

        command "refreshDashboard"
    }

    preferences {
        input(
            name: "enableLogging",
            type: "bool",
            title: "Enable logging",
            defaultValue: true,
            required: false
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Lifecycle                                                                  */
/* -------------------------------------------------------------------------- */

def installed() {
    logInfo("Hero Profile installed.")
    initialize()
}

def updated() {
    logInfo("Hero Profile updated.")
    initialize()
}

def initialize() {
    if (state.xp == null) {
        state.xp = 0
    }

    if (state.coins == null) {
        state.coins = 0
    }

    if (state.heroHearts == null) {
        state.heroHearts = 0
    }

    if (state.pendingApprovals == null) {
        state.pendingApprovals = 0
    }

    if (state.completedToday == null) {
        state.completedToday = 0
    }

    if (state.currentStreak == null) {
        state.currentStreak = 0
    }

    if (state.heroName == null) {
        state.heroName = device.label ?: device.name ?: "Hero"
    }

    if (state.heroIcon == null) {
        state.heroIcon = "🦸"
    }

    if (state.heroColor == null) {
        state.heroColor = "blue"
    }

    updateAllAttributes()
}

/* -------------------------------------------------------------------------- */
/* Refresh                                                                    */
/* -------------------------------------------------------------------------- */

def refresh() {
    updateAllAttributes()
}

def refreshDashboard() {
    updateAllAttributes()
}

/* -------------------------------------------------------------------------- */
/* Hero identity                                                               */
/* -------------------------------------------------------------------------- */

def setHeroName(String name) {
    String value = name?.trim()

    if (!value) {
        return
    }

    state.heroName = value
    sendEvent(name: "heroName", value: value)
    updateSummary()
}

def setHeroIcon(String icon) {
    String value = icon?.trim()

    if (!value) {
        return
    }

    state.heroIcon = value
    sendEvent(name: "heroIcon", value: value)
    updateSummary()
}

def setHeroColor(String color) {
    String value = color?.trim()

    if (!value) {
        return
    }

    state.heroColor = value
    sendEvent(name: "heroColor", value: value)
    updateSummary()
}

/* -------------------------------------------------------------------------- */
/* XP                                                                         */
/* -------------------------------------------------------------------------- */

def setXp(Number amount) {
    Integer value = normalizeNumber(amount)

    if (value < 0) {
        value = 0
    }

    state.xp = value
    updateXpAttributes()
    updateSummary()

    logInfo("XP set to ${value}.")
}

def awardXp(Number amount, String reason = "XP Award") {
    Integer value = normalizeNumber(amount)

    if (value <= 0) {
        return
    }

    Integer current = normalizeNumber(state.xp)
    Integer total = current + value

    state.xp = total

    updateXpAttributes()
    updateSummary()

    logInfo(
        "Awarded ${value} XP to ${getHeroName()} " +
        "(${reason ?: "XP Award"}). Total XP: ${total}"
    )
}

/* -------------------------------------------------------------------------- */
/* Coins                                                                       */
/* -------------------------------------------------------------------------- */

def setCoins(Number amount) {
    Integer value = normalizeNumber(amount)

    if (value < 0) {
        value = 0
    }

    state.coins = value

    sendEvent(
        name: "coins",
        value: value,
        unit: "coins"
    )

    updateSummary()

    logInfo("Coins set to ${value}.")
}

def awardCoins(Number amount, String reason = "Coin Award") {
    Integer value = normalizeNumber(amount)

    if (value <= 0) {
        return
    }

    Integer current = normalizeNumber(state.coins)
    Integer total = current + value

    state.coins = total

    sendEvent(
        name: "coins",
        value: total,
        unit: "coins",
        descriptionText: "+${value} coins: ${reason}"
    )

    updateSummary()

    logInfo(
        "Awarded ${value} coins to ${getHeroName()} " +
        "(${reason ?: "Coin Award"}). Total coins: ${total}"
    )
}

def deductCoins(Number amount, String reason = "Behavior") {
    Integer value = normalizeNumber(amount)

    if (value <= 0) {
        return
    }

    Integer current = normalizeNumber(state.coins)
    Integer total = Math.max(0, current - value)

    state.coins = total

    sendEvent(
        name: "coins",
        value: total,
        unit: "coins",
        descriptionText: "-${value} coins: ${reason}"
    )

    updateSummary()

    logInfo(
        "Deducted ${value} coins from ${getHeroName()} " +
        "(${reason ?: "Behavior"}). Total coins: ${total}"
    )
}

/* -------------------------------------------------------------------------- */
/* Hero Hearts                                                                 */
/* -------------------------------------------------------------------------- */

def setHeroHearts(Number amount) {
    Integer value = normalizeNumber(amount)

    if (value < 0) {
        value = 0
    }

    state.heroHearts = value

    sendEvent(
        name: "heroHearts",
        value: value
    )

    updateSummary()
}

def awardHeroHeart(String reason = "Hero Heart") {
    Integer current = normalizeNumber(state.heroHearts)
    Integer total = current + 1

    state.heroHearts = total

    sendEvent(
        name: "heroHearts",
        value: total,
        descriptionText: "${reason ?: "Hero Heart"}"
    )

    updateSummary()

    logInfo(
        "${getHeroName()} earned a Hero Heart " +
        "(${reason ?: "Hero Heart"}). Total: ${total}"
    )
}

/* -------------------------------------------------------------------------- */
/* Mission / approval counters                                                */
/* -------------------------------------------------------------------------- */

def setPendingApprovals(Number count) {
    Integer value = Math.max(0, normalizeNumber(count))

    state.pendingApprovals = value

    sendEvent(
        name: "pendingApprovals",
        value: value
    )

    updateSummary()
}

def setCompletedToday(Number count) {
    Integer value = Math.max(0, normalizeNumber(count))

    state.completedToday = value

    sendEvent(
        name: "completedToday",
        value: value
    )

    updateSummary()
}

def setCurrentStreak(Number days) {
    Integer value = Math.max(0, normalizeNumber(days))

    state.currentStreak = value

    sendEvent(
        name: "currentStreak",
        value: value,
        unit: "days"
    )

    updateSummary()
}

/* -------------------------------------------------------------------------- */
/* Reset                                                                       */
/* -------------------------------------------------------------------------- */

def resetProgress() {
    state.xp = 0
    state.coins = 0
    state.heroHearts = 0
    state.pendingApprovals = 0
    state.completedToday = 0
    state.currentStreak = 0

    updateAllAttributes()

    logWarn("${getHeroName()} progress was reset.")
}

/* -------------------------------------------------------------------------- */
/* Attribute updates                                                           */
/* -------------------------------------------------------------------------- */

private void updateAllAttributes() {
    updateIdentityAttributes()
    updateXpAttributes()

    sendEvent(
        name: "coins",
        value: normalizeNumber(state.coins),
        unit: "coins"
    )

    sendEvent(
        name: "heroHearts",
        value: normalizeNumber(state.heroHearts)
    )

    sendEvent(
        name: "pendingApprovals",
        value: normalizeNumber(state.pendingApprovals)
    )

    sendEvent(
        name: "completedToday",
        value: normalizeNumber(state.completedToday)
    )

    sendEvent(
        name: "currentStreak",
        value: normalizeNumber(state.currentStreak),
        unit: "days"
    )

    updateSummary()
}

private void updateIdentityAttributes() {
    sendEvent(
        name: "heroName",
        value: getHeroName()
    )

    sendEvent(
        name: "heroIcon",
        value: state.heroIcon ?: "🦸"
    )

    sendEvent(
        name: "heroColor",
        value: state.heroColor ?: "blue"
    )
}

private void updateXpAttributes() {
    Integer currentXp = normalizeNumber(state.xp)

    Integer level = calculateLevel(currentXp)
    Integer currentLevelXp = (level - 1) * XP_PER_LEVEL
    Integer nextLevelXp = level * XP_PER_LEVEL
    Integer progress = currentXp - currentLevelXp

    Integer percent = Math.min(
        100,
        Math.max(
            0,
            Math.round(
                (progress * 100.0) / XP_PER_LEVEL
            )
        )
    )

    Integer remaining = Math.max(
        0,
        nextLevelXp - currentXp
    )

    sendEvent(
        name: "xp",
        value: currentXp,
        unit: "XP"
    )

    sendEvent(
        name: "level",
        value: level
    )

    sendEvent(
        name: "xpToNextLevel",
        value: remaining,
        unit: "XP"
    )

    sendEvent(
        name: "levelProgress",
        value: percent,
        unit: "%"
    )

    sendEvent(
        name: "rank",
        value: calculateRank(level)
    )

    sendEvent(
        name: "status",
        value: calculateStatus()
    )
}

/* -------------------------------------------------------------------------- */
/* Dashboard summary                                                           */
/* -------------------------------------------------------------------------- */

private void updateSummary() {
    Integer xpValue = normalizeNumber(state.xp)
    Integer coinsValue = normalizeNumber(state.coins)
    Integer heartsValue = normalizeNumber(state.heroHearts)
    Integer levelValue = calculateLevel(xpValue)

    String rankValue = calculateRank(levelValue)
    String statusValue = calculateStatus()

    Map summary = [
        name: getHeroName(),
        icon: state.heroIcon ?: "🦸",
        color: state.heroColor ?: "blue",
        xp: xpValue,
        coins: coinsValue,
        level: levelValue,
        rank: rankValue,
        heroHearts: heartsValue,
        pendingApprovals: normalizeNumber(state.pendingApprovals),
        completedToday: normalizeNumber(state.completedToday),
        currentStreak: normalizeNumber(state.currentStreak),
        status: statusValue
    ]

    String json = groovy.json.JsonOutput.toJson(summary)

    sendEvent(
        name: "heroSummary",
        value: buildSummaryText()
    )

    sendEvent(
        name: "dashboardJson",
        value: json
    )
}

/* -------------------------------------------------------------------------- */
/* Calculations                                                                */
/* -------------------------------------------------------------------------- */

private Integer calculateLevel(Integer xpValue) {
    if (xpValue < 0) {
        return 1
    }

    return Math.floor(xpValue / XP_PER_LEVEL) + 1
}

private String calculateRank(Integer level) {
    if (level >= 20) {
        return "Hero Legend"
    }

    if (level >= 15) {
        return "Super Hero"
    }

    if (level >= 10) {
        return "Champion"
    }

    if (level >= 5) {
        return "Hero"
    }

    if (level >= 3) {
        return "Sidekick"
    }

    return "Rookie Hero"
}

private String calculateStatus() {
    Integer pending = normalizeNumber(state.pendingApprovals)
    Integer completed = normalizeNumber(state.completedToday)

    if (pending > 0) {
        return "${pending} mission approval${pending == 1 ? '' : 's'} pending"
    }

    if (completed > 0) {
        return "${completed} mission${completed == 1 ? '' : 's'} complete today"
    }

    return "Ready for action"
}

private String buildSummaryText() {
    String icon = state.heroIcon ?: "🦸"
    String name = getHeroName()

    Integer xpValue = normalizeNumber(state.xp)
    Integer coinsValue = normalizeNumber(state.coins)
    Integer levelValue = calculateLevel(xpValue)

    return "${icon} ${name} • Level ${levelValue} • " +
        "${coinsValue} coins • ${xpValue} XP"
}

/* -------------------------------------------------------------------------- */
/* Helpers                                                                     */
/* -------------------------------------------------------------------------- */

private String getHeroName() {
    return state.heroName ?: device.label ?: device.name ?: "Hero"
}

private Integer normalizeNumber(def value) {
    if (value == null) {
        return 0
    }

    try {
        return value as Integer
    } catch (Exception ignored) {
        return 0
    }
}

private void logInfo(String message) {
    if (settings.enableLogging != false) {
        log.info "Hero Profile: ${message}"
    }
}

private void logWarn(String message) {
    log.warn "Hero Profile: ${message}"
}

/*
 * 100 XP = one level.
 *
 * Keeping this deliberately simple for v0.1.0 means the kids can
 * immediately understand the progression system. We can move the
 * scoring rules into a shared library once missions are implemented.
 */
def XP_PER_LEVEL = 100