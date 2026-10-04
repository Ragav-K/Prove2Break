package com.prove2break.ui.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidationTest {

    @Test
    fun `login validate rejects blank email`() {
        assertEquals("Email is required", validate("", "password123"))
    }

    @Test
    fun `login validate rejects malformed email`() {
        assertEquals("Enter a valid email address", validate("not-an-email", "password123"))
    }

    @Test
    fun `login validate rejects blank password`() {
        assertEquals("Password is required", validate("user@example.com", ""))
    }

    @Test
    fun `login validate accepts a well-formed email and non-blank password`() {
        assertNull(validate("user@example.com", "password123"))
    }

    @Test
    fun `register validation rejects short password`() {
        assertEquals(
            "Password must be at least 8 characters",
            validateRegistration("user@example.com", "short", "short"),
        )
    }

    @Test
    fun `register validation rejects mismatched passwords`() {
        assertEquals(
            "Passwords do not match",
            validateRegistration("user@example.com", "password123", "password124"),
        )
    }

    @Test
    fun `register validation accepts matching strong passwords`() {
        assertNull(validateRegistration("user@example.com", "password123", "password123"))
    }

    @Test
    fun `register validation defers to base email-password validation first`() {
        assertEquals(
            "Enter a valid email address",
            validateRegistration("not-an-email", "password123", "password123"),
        )
    }
}
