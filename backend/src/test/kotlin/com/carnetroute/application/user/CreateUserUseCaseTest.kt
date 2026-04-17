package com.carnetroute.application.user

import com.carnetroute.domain.shared.DomainException
import com.carnetroute.domain.user.User
import com.carnetroute.domain.user.UserRepository
import com.carnetroute.domain.user.vo.Email
import com.carnetroute.domain.user.vo.PasswordHash
import com.carnetroute.domain.user.vo.UserPreferences
import com.carnetroute.infrastructure.security.JwtService
import com.carnetroute.infrastructure.security.PasswordEncoder
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldNotBeBlank
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.Clock

class CreateUserUseCaseTest : BehaviorSpec({

    val userRepository = mockk<UserRepository>()
    val passwordEncoder = mockk<PasswordEncoder>()
    val jwtService = mockk<JwtService>()

    val useCase = CreateUserUseCase(
        userRepository = userRepository,
        passwordEncoder = passwordEncoder,
        jwtService = jwtService
    )

    // Helper pour créer un User stub
    fun stubUser(
        id: String = "user-abc-123",
        email: String = "alice@example.com",
        name: String = "Alice"
    ): User {
        val now = Clock.System.now().toString()
        return User(
            id = id,
            email = email,
            passwordHash = "hashed_password",
            name = name,
            preferences = UserPreferences(),
            createdAt = now,
            updatedAt = now
        )
    }

    // ─────────────────────────────────────────────
    // Création réussie
    // ─────────────────────────────────────────────
    given("une requête valide avec email et mot de passe corrects") {
        val request = CreateUserRequest(
            email = "alice@example.com",
            password = "SecurePass1",
            name = "Alice"
        )

        coEvery { userRepository.existsByEmail(any()) } returns false
        every { passwordEncoder.encode(any()) } returns "hashed_password"
        coEvery { userRepository.save(any()) } returns stubUser()
        every { jwtService.generateToken(any(), any()) } returns "access_token_xyz"
        every { jwtService.generateRefreshToken(any()) } returns "refresh_token_xyz"

        `when`("on crée l'utilisateur") {
            val result = useCase.execute(request)

            then("le résultat ne doit pas être null") {
                result shouldNotBe null
            }

            then("l'accessToken ne doit pas être vide") {
                result.accessToken shouldNotBeBlank()
            }

            then("le refreshToken ne doit pas être vide") {
                result.refreshToken shouldNotBeBlank()
            }

            then("l'userId ne doit pas être vide") {
                result.userId shouldNotBeBlank()
            }

            then("le mot de passe doit être encodé avant d'être sauvegardé") {
                coVerify(exactly = 1) { passwordEncoder.encode("SecurePass1") }
            }

            then("l'utilisateur doit être sauvegardé dans le repository") {
                coVerify(exactly = 1) { userRepository.save(any()) }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Email déjà existant
    // ─────────────────────────────────────────────
    given("un email déjà enregistré dans la base") {
        val request = CreateUserRequest(
            email = "existing@example.com",
            password = "ValidPass99",
            name = "Bob"
        )

        coEvery { userRepository.existsByEmail("existing@example.com") } returns true

        `when`("on tente de créer l'utilisateur") {
            then("doit lever DomainException.UserAlreadyExists") {
                shouldThrow<DomainException.UserAlreadyExists> {
                    useCase.execute(request)
                }
            }

            then("le repository.save ne doit jamais être appelé") {
                coVerify(exactly = 0) { userRepository.save(any()) }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Mot de passe trop court
    // ─────────────────────────────────────────────
    given("un mot de passe trop court (< 8 caractères)") {

        `when`("le mot de passe fait 7 caractères") {
            then("doit lever DomainException.ValidationError sur le champ 'password'") {
                val ex = shouldThrow<DomainException.ValidationError> {
                    useCase.execute(
                        CreateUserRequest(
                            email = "charlie@example.com",
                            password = "1234567",
                            name = "Charlie"
                        )
                    )
                }
                ex.field shouldBe "password"
            }
        }

        `when`("le mot de passe est vide") {
            then("doit lever DomainException.ValidationError sur le champ 'password'") {
                val ex = shouldThrow<DomainException.ValidationError> {
                    useCase.execute(
                        CreateUserRequest(
                            email = "diana@example.com",
                            password = "",
                            name = "Diana"
                        )
                    )
                }
                ex.field shouldBe "password"
            }
        }
    }

    // ─────────────────────────────────────────────
    // Email vide ou invalide
    // ─────────────────────────────────────────────
    given("un email vide") {

        `when`("l'email est une chaîne vide") {
            then("doit lever DomainException.ValidationError sur le champ 'email'") {
                val ex = shouldThrow<DomainException.ValidationError> {
                    useCase.execute(
                        CreateUserRequest(
                            email = "",
                            password = "ValidPass99",
                            name = "Eve"
                        )
                    )
                }
                ex.field shouldBe "email"
            }
        }

        `when`("l'email est composé uniquement d'espaces") {
            then("doit lever DomainException.ValidationError sur le champ 'email'") {
                val ex = shouldThrow<DomainException.ValidationError> {
                    useCase.execute(
                        CreateUserRequest(
                            email = "   ",
                            password = "ValidPass99",
                            name = "Frank"
                        )
                    )
                }
                ex.field shouldBe "email"
            }
        }
    }

    // ─────────────────────────────────────────────
    // Email au format invalide (rejeté par Email.of)
    // ─────────────────────────────────────────────
    given("un email au format invalide") {

        `when`("l'email ne contient pas de @") {
            then("doit lever DomainException.ValidationError") {
                shouldThrow<DomainException.ValidationError> {
                    useCase.execute(
                        CreateUserRequest(
                            email = "notanemail",
                            password = "ValidPass99",
                            name = "Grace"
                        )
                    )
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // Nom vide
    // ─────────────────────────────────────────────
    given("un nom vide") {

        `when`("le name est une chaîne vide") {
            then("doit lever DomainException.ValidationError sur le champ 'name'") {
                val ex = shouldThrow<DomainException.ValidationError> {
                    useCase.execute(
                        CreateUserRequest(
                            email = "henry@example.com",
                            password = "ValidPass99",
                            name = ""
                        )
                    )
                }
                ex.field shouldBe "name"
            }
        }
    }
})
