/**
 * Hero Mission Manager Driver
 *
 * Shared command/data device used by the Hero Mission Manager app.
 *
 * v0.1.0
 */

metadata {
    definition(name: "Hero Mission Manager", namespace: "tp546", author: "Tom Prendergast") {
        capability "Initialize"
        capability "Refresh"

        attribute "missionJson", "string"
        attribute "pendingApprovals", "number"
        attribute "status", "string"
        attribute "lastUpdated", "string"

        command "updateData", [[name: "Data", type: "STRING"]]
        command "completeMission", [[name: "Hero", type: "STRING"], [name: "Mission", type: "STRING"]]
        command "approveMission", [[name: "Hero", type: "STRING"], [name: "Mission", type: "STRING"]]
        command "rejectMission", [[name: "Hero", type: "STRING"], [name: "Mission", type: "STRING"]]

        command "zachAward", [[name: "Coins", type: "NUMBER"], [name: "XP", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "joshAward", [[name: "Coins", type: "NUMBER"], [name: "XP", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "charlieAward", [[name: "Coins", type: "NUMBER"], [name: "XP", type: "NUMBER"], [name: "Reason", type: "STRING"]]

        command "zachBehavior", [[name: "Coins", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "joshBehavior", [[name: "Coins", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "charlieBehavior", [[name: "Coins", type: "NUMBER"], [name: "Reason", type: "STRING"]]
    }
}

def installed() {
    initialize()
}

def updated() {
    initialize()
}

def initialize() {
    if (state.data == null) state.data = "{}"
    updateAttributes()
}

def refresh() {
    updateAttributes()
}

def updateData(String data) {
    state.data = data ?: "{}"
    updateAttributes()
}

def completeMission(String hero, String mission) {
    logInfo("Mission completion requested: ${hero}/${mission}")
    sendEvent(name: "status", value: "Mission submitted")
}

def approveMission(String hero, String mission) {
    logInfo("Mission approval requested: ${hero}/${mission}")
    sendEvent(name: "status", value: "Mission approved")
}

def rejectMission(String hero, String mission) {
    logInfo("Mission rejection requested: ${hero}/${mission}")
    sendEvent(name: "status", value: "Mission rejected")
}

def zachAward(Number coins, Number xp, String reason) {
    logInfo("Quick award Zach: ${coins} coins / ${xp} XP / ${reason}")
}

def joshAward(Number coins, Number xp, String reason) {
    logInfo("Quick award Josh: ${coins} coins / ${xp} XP / ${reason}")
}

def charlieAward(Number coins, Number xp, String reason) {
    logInfo("Quick award Charlie: ${coins} coins / ${xp} XP / ${reason}")
}

def zachBehavior(Number coins, String reason) {
    logInfo("Quick behavior Zach: -${coins} coins / ${reason}")
}

def joshBehavior(Number coins, String reason) {
    logInfo("Quick behavior Josh: -${coins} coins / ${reason}")
}

def charlieBehavior(Number coins, String reason) {
    logInfo("Quick behavior Charlie: -${coins} coins / ${reason}")
}

def updateAttributes() {
    sendEvent(name: "missionJson", value: state.data ?: "{}")

    Integer pending = 0
    try {
        def parsed = new groovy.json.JsonSlurper().parseText(state.data ?: "{}")
        if (parsed?.pending instanceof List) pending = parsed.pending.size()
    } catch (Exception ignored) {
        pending = 0
    }

    sendEvent(name: "pendingApprovals", value: pending)
    sendEvent(name: "status", value: pending > 0 ? "${pending} approval${pending == 1 ? '' : 's'} pending" : "Ready for action")
    sendEvent(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss"))
}

def logInfo(String message) {
    log.info "Hero Mission Manager: ${message}"
}
