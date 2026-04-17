package com.carnetroute.interfaces.http

import io.ktor.server.routing.Route
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinx.coroutines.channels.consumeEach
import java.util.Collections

fun Route.configureWebSocketRoutes() {
    val sessions = Collections.synchronizedSet(mutableSetOf<DefaultWebSocketSession>())

    webSocket("/ws/alerts") {
        sessions.add(this)
        try {
            incoming.consumeEach { frame ->
                if (frame is Frame.Text) {
                    if (frame.readText() == "ping") send(Frame.Text("pong"))
                }
            }
        } finally {
            sessions.remove(this)
        }
    }
}
