/**
 * Hero Dashboard - Hubitat C-7
 * Shared family dashboard data source for SharpTools/Fully Kiosk.
 */

metadata {
    definition(name: "Hero Dashboard", namespace: "tp546", author: "Tom Prendergast") {
        capability "Initialize"
        capability "Refresh"

        attribute "heroSummary", "string"
        attribute "familyChallenge", "string"
        attribute "leaderboard", "string"
        attribute "pendingApprovals", "number"
        attribute "dashboardJson", "string"
        attribute "lastUpdated", "string"

        command "updateFamilyData", [[name: "Family Data", type: "STRING"]]
        command "setFamilyChallenge", [[name: "Challenge", type: "STRING"]]
        command "setPendingApprovals", [[name: "Count", type: "NUMBER"]]
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
    if (state.heroes == null) state.heroes = [:]
    if (state.familyChallenge == null) {
        state.familyChallenge = [title: "Family Hero Challenge", progress: 0, target: 1, status: "Ready"]
    }
    if (state.pendingApprovals == null) state.pendingApprovals = 0
    updateDashboard()
}

def refresh() {
    updateDashboard()
}

def refreshDashboard() {
    updateDashboard()
}

def updateFamilyData(String familyData) {
    Map data = [:]
    try {
        def parsed = new groovy.json.JsonSlurper().parseText(familyData ?: "{}")
        if (parsed instanceof Map) data = parsed as Map
    } catch (Exception e) {
        logWarn("Unable to parse family data: ${e.message}")
        return
    }

    state.heroes = normalizeHeroes(data)
    updateDashboard()
}

def setFamilyChallenge(String challenge) {
    Map value = [:]
    try {
        def parsed = new groovy.json.JsonSlurper().parseText(challenge ?: "{}")
        if (parsed instanceof Map) value = parsed as Map
    } catch (Exception e) {
        value = [title: challenge ?: "Family Hero Challenge", progress: 0, target: 1, status: "Active"]
    }

    state.familyChallenge = [
        title: value.title ?: "Family Hero Challenge",
        progress: numberValue(value.progress),
        target: Math.max(1, numberValue(value.target)),
        status: value.status ?: "Active"
    ]
    updateDashboard()
}

def setPendingApprovals(Number count) {
    state.pendingApprovals = Math.max(0, numberValue(count))
    updateDashboard()
}

def updateDashboard() {
    Map heroes = state.heroes instanceof Map ? state.heroes : [:]
    List leaderboard = buildLeaderboard(heroes)

    Map dashboard = [
        heroes: heroes,
        familyChallenge: state.familyChallenge ?: [:],
        pendingApprovals: numberValue(state.pendingApprovals),
        leaderboard: leaderboard,
        generatedAt: new Date().format("yyyy-MM-dd HH:mm:ss")
    ]

    sendEvent(name: "dashboardJson", value: groovy.json.JsonOutput.toJson(dashboard))
    sendEvent(name: "heroSummary", value: buildHeroSummary(heroes))
    sendEvent(name: "familyChallenge", value: buildChallengeSummary())
    sendEvent(name: "leaderboard", value: buildLeaderboardText(heroes))
    sendEvent(name: "pendingApprovals", value: numberValue(state.pendingApprovals))
    sendEvent(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss"))
}

def normalizeHeroes(Map source) {
    Map result = [:]

    source.each { key, value ->
        if (value instanceof Map) {
            String heroKey = key.toString().toLowerCase()
            result[heroKey] = [
                name: value.name ?: heroKey.capitalize(),
                icon: value.icon ?: defaultIcon(heroKey),
                color: value.color ?: defaultColor(heroKey),
                xp: numberValue(value.xp),
                coins: numberValue(value.coins),
                level: numberValue(value.level) ?: 1,
                heroHearts: numberValue(value.heroHearts),
                pendingApprovals: numberValue(value.pendingApprovals),
                completedToday: numberValue(value.completedToday),
                currentStreak: numberValue(value.currentStreak),
                rank: value.rank ?: "",
                status: value.status ?: "Ready for action"
            ]
        }
    }

    return result
}

def buildHeroSummary(Map heroes) {
    if (!heroes || heroes.isEmpty()) return "🦸 Hero HQ • No heroes configured"

    List parts = []
    orderedHeroKeys(heroes).each { key ->
        Map hero = heroes[key]
        if (hero) {
            parts << "${hero.icon} ${hero.name} • Lv ${hero.level} • ${hero.coins} coins • ${hero.xp} XP"
        }
    }
    return parts.join(" | ")
}

def buildLeaderboard(Map heroes) {
    List entries = []
    heroes.each { key, hero ->
        if (hero instanceof Map) {
            entries << [
                key: key,
                name: hero.name ?: key.toString().capitalize(),
                icon: hero.icon ?: defaultIcon(key.toString()),
                xp: numberValue(hero.xp),
                coins: numberValue(hero.coins),
                level: numberValue(hero.level) ?: 1
            ]
        }
    }

    entries.sort { a, b ->
        if (a.xp != b.xp) return b.xp <=> a.xp
        return b.level <=> a.level
    }

    Integer position = 1
    entries.each { it.rank = position++ }
    return entries
}

def buildLeaderboardText(Map heroes) {
    List leaderboard = buildLeaderboard(heroes)
    if (!leaderboard) return "🏆 No heroes yet"

    List lines = []
    leaderboard.each { hero ->
        String marker = hero.rank == 1 ? "🥇" : hero.rank == 2 ? "🥈" : hero.rank == 3 ? "🥉" : "${hero.rank}."
        lines << "${marker} ${hero.icon} ${hero.name} • Lv ${hero.level} • ${hero.xp} XP"
    }
    return lines.join(" | ")
}

def buildChallengeSummary() {
    Map challenge = state.familyChallenge ?: [:]
    Integer progress = numberValue(challenge.progress)
    Integer target = Math.max(1, numberValue(challenge.target))
    Integer percent = Math.min(100, Math.round((progress * 100.0) / target))
    return "🎯 ${challenge.title ?: 'Family Hero Challenge'} • ${progress}/${target} • ${percent}% • ${challenge.status ?: 'Active'}"
}

def orderedHeroKeys(Map heroes) {
    List result = []
    ["zach", "josh", "charlie"].each { key ->
        if (heroes.containsKey(key)) result << key
    }
    heroes.keySet().each { key ->
        String normalized = key.toString().toLowerCase()
        if (!result.contains(normalized)) result << normalized
    }
    return result
}

def defaultIcon(String key) {
    if (key == "zach") return "🔥"
    if (key == "josh") return "⚡"
    if (key == "charlie") return "🌟"
    return "🦸"
}

def defaultColor(String key) {
    if (key == "zach") return "blue"
    if (key == "josh") return "green"
    if (key == "charlie") return "yellow"
    return "blue"
}

def numberValue(def value) {
    try {
        return value == null ? 0 : value as Integer
    } catch (Exception e) {
        return 0
    }
}

def logInfo(String message) {
    if (settings.enableLogging != false) log.info "Hero Dashboard: ${message}"
}

def logWarn(String message) {
    log.warn "Hero Dashboard: ${message}"
}
