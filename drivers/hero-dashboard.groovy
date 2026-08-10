/**
 * Hero Dashboard
 *
 * Shared family dashboard data source for Hero HQ.
 *
 * v0.1.0
 *
 * Designed for:
 *   - Hubitat C-7
 *   - SharpTools
 *   - Fully Kiosk Browser
 *
 * One device represents the entire family.
 * It exposes JSON/string attributes that can be consumed by
 * SharpTools for the shared Hero HQ dashboard.
 */

import groovy.json.JsonOutput

metadata {
    definition(
        name: "Hero Dashboard",
        namespace: "tp546",
        author: "Tom Prendergast",
        importUrl: "https://raw.githubusercontent.com/tp546/hero-academy/main/drivers/hero-dashboard.groovy"
    ) {
        capability "Initialize"
        capability "Refresh"

        attribute "heroSummary", "string"
        attribute "familyChallenge", "string"
        attribute "leaderboard", "string"
        attribute "pendingApprovals", "number"
        attribute "dashboardJson", "string"
        attribute "lastUpdated", "string"

        command "updateFamilyData", [
            [name: "Family Data", type: "STRING"]
        ]

        command "setFamilyChallenge", [
            [name: "Challenge", type: "STRING"]
        ]

        command "setPendingApprovals", [
            [name: "Count", type: "NUMBER"]
        ]

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
    logInfo("Hero Dashboard installed.")
    initialize()
}

def updated() {
    logInfo("Hero Dashboard updated.")
    initialize()
}

def initialize() {
    if (state.heroes == null) {
        state.heroes = [:]
    }

    if (state.familyChallenge == null) {
        state.familyChallenge = [
            title: "Family Hero Challenge",
            progress: 0,
            target: 1,
            status: "Ready"
        ]
    }

    if (state.pendingApprovals == null) {
        state.pendingApprovals = 0
    }

    updateDashboard()
}

/* -------------------------------------------------------------------------- */
/* Refresh                                                                    */
/* -------------------------------------------------------------------------- */

def refresh() {
    refreshDashboard()
}

def refreshDashboard() {
    updateDashboard()
}

/* -------------------------------------------------------------------------- */
/* Family data                                                                */
/* -------------------------------------------------------------------------- */

/*
 * The parent app calls this method with the current family map.
 *
 * Expected structure:
 *
 * [
 *     zach: [
 *         name: "Zach",
 *         xp: 100,
 *         coins: 50,
 *         level: 2,
 *         heroHearts: 1,
 *         status: "Ready for action"
 *     ],
 *     josh: [...],
 *     charlie: [...]
 * ]
 *
 * The method accepts either a Map or a JSON string.
 */
def updateFamilyData(def familyData) {
    Map data = [:]

    if (familyData instanceof Map) {
        data = familyData
    } else if (familyData instanceof String) {
        try {
            def parsed = new groovy.json.JsonSlurper().parseText(familyData)

            if (parsed instanceof Map) {
                data = parsed as Map
            }
        } catch (Exception e) {
            logWarn("Unable to parse family data: ${e.message}")
            return
        }
    }

    state.heroes = normalizeHeroes(data)

    updateDashboard()

    logInfo(
        "Family dashboard updated with " +
        "${state.heroes.size()} hero profiles."
    )
}

/* -------------------------------------------------------------------------- */
/* Family challenge                                                           */
/* -------------------------------------------------------------------------- */

def setFamilyChallenge(def challenge) {
    Map value = [:]

    if (challenge instanceof Map) {
        value = challenge
    } else if (challenge instanceof String) {
        try {
            def parsed = new groovy.json.JsonSlurper().parseText(challenge)

            if (parsed instanceof Map) {
                value = parsed as Map
            } else {
                value = [
                    title: challenge,
                    progress: 0,
                    target: 1,
                    status: "Active"
                ]
            ]
        } catch (Exception ignored) {
            value = [
                title: challenge,
                progress: 0,
                target: 1,
                status: "Active"
            ]
        }
    }

    state.familyChallenge = [
        title: value.title ?: "Family Hero Challenge",
        progress: safeNumber(value.progress),
        target: Math.max(1, safeNumber(value.target) ?: 1),
        status: value.status ?: "Active"
    ]

    updateDashboard()

    logInfo("Family challenge updated.")
}

/* -------------------------------------------------------------------------- */
/* Pending approvals                                                          */
/* -------------------------------------------------------------------------- */

def setPendingApprovals(Number count) {
    Integer value = Math.max(0, safeNumber(count))

    state.pendingApprovals = value

    sendEvent(
        name: "pendingApprovals",
        value: value
    )

    updateDashboard()
}

/* -------------------------------------------------------------------------- */
/* Dashboard generation                                                       */
/* -------------------------------------------------------------------------- */

private void updateDashboard() {
    Map heroes = state.heroes instanceof Map ? state.heroes : [:]

    Map dashboard = [
        heroes: heroes,
        familyChallenge: state.familyChallenge ?: [:],
        pendingApprovals: safeNumber(state.pendingApprovals),
        leaderboard: buildLeaderboard(heroes),
        generatedAt: new Date().format(
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            location?.timeZone ?: TimeZone.getDefault()
        )
    ]

    String json = JsonOutput.toJson(dashboard)

    sendEvent(
        name: "dashboardJson",
        value: json
    )

    sendEvent(
        name: "heroSummary",
        value: buildHeroSummary(heroes)
    )

    sendEvent(
        name: "familyChallenge",
        value: buildChallengeSummary()
    )

    sendEvent(
        name: "leaderboard",
        value: buildLeaderboardText(heroes)
    )

    sendEvent(
        name: "pendingApprovals",
        value: safeNumber(state.pendingApprovals)
    )

    sendEvent(
        name: "lastUpdated",
        value: new Date().format(
            "yyyy-MM-dd HH:mm:ss",
            location?.timeZone ?: TimeZone.getDefault()
        )
    )
}

/* -------------------------------------------------------------------------- */
/* Hero normalization                                                         */
/* -------------------------------------------------------------------------- */

private Map normalizeHeroes(Map source) {
    Map result = [:]

    source.each { key, value ->

        if (!(value instanceof Map)) {
            return
        }

        String heroKey = key.toString().toLowerCase()

        result[heroKey] = [
            name: value.name ?: heroKey.capitalize(),
            icon: value.icon ?: defaultIcon(heroKey),
            color: value.color ?: defaultColor(heroKey),

            xp: safeNumber(value.xp),
            coins: safeNumber(value.coins),
            level: safeNumber(value.level) ?: 1,

            heroHearts: safeNumber(value.heroHearts),
            pendingApprovals: safeNumber(value.pendingApprovals),
            completedToday: safeNumber(value.completedToday),
            currentStreak: safeNumber(value.currentStreak),

            rank: value.rank ?: "",
            status: value.status ?: "Ready for action"
        ]
    }

    return result
}

/* -------------------------------------------------------------------------- */
/* Hero summary                                                               */
/* -------------------------------------------------------------------------- */

private String buildHeroSummary(Map heroes) {
    if (!heroes || heroes.isEmpty()) {
        return "🦸 Hero HQ • No heroes configured"
    }

    List<String> summaries = []

    orderedHeroKeys(heroes).each { key ->
        Map hero = heroes[key]

        if (!hero) {
            return
        }

        String icon = hero.icon ?: defaultIcon(key)
        String name = hero.name ?: key.capitalize()

        Integer level = safeNumber(hero.level) ?: 1
        Integer coins = safeNumber(hero.coins)
        Integer xp = safeNumber(hero.xp)

        summaries << (
            "${icon} ${name} • " +
            "Lv ${level} • " +
            "${coins} coins • " +
            "${xp} XP"
        )
    }

    return summaries.join(" | ")
}

/* -------------------------------------------------------------------------- */
/* Leaderboard                                                                */
/* -------------------------------------------------------------------------- */

private List buildLeaderboard(Map heroes) {
    if (!heroes || heroes.isEmpty()) {
        return []
    }

    List entries = []

    heroes.each { key, hero ->
        if (!(hero instanceof Map)) {
            return
        }

        entries << [
            key: key,
            name: hero.name ?: key.capitalize(),
            icon: hero.icon ?: defaultIcon(key),
            xp: safeNumber(hero.xp),
            coins: safeNumber(hero.coins),
            level: safeNumber(hero.level) ?: 1
        ]
    }

    entries.sort { a, b ->
        if (a.xp != b.xp) {
            return b.xp <=> a.xp
        }

        return b.level <=> a.level
    }

    entries.eachWithIndex { entry, index ->
        entry.rank = index + 1
    }

    return entries
}

private String buildLeaderboardText(Map heroes) {
    List leaderboard = buildLeaderboard(heroes)

    if (!leaderboard) {
        return "🏆 No heroes yet"
    }

    List<String> lines = []

    leaderboard.each { hero ->
        String medal

        switch (hero.rank) {
            case 1:
                medal = "🥇"
                break

            case 2:
                medal = "🥈"
                break

            case 3:
                medal = "🥉"
                break

            default:
                medal = "${hero.rank}."
                break
        }

        lines << (
            "${medal} ${hero.icon} ${hero.name} " +
            "• Lv ${hero.level} • ${hero.xp} XP"
        )
    }

    return lines.join(" | ")
}

/* -------------------------------------------------------------------------- */
/* Family challenge display                                                   */
/* -------------------------------------------------------------------------- */

private String buildChallengeSummary() {
    Map challenge = state.familyChallenge ?: [:]

    String title = challenge.title ?: "Family Hero Challenge"

    Integer progress = safeNumber(challenge.progress)
    Integer target = Math.max(1, safeNumber(challenge.target) ?: 1)

    String status = challenge.status ?: "Active"

    Integer percent = Math.min(
        100,
        Math.round((progress * 100.0) / target)
    )

    return "🎯 ${title} • ${progress}/${target} • ${percent}% • ${status}"
}

/* -------------------------------------------------------------------------- */
/* Dashboard helpers                                                          */
/* -------------------------------------------------------------------------- */

private List<String> orderedHeroKeys(Map heroes) {
    List<String> order = [
        "zach",
        "josh",
        "charlie"
    ]

    List<String> result = []

    order.each { key ->
        if (heroes.containsKey(key)) {
            result << key
        }
    }

    heroes.keySet().each { key ->
        String normalized = key.toString().toLowerCase()

        if (!result.contains(normalized)) {
            result << normalized
        }
    }

    return result
}

private String defaultIcon(String key) {
    switch (key?.toLowerCase()) {
        case "zach":
            return "🔥"

        case "josh":
            return "⚡"

        case "charlie":
            return "🌟"

        default:
            return "🦸"
    }
}

private String defaultColor(String key) {
    switch (key?.toLowerCase()) {
        case "zach":
            return "blue"

        case "josh":
            return "green"

        case "charlie":
            return "yellow"

        default:
            return "blue"
    }
}

private Integer safeNumber(def value) {
    if (value == null) {
        return 0
    }

    try {
        return value as Integer
    } catch (Exception ignored) {
        return 0
    }
}

/* -------------------------------------------------------------------------- */
/* Logging                                                                    */
/* -------------------------------------------------------------------------- */

private void logInfo(String message) {
    if (settings.enableLogging != false) {
        log.info "Hero Dashboard: ${message}"
    }
}

private void logWarn(String message) {
    log.warn "Hero Dashboard: ${message}"
}