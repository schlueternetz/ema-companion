[User Guide](user-guide.md) › Home-Screen Widgets

# Home-Screen Widgets

EMA Companion offers three Android home-screen widgets so you can glance at your solar production without opening the app. Add them the same way as any other widget: long-press an empty area of your home screen, choose **Widgets**, find **EMA Companion**, then drag one onto your home screen.

## Today's Production

Shows today's hourly production as a line chart (06:00 through the current hour) plus a running total in kWh. Completed hours are a solid line; the current, still-accumulating hour is dashed. The Y-axis maximum follows your configured System Capacity, the same as the equivalent Home screen chart.

## Production Summary

Shows three bold figures at a glance: **Today**, **This Month**, and **Last 30 Days**, each in kWh. "Today" comes from the same hourly data as the Today's Production widget; the other two come from your daily production history.

## Production History

Shows a bar chart of daily totals over your configured history window (Settings → Historic Data Days), colour-coded by calendar month — the same chart as the Home screen's Production History tile, sized for a widget.

## Not configured yet

If you haven't entered your EMA credentials in Settings, every widget shows a neutral **"Not configured — open EMA Companion"** message instead of a chart or figures. Tapping the widget opens the app directly to Settings so you can finish setup.

## When a fetch fails

Widgets always show either fresh data or a clear reason it's missing — never a stale-looking chart with no explanation. If the latest attempt to fetch data failed, the affected content is replaced with a short message:

- **Network issue — couldn't update** — the app could not reach the EMA service
- **Authentication failed — check your API credentials** — your credentials were rejected
- **Couldn't update production data** — another error, such as a server problem

On the Production Summary widget, only the affected figure(s) show the error — an hourly failure only replaces "Today"; a daily failure only replaces "This Month" and "Last 30 Days". The message clears automatically as soon as the next fetch succeeds.

While a widget is showing an error (or the app isn't configured yet), tapping it opens Settings instead of Home, so you can check your credentials or the API call log directly.

## Appearance

Each widget follows the Display Mode preference you've set in Settings (System, Light, or Dark) — the same choice already applied inside the app. If you change Display Mode, widgets update their colours the next time they refresh.

## Staying up to date

Widgets refresh automatically in the background approximately every two hours, and immediately whenever you open the app or change a connection setting (credentials, Base URL, import, or factory reset) in Settings. Background refreshes share the same cached data and API call budget as the in-app tiles — placing widgets does not use any additional API calls beyond what the app already makes.
