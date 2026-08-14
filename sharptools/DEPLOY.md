# Hero Academy — SharpTools Deployment

## Backend update

Install/save these three updated Hubitat files before building the dashboard:

1. `drivers/hero-profile.groovy`
2. `drivers/hero-mission-manager.groovy`
3. `apps/hero-mission-manager.groovy`

The first two are device drivers. The third is the app that owns the mission logic.

## Hubitat

1. Open **Drivers Code** and update **Hero Profile**.
2. Open **Drivers Code** and update **Hero Mission Manager**.
3. Open **Apps Code** and update **Hero Mission Manager**.
4. Open the installed **Hero Mission Manager** app and click **Done/Save** so the updated child device is refreshed.
5. Confirm the app still has the three hero profile devices selected.
6. Open the **Hero Mission Manager** device. The new command should be visible as:
   - `recordActivity`
   - Arguments: Hero, Type, Title, XP, Coins, Reason

## SharpTools dashboard

The dashboard blueprint is `hero-academy-dashboard.json`.

The target is a 16:9 landscape dashboard with:

- One shared quick-action area for all three heroes.
- Three hero cards.
- Parent approvals.
- XP leaderboard.
- Recent activity.
- Parent custom activity form.

### Custom activity form

Build a form/button group that sends this command to the **Hero Mission Manager** device:

`recordActivity(Hero, Type, Title, XP, Coins, Reason)`

Recommended controls:

- **Hero:** Zach / Josh / Charlie
- **Type:** Good / Bad
- **Description:** free text
- **XP:** numeric
- **Coins:** numeric
- **Submit:** sends the command

For a **Good** entry, enter positive XP/coin values.
For a **Bad** entry, enter the positive size of the penalty; the backend converts it to a deduction.

Examples:

- Zach / Good / Helped clean the kitchen / 10 / 10
- Josh / Bad / Fighting with brother / 5 / 5
- Charlie / Good / Helped without being asked / 5 / 5

## Activity history

Each hero stores the most recent 100 activities. The latest 12 are exposed as `activity1` through `activity12` and are included in `dashboardJson`.

Each activity contains:

- timestamp
- type
- title
- XP change
- coin change
- reason

Daily dashboard attributes include:

- `todayGood`
- `todayBad`
- `todayXpEarned`
- `todayCoinChange`

## Important

`hero-academy-dashboard.json` is a **build blueprint**, not a native SharpTools import file. SharpTools dashboard layouts are managed by the SharpTools account/UI and are not directly deployable through the Hubitat GitHub integration.
