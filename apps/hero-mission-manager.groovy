/**
 * Hero Mission Manager
 *
 * v0.1.0
 * Central mission/chore/behavior engine for Hero HQ.
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

def installed() {
    initialize()
}

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
    def child = getChildDevice(dni)
    if (child) return

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
    return [id: id, icon: icon, name: name, xp: xp, coins: coins, requiresApproval: approval]
}

def getMission(String missionId) {
    seedMissions()
    return state.missions[missionId]
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
        if (mission.xp > 0) hero.awardXp(mission.xp, mission.name)
        else if (mission.xp < 0) hero.setXp(Math.max(0, numberValue(hero.currentValue("xp")) + mission.xp))

        if (mission.coins > 0) hero.awardCoins(mission.coins, mission.name)
        else if (mission.coins < 0) hero.deductCoins(Math.abs(mission.coins), mission.name)

        Integer completed = numberValue(hero.currentValue("completedToday"))
        if (mission.xp > 0) hero.setCompletedToday(completed + 1)

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
    return groovy.json.JsonOutput.toJson([
        missions: state.missions,
        pending: state.pending ?: [],
        heroes: [zach: heroSnapshot(settings.zach), josh: heroSnapshot(settings.josh), charlie: heroSnapshot(settings.charlie)]
    ])
}

def heroSnapshot(def hero) {
    if (!hero) return [:]
    return [
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
