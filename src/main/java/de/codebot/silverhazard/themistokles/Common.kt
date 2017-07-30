package de.codebot.silverhazard.themistokles

import org.apache.commons.validator.routines.EmailValidator

/**
 * (Extension) methods for common tasks
 */

/**
 * Compare two [Iterables[Iterable] based on their element count
 */
operator fun Iterable<*>.compareTo(other: Iterable<*>): Int = this.count() - other.count()

/**
 * Compare an [Array] to a [Iterable] based on their element count
 */
operator fun Array<*>.compareTo(other: Iterable<*>): Int = this.count() - other.count()

/**
 * Compare an [Array] to a [Iterable] based on their element count
 */
operator fun Iterable<*>.compareTo(other: Array<*>): Int = this.count() - other.count()


private val validator = EmailValidator.getInstance()
/**
 * Check if String is a valid email address using Apache Commons EmailValidator
 */
fun String.isValidEmail(): Boolean = validator.isValid(this)