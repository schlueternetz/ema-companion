package com.schlueternetz.emacompanion.core.api

import android.util.Log
import com.schlueternetz.emacompanion.feature.settings.SettingsRepository
import kotlinx.coroutines.CancellationException
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
    private val httpClient: OkHttpClient = sharedClient,
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
        ).sign(method = "GET", lastPathSegment = EmaRequestSigner.lastPathSegment(url.encodedPath))

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
        } catch (e: CancellationException) {
            // The fetch was cancelled (e.g. Home left mid-request) — let cancellation propagate
            // instead of disguising it as a network error and breaking structured concurrency.
            throw e
        } catch (e: Exception) {
            // A background fetch must never crash the app. Any other failure executing the
            // request (e.g. a SecurityException when a permission is missing) degrades to a
            // network error so Home shows the banner instead of the process dying — but log it
            // so a genuine bug is not silently disguised as "network down".
            Log.w(TAG, "Unexpected error during EMA fetch; treating as network error", e)
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
            ?: return ApiResult.ApiError() // code 0 but the response shape is unexpected
        // An empty power array is a valid "not producing right now" (e.g. night or before
        // dawn) — that is 0 W, a successful read, not a fetch error.
        if (power.length() == 0) return ApiResult.Success(ProductionSnapshot(0))
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
        private const val TAG = "OkHttpEmaApiClient"

        /**
         * Process-wide shared client. OkHttp is designed to be used as a singleton — sharing one
         * instance reuses its thread pool and connection pool, so repeated fetches reuse the
         * kept-alive TLS connection instead of paying for a fresh handshake (a real cost on
         * older devices, and wasteful when Home is recreated on every tab switch).
         */
        private val sharedClient: OkHttpClient by lazy { OkHttpClient() }
    }
}
