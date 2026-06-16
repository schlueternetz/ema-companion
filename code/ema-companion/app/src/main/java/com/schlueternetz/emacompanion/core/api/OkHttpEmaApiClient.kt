package com.schlueternetz.emacompanion.core.api

import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate

/**
 * OkHttp-backed [EmaApiClient]. Builds and signs the request inline (so the exact signed
 * request can be logged), runs it on [ioDispatcher], and maps the response to an [ApiResult]
 * without letting expected failures escape as exceptions.
 */
class OkHttpEmaApiClient(
    private val settings: SettingsRepository,
    private val httpClient: OkHttpClient = OkHttpClient(),
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val today: () -> String = { LocalDate.now().toString() },
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : EmaApiClient {

    override suspend fun getCurrentProduction(): ProductionFetch = withContext(ioDispatcher) {
        if (!settings.isConfigured()) {
            return@withContext ProductionFetch(ApiResult.ConfigurationError)
        }

        val sid = settings.getEmaSystemId()
        val eid = settings.getEmaEcuId()
        val base = settings.getBaseUrl().let { if (it.endsWith("/")) it else "$it/" }
        val url = base.toHttpUrlOrNull()
            ?.resolve("systems/$sid/devices/ecu/energy/$eid")
            ?.newBuilder()
            ?.addQueryParameter("energy_level", "minutely")
            ?.addQueryParameter("date_range", today())
            ?.build()
            ?: return@withContext ProductionFetch(ApiResult.ApiError())

        val headers = EmaRequestSigner(
            appId = settings.getEmaAppId(),
            appSecret = settings.getEmaAppSecret(),
            clock = clock,
        ).sign(method = "GET", lastPathSegment = eid)

        val request = Request.Builder()
            .url(url)
            .header("X-CA-AppId", headers.appId)
            .header("X-CA-Timestamp", headers.timestamp)
            .header("X-CA-Nonce", headers.nonce)
            .header("X-CA-Signature-Method", headers.signatureMethod)
            .header("X-CA-Signature", headers.signature)
            .get()
            .build()

        val endpoint = url.encodedPath
        val requestText = buildRequestText(request)
        val start = clock()

        try {
            httpClient.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val duration = clock() - start
                val result = parse(response.code, response.isSuccessful, body)
                ProductionFetch(result, endpoint, duration, requestText, body)
            }
        } catch (e: IOException) {
            ProductionFetch(ApiResult.NetworkError, endpoint, clock() - start, requestText, "")
        } catch (e: Exception) {
            // A background fetch must never crash the app. Any other failure executing the
            // request (e.g. a SecurityException when a permission is missing) degrades to a
            // network error so Home shows the banner instead of the process dying.
            ProductionFetch(ApiResult.NetworkError, endpoint, clock() - start, requestText, "")
        }
    }

    private fun parse(httpCode: Int, httpSuccessful: Boolean, body: String): ApiResult<ProductionSnapshot> {
        if (!httpSuccessful) return ApiResult.ApiError(httpStatus = httpCode)
        val json = try {
            JSONObject(body)
        } catch (e: JSONException) {
            return ApiResult.ApiError(httpStatus = httpCode)
        }
        val code = json.optInt("code", -1)
        if (code != 0) return ApiResult.ApiError(code = code)
        val power = json.optJSONObject("data")?.optJSONArray("power")
        if (power == null || power.length() == 0) return ApiResult.ApiError(code = NO_DATA_CODE)
        return ApiResult.Success(ProductionSnapshot(power.getInt(power.length() - 1)))
    }

    private fun buildRequestText(request: Request): String {
        val sb = StringBuilder()
        sb.append("${request.method} ${request.url}\n")
        for (i in 0 until request.headers.size) {
            sb.append("${request.headers.name(i)}: ${request.headers.value(i)}\n")
        }
        return sb.toString().trimEnd()
    }

    companion object {
        /** EMA "no data" business code (manual §4); used when the power array is empty. */
        const val NO_DATA_CODE = 1001
    }
}
