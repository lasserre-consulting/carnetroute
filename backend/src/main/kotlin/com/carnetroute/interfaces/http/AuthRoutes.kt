package com.carnetroute.interfaces.http

import com.carnetroute.application.user.AuthenticateUserUseCase
import com.carnetroute.application.user.AuthenticateUserRequest
import com.carnetroute.application.user.CreateUserUseCase
import com.carnetroute.application.user.CreateUserRequest
import com.carnetroute.application.user.GetUserProfileUseCase
import com.carnetroute.application.user.UpdateUserProfileUseCase
import com.carnetroute.application.user.UpdateProfileRequest as DomainUpdateProfileRequest
import com.carnetroute.application.user.UserPreferencesRequest
import com.carnetroute.interfaces.http.dto.AuthResponse
import com.carnetroute.interfaces.http.dto.LoginRequest
import com.carnetroute.interfaces.http.dto.PreferencesResponse
import com.carnetroute.interfaces.http.dto.RegisterRequest
import com.carnetroute.interfaces.http.dto.UpdateProfileRequest
import com.carnetroute.interfaces.http.dto.UserResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
fun Route.configureAuthRoutes(
    createUserUseCase: CreateUserUseCase,
    authenticateUseCase: AuthenticateUserUseCase,
    getUserProfileUseCase: GetUserProfileUseCase,
    updateUserProfileUseCase: UpdateUserProfileUseCase,
) {

    route("/auth") {
        // POST /api/auth/register
        post("/register") {
            val request = call.receive<RegisterRequest>()
            val tokens = createUserUseCase.execute(
                CreateUserRequest(
                    email = request.email,
                    password = request.password,
                    name = request.name
                )
            )
            call.respond(
                HttpStatusCode.Created,
                AuthResponse(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    userId = tokens.userId
                )
            )
        }

        // POST /api/auth/login
        post("/login") {
            val request = call.receive<LoginRequest>()
            val tokens = authenticateUseCase.execute(
                AuthenticateUserRequest(
                    email = request.email,
                    password = request.password
                )
            )
            call.respond(
                AuthResponse(
                    accessToken = tokens.accessToken,
                    refreshToken = tokens.refreshToken,
                    userId = tokens.userId
                )
            )
        }

        authenticate("auth-jwt") {
            // GET /api/auth/me
            get("/me") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val user = getUserProfileUseCase.execute(userId)
                call.respond(
                    UserResponse(
                        id = user.id,
                        email = user.email,
                        name = user.name,
                        preferences = PreferencesResponse(
                            defaultFuelType = user.preferences.defaultFuelType,
                            alertsEnabled = user.preferences.alertsEnabled,
                            theme = user.preferences.theme
                        )
                    )
                )
            }

            // PATCH /api/auth/me
            patch("/me") {
                val userId = call.principal<JWTPrincipal>()!!.payload.getClaim("userId").asString()
                val request = call.receive<UpdateProfileRequest>()
                val updated = updateUserProfileUseCase.execute(
                    userId,
                    DomainUpdateProfileRequest(
                        name = request.name,
                        preferences = UserPreferencesRequest(
                            defaultFuelType = request.defaultFuelType,
                            alertsEnabled = request.alertsEnabled,
                            theme = request.theme
                        )
                    )
                )
                call.respond(
                    UserResponse(
                        id = updated.id,
                        email = updated.email,
                        name = updated.name,
                        preferences = PreferencesResponse(
                            defaultFuelType = updated.preferences.defaultFuelType,
                            alertsEnabled = updated.preferences.alertsEnabled,
                            theme = updated.preferences.theme
                        )
                    )
                )
            }
        }
    }
}
