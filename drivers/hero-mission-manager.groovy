/**
 * Hero Mission Manager Driver
 * Shared command/data device used by the Hero Mission Manager app.
 */

metadata {
    definition(name: "Hero Mission Manager", namespace: "tp546", author: "Tom Prendergast") {
        capability "Initialize"
        capability "Refresh"

        attribute "missionJson", "string"
        attribute "pendingApprovals", "number"
        attribute "status", "string"
        attribute "lastUpdated", "string"
        (1..10).each { n -> attribute "pending${n}", "string" }

        command "updateData", [[name: "Data", type: "STRING"]]
        command "completeMission", [[name: "Hero", type: "STRING"], [name: "Mission", type: "STRING"]]
        command "approveMission", [[name: "Hero", type: "STRING"], [name: "Mission", type: "STRING"]]
        command "rejectMission", [[name: "Hero", type: "STRING"], [name: "Mission", type: "STRING"]]
        command "quickAction", [[name: "Hero", type: "STRING"], [name: "Action", type: "STRING"]]

        command "zachCleanUp"
        command "zachBrushTeethAm"
        command "zachBrushTeethPm"
        command "zachFeedGecko"
        command "zachMakeBed"
        command "zachFighting"
        command "zachTalkingBack"
        command "zachNotListening"

        command "joshCleanUp"
        command "joshBrushTeethAm"
        command "joshBrushTeethPm"
        command "joshFeedGecko"
        command "joshMakeBed"
        command "joshFighting"
        command "joshTalkingBack"
        command "joshNotListening"

        command "charlieCleanUp"
        command "charlieBrushTeethAm"
        command "charlieBrushTeethPm"
        command "charlieFeedGecko"
        command "charlieMakeBed"
        command "charlieFighting"
        command "charlieTalkingBack"
        command "charlieNotListening"

        (1..10).each { n ->
            command "approvePending${n}"
            command "rejectPending${n}"
        }

        command "zachAward", [[name: "Coins", type: "NUMBER"], [name: "XP", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "joshAward", [[name: "Coins", type: "NUMBER"], [name: "XP", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "charlieAward", [[name: "Coins", type: "NUMBER"], [name: "XP", type: "NUMBER"], [name: "Reason", type: "STRING"]]

        command "zachBehavior", [[name: "Coins", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "joshBehavior", [[name: "Coins", type: "NUMBER"], [name: "Reason", type: "STRING"]]
        command "charlieBehavior", [[name: "Coins", type: "NUMBER"], [name: "Reason", type: "STRING"]]
    }
}

def installed() { initialize() }
def updated() { initialize() }

def initialize() {
    if (state.data == null) state.data = "{}"
    updateAttributes()
}

def refresh() {
    parent?.updateManager()
    updateAttributes()
}

def updateData(String data) {
    state.data = data ?: "{}"
    updateAttributes()
}

def completeMission(String hero, String mission) {
    parent?.completeMission(hero, mission)
}

def approveMission(String hero, String mission) {
    parent?.approveMission(hero, mission)
}

def rejectMission(String hero, String mission) {
    parent?.rejectMission(hero, mission)
}

def quickAction(String hero, String action) {
    parent?.quickAction(hero, action)
}

def approvePending(Integer slot) {
    def item = pendingItem(slot)
    if (!item) return
    parent?.approveMission(item.hero.toString(), item.mission.toString())
}

def rejectPending(Integer slot) {
    def item = pendingItem(slot)
    if (!item) return
    parent?.rejectMission(item.hero.toString(), item.mission.toString())
}

def pendingItem(Integer slot) {
    if (!state.data) return null
    try {
        def parsed = new groovy.json.JsonSlurper().parseText(state.data ?: "{}")
        def pending = parsed?.pending
        if (!(pending instanceof List)) return null
        Integer index = (slot ?: 1) - 1
        if (index < 0 || index >= pending.size()) return null
        return pending[index]
    } catch (Exception e) {
        log.warn "Hero Mission Manager: unable to read pending slot ${slot}: ${e.message}"
        return null
    }
}

def approvePending1() { approvePending(1) }
def approvePending2() { approvePending(2) }
def approvePending3() { approvePending(3) }
def approvePending4() { approvePending(4) }
def approvePending5() { approvePending(5) }
def approvePending6() { approvePending(6) }
def approvePending7() { approvePending(7) }
def approvePending8() { approvePending(8) }
def approvePending9() { approvePending(9) }
def approvePending10() { approvePending(10) }

def rejectPending1() { rejectPending(1) }
def rejectPending2() { rejectPending(2) }
def rejectPending3() { rejectPending(3) }
def rejectPending4() { rejectPending(4) }
def rejectPending5() { rejectPending(5) }
def rejectPending6() { rejectPending(6) }
def rejectPending7() { rejectPending(7) }
def rejectPending8() { rejectPending(8) }
def rejectPending9() { rejectPending(9) }
def rejectPending10() { rejectPending(10) }

// Zach

def zachCleanUp() { quickAction("zach", "clean_up") }
def zachBrushTeethAm() { quickAction("zach", "brush_teeth_am") }
def zachBrushTeethPm() { quickAction("zach", "brush_teeth_pm") }
def zachFeedGecko() { quickAction("zach", "feed_gecko") }
def zachMakeBed() { quickAction("zach", "make_bed") }
def zachFighting() { quickAction("zach", "fighting") }
def zachTalkingBack() { quickAction("zach", "talking_back") }
def zachNotListening() { quickAction("zach", "not_listening") }

// Josh

def joshCleanUp() { quickAction("josh", "clean_up") }
def joshBrushTeethAm() { quickAction("josh", "brush_teeth_am") }
def joshBrushTeethPm() { quickAction("josh", "brush_teeth_pm") }
def joshFeedGecko() { quickAction("josh", "feed_gecko") }
def joshMakeBed() { quickAction("josh", "make_bed") }
def joshFighting() { quickAction("josh", "fighting") }
def joshTalkingBack() { quickAction("josh", "talking_back") }
def joshNotListening() { quickAction("josh", "not_listening") }

// Charlie

def charlieCleanUp() { quickAction("charlie", "clean_up") }
def charlieBrushTeethAm() { quickAction("charlie", "brush_teeth_am") }
def charlieBrushTeethPm() { quickAction("charlie", "brush_teeth_pm") }
def charlieFeedGecko() { quickAction("charlie", "feed_gecko") }
def charlieMakeBed() { quickAction("charlie", "make_bed") }
def charlieFighting() { quickAction("charlie", "fighting") }
def charlieTalkingBack() { quickAction("charlie", "talking_back") }
def charlieNotListening() { quickAction("charlie", "not_listening") }

def zachAward(Number coins, Number xp, String reason) {
    def hero = parent?.getHero("zach")
    if (hero) {
        if (xp > 0) hero.awardXp(xp, reason ?: "Quick award")
        if (coins > 0) hero.awardCoins(coins, reason ?: "Quick award")
        parent?.updateManager()
    }
}

def joshAward(Number coins, Number xp, String reason) {
    def hero = parent?.getHero("josh")
    if (hero) {
        if (xp > 0) hero.awardXp(xp, reason ?: "Quick award")
        if (coins > 0) hero.awardCoins(coins, reason ?: "Quick award")
        parent?.updateManager()
    }
}

def charlieAward(Number coins, Number xp, String reason) {
    def hero = parent?.getHero("charlie")
    if (hero) {
        if (xp > 0) hero.awardXp(xp, reason ?: "Quick award")
        if (coins > 0) hero.awardCoins(coins, reason ?: "Quick award")
        parent?.updateManager()
    }
}

def zachBehavior(Number coins, String reason) { applyBehavior("zach", coins, reason) }
def joshBehavior(Number coins, String reason) { applyBehavior("josh", coins, reason) }
def charlieBehavior(Number coins, String reason) { applyBehavior("charlie", coins, reason) }

def applyBehavior(String heroKey, Number coins, String reason) {
    def hero = parent?.getHero(heroKey)
    if (hero && coins > 0) {
        hero.deductCoins(coins, reason ?: "Behavior")
        parent?.updateManager()
    }
}

def updateAttributes() {
    sendEvent(name: "missionJson", value: state.data ?: "{}")

    Integer pending = 0
    List pendingItems = []
    def parsed = null

    try {
        parsed = new groovy.json.JsonSlurper().parseText(state.data ?: "{}")
        if (parsed?.pending instanceof List) {
            pendingItems = parsed.pending
            pending = pendingItems.size()
        }
    } catch (Exception ignored) {
        pending = 0
        pendingItems = []
    }

    sendEvent(name: "pendingApprovals", value: pending)
    sendEvent(name: "status", value: pending > 0 ? "${pending} approval${pending == 1 ? '' : 's'} pending" : "Ready for action")
    sendEvent(name: "lastUpdated", value: new Date().format("yyyy-MM-dd HH:mm:ss"))

    (1..10).each { n ->
        String value = ""
        Integer index = n - 1
        if (index < pendingItems.size()) {
            def item = pendingItems[index]
            def mission = null
            if (parsed?.missions instanceof Map) {
                mission = parsed.missions.get(item?.mission?.toString())
            }

            String hero = item?.hero?.toString() ?: ""
            String missionId = item?.mission?.toString() ?: ""
            String name = mission?.name?.toString() ?: missionId
            String xp = mission?.xp?.toString() ?: "0"
            String coins = mission?.coins?.toString() ?: "0"
            value = "${hero}|${missionId}|${name}|${xp}|${coins}"
        }
        sendEvent(name: "pending${n}", value: value)
    }
}
