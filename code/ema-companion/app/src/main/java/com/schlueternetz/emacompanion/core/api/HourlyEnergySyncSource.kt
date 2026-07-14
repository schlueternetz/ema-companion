package com.schlueternetz.emacompanion.core.api

/** An [HourlyEnergySource] whose throttle `ApiSyncWorker` can also reset (see ADR-010). */
interface HourlyEnergySyncSource :
    HourlyEnergySource,
    ThrottleResettable
