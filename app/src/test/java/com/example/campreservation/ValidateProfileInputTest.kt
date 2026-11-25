package com.example.campreservation


import org.junit.Assert.*
import org.junit.Test

class ValidateProfileInputTest {
    @Test fun blankName() = assertEquals("Name can not be left empty", validateProfileInput("  ", "a@b.com"))
    @Test fun blankEmail() = assertEquals("Email can not be left empty", validateProfileInput("Sam", ""))
    @Test fun missingAt() = assertEquals("Enter a valid email address", validateProfileInput("Sam", "sam.example.com"))
    @Test fun missingDomainDot() = assertEquals("Enter a valid email address", validateProfileInput("Sam", "sam@example"))
    @Test fun spaceInside() = assertEquals("Enter a valid email address", validateProfileInput("Sam", "sa m@example.com"))
    @Test fun validSimple() = assertNull(validateProfileInput("Sam", "sam@example.com"))
    @Test fun validPlusAndSubdomain() = assertNull(validateProfileInput("Sam", "sam+camp@mail.uni.ca"))
    @Test fun validWithTrailingSpace() = assertNull(validateProfileInput("Sam", "sam@example.com "))
}