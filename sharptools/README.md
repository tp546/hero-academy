# Hero Academy — SharpTools Dashboard

Target: a single 16:9 landscape SharpTools dashboard for the whole family.

## Hubitat device

Use the existing **Hero Mission Manager** device. The action command is:

`quickAction(Hero, Action)`

Hero values:
- `zach`
- `josh`
- `charlie`

Action values:
- `clean_up`
- `brush_teeth_am`
- `brush_teeth_pm`
- `feed_gecko`
- `make_bed`
- `fighting`
- `talking_back`
- `not_listening`

## 16:9 layout

Use a 24-column x 13-row conceptual grid. SharpTools may express this through its own tile sizing controls; the JSON blueprint in `hero-academy-dashboard.json` is the source of truth for relative placement.

### Header

1. Hero Academy / Mission Control
2. Parent Approvals → `pendingApprovals`
3. Family Coins → sum of the three Hero Profile `coins` attributes
4. Family XP → sum of the three Hero Profile `xp` attributes
5. Family Challenge → reserve for the family challenge tile

### Hero cards

Three equal-width cards:

- Zach — red accent
- Josh — blue accent
- Charlie — gold accent

Each card contains the five positive actions followed by the three negative behaviors.

Positive actions should call `quickAction(hero, actionId)` and are approval-based.

Negative behaviors should call `quickAction(hero, actionId)` and apply immediately.

### Bottom row

- Parent Approvals
- XP Leaderboard
- Family Challenge

## SharpTools implementation note

SharpTools dashboard layouts are managed by the SharpTools account/UI rather than imported as a portable JSON document through the Hubitat integration. Therefore `hero-academy-dashboard.json` is intentionally a **build blueprint**, not a fake SharpTools import file.

The blueprint lets the dashboard be reproduced consistently without guessing tile positions, commands, action IDs, rewards, or colors.

## Child artwork

The intended visual treatment is superhero avatars with blonde hair and blue eyes for Zach, Josh, and Charlie. Use the generated 16:9 mockup as the visual reference when creating image/icon tiles.
