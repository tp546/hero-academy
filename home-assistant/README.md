# Hero Academy — Home Assistant Dashboard

The active dashboard target is Home Assistant, not SharpTools.

Dashboard URL:

`http://10.13.18.215:8123/dashboard/hero-academy`

## Dashboard file

`hero-academy-dashboard.yaml` contains the 16:9/landscape Hero Academy layout using the existing Hubitat entities exposed to Home Assistant.

## Important

The YAML assumes the Hubitat integration exposes the existing Hero Mission Manager quick-action button entities with names matching the button IDs in the dashboard. If Home Assistant used different entity IDs, replace only those entity IDs; do not change the Hubitat driver.

The dashboard also assumes these Hero Profile sensors exist for each child:

- `<hero>_hero_profile_rank`
- `<hero>_hero_profile_level`
- `<hero>_hero_profile_xp`
- `<hero>_hero_profile_coins`
- `<hero>_hero_profile_completed_today`
- `<hero>_hero_profile_pending_approvals`
- `<hero>_hero_profile_today_good`
- `<hero>_hero_profile_today_bad`
- `<hero>_hero_profile_today_xp_earned`
- `<hero>_hero_profile_today_coin_change`
- `<hero>_hero_profile_activity1` through `activity12`

The custom parent activity command is implemented in the Hubitat backend as `recordActivity`. The dashboard UI should call that command through whatever Home Assistant Hubitat command service/entity is available in the user's installation; the command is deliberately not routed through the mission approval queue.

## Custom cards

The dashboard uses Mushroom template cards. Install the Mushroom frontend cards through HACS if they are not already installed. Standard Home Assistant cards can be substituted if desired.
