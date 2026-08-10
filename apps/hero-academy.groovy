/**
 * Hero HQ
 *
 * Hero Academy family reward system for Hubitat.
 *
 * v0.1.0
 *
 * Designed for:
 *   - Hubitat C-7
 *   - Zach, Josh, and Charlie
 *   - SharpTools dashboards
 *   - Fully Kiosk wall tablets
 *
 * This parent app owns family configuration and orchestrates
 * Hero Profile and Hero Dashboard child devices.
 */

definition(
    name: "Hero HQ",
    namespace: "tp546",
    author: "Tom Prendergast",
    description: "Hero Academy family reward and behavior system.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/tp546/hero-academy/main/images/hero-hq.png",
    iconX2Url: "https://raw.githubusercontent.com/tp546/hero-academy/main/images/hero-hq.png",
    iconX3Url: "https://raw.githubusercontent.com/tp546/hero-academy/main/images/hero-hq.png"
)

preferences {
    page(
        name: "mainPage",
        title: "🦸 Hero HQ",
        install: true,
        uninstall: true
    ) {
        section("Heroes") {
            input(
                name: "enableZach",
                type: "bool",
                title: "🔥 Enable Zach",
                defaultValue: true,
                required: false
            )

            input(
                name: "zachColor",
                type: "text",
                title: "Zach color",
                defaultValue: "blue",
                required: false
            )

            input(
                name: "enableJosh",
                type: "bool",
                title: "⚡ Enable Josh",
                defaultValue: true,
                required: false
            )

            input(
                name: "joshColor",
                type: "text",
                title: "Josh color",
                defaultValue: "green",
                required: false
            )

            input(
                name: "enableCharlie",
                type: "bool",
                title: "🌟 Enable Charlie",
                defaultValue: true,
                required: false
            )

            input(
                name: "charlieColor",
                type: "text",
                title: "Charlie color",
                defaultValue: "yellow",
                required: false
            )
        }

        section("Hero Dashboard") {
            input(
                name: "createDashboard",
                type: "bool",
                title: "Create Hero HQ Dashboard device",
                description: "Recommended: Yes",
                defaultValue: true,
                required: false
            )

            input(
                name: "dashboardName",
                type: "text",
                title: "Dashboard device name",
                defaultValue: "Hero HQ Dashboard",
                required: false
            )
        }

        section("Options") {
            input(
                name: "enableLogging",
                type: "bool",
                title: "Enable logging",
                defaultValue: true,
                required: false
            )
        }

        section("About") {
            paragraph(
                "<b>Hero HQ v0.1.0</b><br>" +
                "Family reward system for Zach, Josh, and Charlie.<br><br>" +
                "The parent app manages hero profiles and the shared Hero HQ dashboard."
            )
        }
    }
}

/* -------------------------------------------------------------------------- */
/* Lifecycle                                                                  */
/* -------------------------------------------------------------------------- */

def installed() {
    logInfo("Hero HQ installed.")
    initialize()
}

def updated() {
    logInfo("Hero HQ updated.")
    unsubscribe()
    initialize()
}

def initialize() {
    createConfiguredChildren()
    refreshDashboard()
}

/* -------------------------------------------------------------------------- */
/* Child device management                                                    */
/* -------------------------------------------------------------------------- */

private void createConfiguredChildren() {
    if (settings.enableZach != false) {
        createHeroProfile(
            "Zach",
            "🔥",
            settings.zachColor ?: "blue",
            "zach"
        )
    } else {
        removeHeroProfile("zach")
    }

    if (settings.enableJosh != false) {
        createHeroProfile(
            "Josh",
            "⚡",
            settings.joshColor ?: "green",
            "josh"
        )
    } else {
        removeHeroProfile("josh")
    }

    if (settings.enableCharlie != false) {
        createHeroProfile(
            "Charlie",
            "🌟",
            settings.charlieColor ?: "yellow",
            "charlie"
        )
    } else {
        removeHeroProfile("charlie")
    }

    if (settings.createDashboard != false) {
        createDashboardDevice()
    } else {
        removeDashboardDevice()
    }
}

private void createHeroProfile(
    String heroName,
    String icon,
    String color,
    String key
) {
    String dni = getHeroDni(key)
    String childName = "${heroName} Hero Profile"

    def existing = getChildDevice(dni)

    if (existing) {
        try {
            existing.setHeroName(heroName)
            existing.setHeroIcon(icon)
            existing.setHeroColor(color)
        } catch (Exception e) {
            logWarn("Could not update ${childName}: ${e.message}")
        }

        logInfo("Hero profile already exists: ${childName}")
        return
    }

    try {
        addChildDevice(
            "tp546",
            "Hero Profile",
            dni,
            [
                name: childName,
                label: childName,
                isComponent: false
            ]
        )

        def hero = getChildDevice(dni)

        if (hero) {
            hero.setHeroName(heroName)
            hero.setHeroIcon(icon)
            hero.setHeroColor(color)
            logInfo("Created ${childName}.")
        }
    } catch (Exception e) {
        logWarn(
            "Unable to create ${childName}. " +
            "Make sure the 'Hero Profile' driver is installed. " +
            "Error: ${e.message}"
        )
    }
}

