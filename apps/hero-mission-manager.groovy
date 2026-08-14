/**
 * Hero Mission Manager
 * Central mission, chore, behavior, approval, and family quick-action engine.
 */

definition(
    name: "Hero Mission Manager",
    namespace: "tp546",
    author: "Tom Prendergast",
    description: "Mission, chore, behavior, approval, and quick-action engine for Hero HQ.",
    category: "Convenience",
    iconUrl: "https://raw.githubusercontent.com/tp546/hero-academy/main/images/hero-hq.png",
    iconX2Url: "https://raw.githubusercontent.com/tp546/hero-academy/main/images/hero-hq.png",
    iconX3Url: "https://raw.githubusercontent.com/tp546/hero-academy/main/images/hero-hq.png"
)

preferences {
    page(name: "mainPage", title: "🦸 Hero Mission Manager", install: true, uninstall: true) {
        section("Hero Profiles") {
            input name: "zach", type: "capability.refresh", title: "Zach Hero Profile", required: false, multiple: false
            input name: "josh", type: "capability.refresh", title: "Josh Hero Profile", required: false, multiple: false
            input name: "charlie", type: "capability.refresh", title: "Charlie Hero Profile", required: false, multiple: false
        }
        section("Mission Manager Device") {
            input name: "managerName", type: "text", title: "Device name", defaultValue: "Hero Mission Manager", required: false
        }
        section("Logging") {
            input name: "enableLogging", type: "bool", title: "Enable logging", defaultValue: true, required: false
        }
    }
}

def installed() { initialize() }

def updated() {
    unsubscribe()
    initialize()
}

def initialize() {
    createManagerDevice()
    seedMissions()
    updateManager()
}

def createManagerDevice() {
    String dni = "${app.id}-manager"
    if (getChildDevice(dni)) return
    try {
        addChildDevice("tp546", "Hero Mission Manager", dni, [
            name: settings.managerName ?: "Hero Mission Manager",
            label: settings.managerName ?: "Hero Mission Manager",
            isComponent: false
        ])
    } catch (Exception e) {
        log.warn "Hero Mission Manager: unable to create manager device: ${e.message}"
    }
}

def seedMissions() {
    if (state.missions != null) return
    state.missions = [
        clean_up: mission("clean_up", "🧹", "Clean up after yourself", 5, 5, true),
        brush_teeth_am: mission("brush_teeth_am", "🪥", "Brush teeth — morning", 3, 3, true),
        brush_teeth_pm: mission("brush_teeth_pm", "🪥", "Brush teeth — night", 3, 3, true),
        feed_gecko: mission("feed_gecko", "🦎", "Feed gecko", 5, 5, true),
        make_bed: mission("make_bed", "🛏️", "Make your bed", 3, 3, true),
        fighting: mission("fighting", "👊", "Fighting with siblings", -5, -5, false),
        talking_back: mission("talking_back", "🗣️", "Talking back to parents", -5, -5, false),
        not_listening: mission("not_listening", "👂", "Not listening to parents", -5, -5, false)
    ]
}

def mission(String id, String icon, String name, Integer xp, Integer coins, Boolean approval) {
    [id: id, icon: icon, name: name, xp: xp, coins: coins, requiresApproval: approval]
}

def getMission(String missionId) {
    seedMissions()
    state.missions[missionId]
}

def getHero(String heroKey) {
    if (!heroKey) return null
    switch (heroKey.toLowerCase()) {
        case "zach": return settings.zach
        case "josh": return settings.josh
        case "charlie": return settings.charlie
        default: return null
    }
}

def quickAction(String heroKey, String action) {
    String key = heroKey?.toLowerCase()
    String actionKey = action?.toLowerCase()?.trim()
    if (!getHero(key) || !getMission(actionKey)) {
        log.warn "Hero Mission Manager: unknown quick action ${heroKey}/${action}"
        return
    }

    def mission = getMission(actionKey)
    if (mission.requiresApproval) {
        completeMission(key, actionKey)
    } else {
        applyMission(getHero(key), mission, key)
    }
}

def completeMission(String heroKey, String missionId) {
    def hero = getHero(heroKey)
    def mission = getMission(missionId)
    if (!hero || !mission) return

    if (mission.requiresApproval) {
        addPending(heroKey, missionId)
        syncPendingCounts()
        updateManager()
        logInfo("${heroKey} submitted ${mission.name} for parent approval.")
        return
    }

    applyMission(hero, mission, heroKey)
}

