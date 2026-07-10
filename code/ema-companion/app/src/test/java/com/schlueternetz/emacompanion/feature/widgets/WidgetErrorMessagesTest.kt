package com.schlueternetz.emacompanion.feature.widgets

import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError
import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetErrorMessagesTest {
    @Test
    fun hourlySource_mapsToTodaySectionStrings() {
        assertEquals(
            R.string.home_today_status_network_error,
            widgetErrorStringRes(FetchError.NETWORK, WidgetDataSource.HOURLY),
        )
        assertEquals(
            R.string.home_today_status_auth_error,
            widgetErrorStringRes(FetchError.AUTH, WidgetDataSource.HOURLY),
        )
        assertEquals(
            R.string.home_today_status_api_error,
            widgetErrorStringRes(FetchError.API, WidgetDataSource.HOURLY),
        )
    }

    @Test
    fun dailySource_mapsToHistorySectionStrings() {
        assertEquals(
            R.string.home_history_status_network_error,
            widgetErrorStringRes(FetchError.NETWORK, WidgetDataSource.DAILY),
        )
        assertEquals(
            R.string.home_history_status_auth_error,
            widgetErrorStringRes(FetchError.AUTH, WidgetDataSource.DAILY),
        )
        assertEquals(
            R.string.home_history_status_api_error,
            widgetErrorStringRes(FetchError.API, WidgetDataSource.DAILY),
        )
    }
}
