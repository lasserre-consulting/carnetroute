package com.carnetroute.domain.user.vo

import com.carnetroute.domain.shared.DomainException

@JvmInline
value class Email(val value: String) {
    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}$")

        fun of(raw: String): Email {
            val trimmed = raw.trim()
            if (!EMAIL_REGEX.matches(trimmed)) {
                throw DomainException.ValidationError("email", "Invalid email format")
            }
            return Email(trimmed)
        }
    }
}