def approveMission(String heroKey, String missionId) {
    def hero = getHero(heroKey)
    def mission = getMission(missionId)
    if (!hero || !mission) return

    removePending(heroKey, missionId)
    syncPendingCounts()
    applyMission(hero, mission, heroKey)
}

def rejectMission(String heroKey, String missionId) {
    removePending(heroKey, missionId)
    syncPendingCounts()
    updateManager()
    logInfo("Rejected ${missionId} for ${heroKey}.")
}

def applyMission(def hero, Map mission, String heroKey) {
    try {
        Integer appliedXp = numberValue(mission.xp)
        Integer appliedCoins = numberValue(mission.coins)

        if (appliedXp > 0) hero.awardXp(appliedXp, mission.name)
        else if (appliedXp < 0) hero.setXp(Math.max(0, numberValue(hero.currentValue("xp")) + appliedXp))

        if (appliedCoins > 0) hero.awardCoins(appliedCoins, mission.name)
        else if (appliedCoins < 0) hero.deductCoins(Math.abs(appliedCoins), mission.name)

        // Keep every completed/penalized mission in the same activity history
        // used by the custom parent-entry system and SharpTools dashboard.
        String activityType = appliedXp < 0 || appliedCoins < 0 ? "bad" : "mission"
        hero.addActivity(activityType, mission.name, appliedXp, appliedCoins, mission.name)

        Integer completed = numberValue(hero.currentValue("completedToday"))
        if (appliedXp > 0) hero.setCompletedToday(completed + 1)

        syncPendingCounts()
        updateManager()
        logInfo("Applied ${mission.name} to ${heroKey}.")
    } catch (Exception e) {
        log.warn "Hero Mission Manager: unable to apply mission: ${e.message}"
    }
}

def addPending(String heroKey, String missionId) {
    if (state.pending == null) state.pending = []
    Boolean exists = state.pending.any { it.hero == heroKey.toLowerCase() && it.mission == missionId }
    if (!exists) state.pending << [hero: heroKey.toLowerCase(), mission: missionId]
}

def removePending(String heroKey, String missionId) {
    if (state.pending == null) state.pending = []
    state.pending = state.pending.findAll { !(it.hero == heroKey.toLowerCase() && it.mission == missionId) }
}

def syncPendingCounts() {
    Map counts = [zach: 0, josh: 0, charlie: 0]
    (state.pending ?: []).each { item ->
        String key = item.hero?.toString()?.toLowerCase()
        if (counts.containsKey(key)) counts[key] = counts[key] + 1
    }
    if (settings.zach) settings.zach.setPendingApprovals(counts.zach)
    if (settings.josh) settings.josh.setPendingApprovals(counts.josh)
    if (settings.charlie) settings.charlie.setPendingApprovals(counts.charlie)
}

def updateManager() {
    def device = getChildDevice("${app.id}-manager")
    if (!device) return
    device.updateData(buildDashboardData())
}

def buildDashboardData() {
    seedMissions()
    groovy.json.JsonOutput.toJson([
        quickActions: [
            [id: "clean_up", label: "🧹 Clean up", approval: true],
            [id: "brush_teeth_am", label: "🪥 Teeth AM", approval: true],
            [id: "brush_teeth_pm", label: "🪥 Teeth PM", approval: true],
            [id: "feed_gecko", label: "🦎 Feed gecko", approval: true],
            [id: "make_bed", label: "🛏️ Make bed", approval: true],
            [id: "fighting", label: "👊 Fighting", approval: false],
            [id: "talking_back", label: "🗣️ Talking back", approval: false],
            [id: "not_listening", label: "👂 Not listening", approval: false]
        ],
        missions: state.missions,
        pending: state.pending ?: [],
        heroes: [zach: heroSnapshot(settings.zach), josh: heroSnapshot(settings.josh), charlie: heroSnapshot(settings.charlie)]
    ])
}

def heroSnapshot(def hero) {
    if (!hero) return [:]
    [
        name: hero.currentValue("heroName") ?: hero.label,
        xp: numberValue(hero.currentValue("xp")),
        coins: numberValue(hero.currentValue("coins")),
        level: numberValue(hero.currentValue("level")),
        pendingApprovals: numberValue(hero.currentValue("pendingApprovals")),
        completedToday: numberValue(hero.currentValue("completedToday"))
    ]
}

def numberValue(def value) {
    try { return value == null ? 0 : value as Integer } catch (Exception e) { return 0 }
}

def logInfo(String message) {
    if (settings.enableLogging != false) log.info "Hero Mission Manager: ${message}"
}
