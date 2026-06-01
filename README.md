# ema-companion

An Android app to provide missing features of the [APsystems EMA app](https://play.google.com/store/apps/details?id=com.apsemaappforandroid) for solar arrays.

## Overview
The EMA Companion app is designed to work in combination with the existing EMA App (it does not replace it). It adds additional features such as additional production stats and graphs, home screen widgets, and notifications for alerts.

```mermaid
C4Context
    title EMA Companion – System Context

    Person(owner, "APsystems Solar Owner")

    Boundary(device, "Android Phone or Tablet") {
        System_Ext(emaApp, "EMA App", "- Production<br/>- Module<br/>- Data (Reports)<br/>- Settings")
        System(companion, "EMA Companion", "- Production<br/>- Widgets<br/>- Notifications")
    }

    System_Ext(emaApi, "EMA API")

    Rel(owner, emaApp, "Uses")
    Rel(owner, companion, "Uses")
    Rel(emaApp, emaApi, "Retrieves data")
    Rel(companion, emaApi, "Retrieves data")

    UpdateLayoutConfig($c4ShapeInRow, "2", $c4BoundaryInRow, "1")
```

This project is currently in early development.

## Getting Started

_Setup instructions will be added as the project takes shape._

## Development
This project uses [OpenSpec](https://openspec.dev/) for functional development.
All application code is stored in `code/` and generated using Claude Code.