private void createDashboardDevice() {
    String dni = getDashboardDni()
    String label = settings.dashboardName ?: "Hero HQ Dashboard"

    def existing = getChildDevice(dni)

    if (existing) {
        logInfo("Hero HQ Dashboard already exists.")
        refreshDashboard()
        return
    }

    try {
        addChildDevice(
            "tp546",
            "Hero Dashboard",
            dni,
            [
                name: label,
                label: label,
                isComponent: false
            ]
        )

        def dashboard = getChildDevice(dni)

        if (dashboard) {
            logInfo("Created ${label}.")
            refreshDashboard()
        }
    } catch (Exception e) {
        logWarn(
            "Unable to create Hero HQ Dashboard. " +
            "Make sure the 'Hero Dashboard' driver is installed. " +
            "Error: ${e.message}"
        )
    }
}

/* -------------------------------------------------------------------------- */
/* Child removal                                                              */
/* -------------------------------------------------------------------------- */

private void removeHeroProfile(String key) {
    String dni = getHeroDni(key)
    def child = getChildDevice(dni)

    if (!child) {
        return
    }

    try {
        deleteChildDevice(dni)
        logInfo("Removed disabled hero profile: ${key}")
    } catch (Exception e) {
        logWarn("Unable to remove hero profile ${key}: ${e.message}")
    }
}

private void removeDashboardDevice() {
    String dni = getDashboardDni()
    def child = getChildDevice(dni)

    if (!child) {
        return
    }

    try {
        deleteChildDevice(dni)
        logInfo("Removed Hero HQ Dashboard.")
    } catch (Exception e) {
        logWarn("Unable to remove Hero HQ Dashboard: ${e.message}")
    }
}

/* -------------------------------------------------------------------------- */
/* Dashboard                                                                  */
/* -------------------------------------------------------------------------- */

def refreshDashboard() {
    def dashboard = getChildDevice(getDashboardDni())

    if (!dashboard) {
        logDebug("Dashboard device does not exist yet.")
        return
    }

    try {
        Map heroes = [:]

        getHeroKeys().each { key ->
            def hero = getChildDevice(getHeroDni(key))

            if (hero) {
                heroes[key] = [
                    name: hero.currentValue("heroName") ?: key.capitalize(),
                    xp: safeNumber(hero.currentValue("xp")),
                    coins: safeNumber(hero.currentValue("coins")),
                    level: safeNumber(hero.currentValue("level")),
                    heroHearts: safeNumber(hero.currentValue("heroHearts")),
                    status: hero.currentValue("status") ?: "Ready"
                ]
            }
        }

        dashboard.updateFamilyData(heroes)

        logInfo("Hero HQ Dashboard refreshed.")
    } catch (Exception e) {
        logWarn("Unable to refresh Hero HQ Dashboard: ${e.message}")
    }
}

/* -------------------------------------------------------------------------- */
/* Public orchestration methods                                               */
/* -------------------------------------------------------------------------- */

def awardCoins(String heroKey, Number amount, String reason = "Parent award") {
    def hero = getHeroByKey(heroKey)

    if (!hero) {
        logWarn("Hero not found: ${heroKey}")
        return
    }

    try {
        hero.awardCoins(amount as Integer, reason)
        refreshDashboard()
    } catch (Exception e) {
        logWarn("Unable to award coins to ${heroKey}: ${e.message}")
    }
}

def deductCoins(String heroKey, Number amount, String reason = "Behavior") {
    def hero = getHeroByKey(heroKey)

    if (!hero) {
        logWarn("Hero not found: ${heroKey}")
        return
    }

    try {
        hero.deductCoins(amount as Integer, reason)
        refreshDashboard()
    } catch (Exception e) {
        logWarn("Unable to deduct coins from ${heroKey}: ${e.message}")
    }
}

def awardXp(String heroKey, Number amount, String reason = "Parent award") {
    def hero = getHeroByKey(heroKey)

    if (!hero) {
        logWarn("Hero not found: ${heroKey}")
        return
    }

    try {
        hero.awardXp(amount as Integer, reason)
        refreshDashboard()
    } catch (Exception e) {
        logWarn("Unable to award XP to ${heroKey}: ${e.message}")
    }
}

def awardHeroHeart(String heroKey, String reason = "Hero Heart") {
    def hero = getHeroByKey(heroKey)

    if (!hero) {
        logWarn("Hero not found: ${heroKey}")
        return
    }

    try {
        hero.awardHeroHeart(reason)
        refreshDashboard()
    } catch (Exception e) {
        logWarn("Unable to award Hero Heart to ${heroKey}: ${e.message}")
    }
}

/* -------------------------------------------------------------------------- */
/* Helpers                                                                    */
/* -------------------------------------------------------------------------- */

private def getHeroByKey(String key) {
    if (!key) {
        return null
    }

    return getChildDevice(getHeroDni(key.toLowerCase()))
}

private List<String> getHeroKeys() {
    List<String> result = []

    if (settings.enableZach != false) {
        result << "zach"
    }

    if (settings.enableJosh != false) {
        result << "josh"
    }

    if (settings.enableCharlie != false) {
        result << "charlie"
    }

    return result
}

private String getHeroDni(String key) {
    return "${app.id}-hero-${key}"
}

private String getDashboardDni() {
    return "${app.id}-dashboard"
}

private Integer safeNumber(def value) {
    if (value == null) {
        return 0
    }

    try {
        return value as Integer
    } catch (Exception e) {
        return 0
    }
}

/* -------------------------------------------------------------------------- */
/* Logging                                                                    */
/* -------------------------------------------------------------------------- */

private void logInfo(String message) {
    if (settings.enableLogging != false) {
        log.info "Hero HQ: ${message}"
    }
}

private void logDebug(String message) {
    if (settings.enableLogging == true) {
        log.debug "Hero HQ: ${message}"
    }
}

private void logWarn(String message) {
    log.warn "Hero HQ: ${message}"
}