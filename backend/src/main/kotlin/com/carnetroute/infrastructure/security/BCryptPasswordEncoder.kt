package com.carnetroute.infrastructure.security

import at.favre.lib.crypto.bcrypt.BCrypt

class BCryptPasswordEncoder : PasswordEncoder {
    override fun encode(rawPassword: String): String =
        BCrypt.withDefaults().hashToString(12, rawPassword.toCharArray())

    override fun matches(rawPassword: String, encodedPassword: String): Boolean =
        BCrypt.verifyer().verify(rawPassword.toCharArray(), encodedPassword).verified
}
