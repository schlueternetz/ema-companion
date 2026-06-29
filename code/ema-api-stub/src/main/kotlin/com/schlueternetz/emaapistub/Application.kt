package com.schlueternetz.emaapistub

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.httpMethod
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.serialization.Serializable
import java.io.File

const val DEFAULT_PORT = 8080

/** Path template for the implemented ECU energy-in-period endpoint. */
const val ECU_ENERGY_PATH = "/user/api/v2/systems/{sid}/devices/ecu/energy/{eid}"

@Serializable
data class ActualRequest(
    val method: String,
    val path: String,
    val pathParams: Map<String, String>,
    val query: Map<String, String>,
)

@Serializable
data class UnexpectedCallDiagnostic(
    val reason: String,
    val expected: RequestMatcher?,
    val actual: ActualRequest,
    val error: String = "unexpected_call",
)

/** Installs content negotiation and the EMA + control routes, dispatching to [engine]. */
fun Application.stubModule(engine: MatchingEngine) {
    install(ContentNegotiation) { json() }
    routing {
        // ECU energy in period (the implemented EMA endpoint). The ECU id is the {eid} param.
        get(ECU_ENERGY_PATH) {
            val pathParams =
                buildMap {
                    call.parameters["sid"]?.let { put("sid", it) }
                    call.parameters["eid"]?.let { put("eid", it) }
                }
            val query = call.request.queryParameters.entries().associate { it.key to it.value.first() }
            val request =
                IncomingRequest(
                    method = call.request.httpMethod.value,
                    path = ECU_ENERGY_PATH,
                    pathParams = pathParams,
                    query = query,
                )
            val ecuId = pathParams["eid"].orEmpty()

            when (val result = engine.handle(ecuId, request)) {
                is MatchResult.Matched ->
                    call.respondText(
                        result.body.toString(),
                        ContentType.Application.Json,
                        HttpStatusCode.fromValue(result.status),
                    )

                is MatchResult.Unexpected ->
                    call.respond(
                        HttpStatusCode.Conflict,
                        UnexpectedCallDiagnostic(
                            reason = result.reason,
                            expected = result.expected,
                            actual = ActualRequest(request.method, request.path, request.pathParams, request.query),
                        ),
                    )
            }
        }

        post("/__stub__/reset") {
            engine.reset()
            call.respondText("""{"reset":"all"}""", ContentType.Application.Json)
        }
        post("/__stub__/reset/{eid}") {
            val eid = call.parameters["eid"].orEmpty()
            engine.reset(eid)
            call.respondText("""{"reset":"$eid"}""", ContentType.Application.Json)
        }
    }
}

fun main(args: Array<String>) {
    val port = (System.getenv("STUB_PORT") ?: args.getOrNull(0))?.toIntOrNull() ?: DEFAULT_PORT
    val scenarioDir = System.getenv("STUB_SCENARIO_DIR") ?: args.getOrNull(1)
    val scenarios =
        if (scenarioDir != null) {
            ScenarioLoader.loadFromDirectory(File(scenarioDir))
        } else {
            ScenarioLoader.loadDefault()
        }
    val engine = MatchingEngine(scenarios)
    println("EMA API stub listening on port $port with ${scenarios.size} ECU scenario(s): ${scenarios.keys}")
    embeddedServer(CIO, port = port) { stubModule(engine) }.start(wait = true)
}
