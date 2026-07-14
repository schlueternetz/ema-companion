package com.schlueternetz.emacompanion.core.api

/** A [DailyEnergySource] whose throttle `ApiSyncWorker` can also reset (see ADR-010). */
interface DailyEnergySyncSource :
    DailyEnergySource,
    ThrottleResettable
