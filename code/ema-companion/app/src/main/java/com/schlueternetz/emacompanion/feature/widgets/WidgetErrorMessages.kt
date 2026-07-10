package com.schlueternetz.emacompanion.feature.widgets

import androidx.annotation.StringRes
import com.schlueternetz.emacompanion.R
import com.schlueternetz.emacompanion.core.api.FetchError

/** Which repository's cache backs a given widget figure, for choosing the right error copy. */
enum class WidgetDataSource {
    HOURLY,
    DAILY,
}

/** Reuses the Home tile's own status-line copy so widget and in-app error text stay consistent. */
@StringRes
fun widgetErrorStringRes(
    error: FetchError,
    source: WidgetDataSource,
): Int =
    when (source) {
        WidgetDataSource.HOURLY ->
            when (error) {
                FetchError.NETWORK -> R.string.home_today_status_network_error
                FetchError.AUTH -> R.string.home_today_status_auth_error
                FetchError.API -> R.string.home_today_status_api_error
            }
        WidgetDataSource.DAILY ->
            when (error) {
                FetchError.NETWORK -> R.string.home_history_status_network_error
                FetchError.AUTH -> R.string.home_history_status_auth_error
                FetchError.API -> R.string.home_history_status_api_error
            }
    }
