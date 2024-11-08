/*
 * Copyrighted (Kord Extensions, 2024). Licensed under the EUPL-1.2
 * with the specific provision (EUPL articles 14 & 15) that the
 * applicable law is the (Republic of) Irish law and the Jurisdiction
 * Dublin.
 * Any redistribution must include the specific provision above.
 */

package dev.kordex.core.utils

import dev.kordex.core.serialization.deserializeRaw
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.serializer
import kotlin.io.path.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.readLines

private var firstLoad: Boolean = true
private var logger = KotlinLogging.logger {}
private val envMap: MutableStringMap = mutableMapOf()

/**
 * Returns the value of an environmental variable, loading from a `.env` file in the current working directory if
 * possible.
 *
 * This function caches the contents of the `.env` file the first time it's called - there's no way to parse the file
 * again later.
 *
 * @param name Environmental variable to get the value for.
 * @return The value of the environmental variable, or `null` if it doesn't exist.
 */
public fun envOrNull(name: String): String? {
	if (firstLoad) {
		firstLoad = false

		val dotenvFile = Path(".env")

		if (dotenvFile.isRegularFile()) {
			logger.info { "Loading environment variables from .env file" }

			val lines = dotenvFile.readLines()

			for (line in lines) {
				var effectiveLine = line.trimStart()

				if (effectiveLine.isBlank() || effectiveLine.startsWith("#")) {
					continue
				}

				if (effectiveLine.contains("#")) {
					effectiveLine = effectiveLine.substring(0, effectiveLine.indexOf("#"))
				}

				if (!effectiveLine.contains('=')) {
					logger.warn {
						"Invalid line in dotenv file: \"=\" not found\n" +
							"    $effectiveLine"
					}

					continue
				}

				val split = effectiveLine
					.split("=", limit = 2)
					.map { it.trim() }

				if (split.size != 2) {
					logger.warn {
						"Invalid line in dotenv file: variables must be of the form \"name=value\"\n" +
							" -> $effectiveLine"
					}

					continue
				}

				logger.trace { "${split[0]} -> ${split[1]}" }

				envMap[split[0]] = split[1]
			}
		}
	}

	return envMap[name] ?: System.getenv()[name]
}

/**
 * Returns the value of an environmental variable, loading from a `.env` file in the current working directory if
 * possible.
 *
 * This function caches the contents of the `.env` file the first time it's called - there's no way to parse the file
 * again later.
 *
 * This function will throw an exception if the environmental variable can't be found.
 *
 * @param name Environmental variable to get the value for.
 *
 * @throws RuntimeException Thrown if the environmental variable can't be found.
 * @return The value of the environmental variable.
 */
public fun env(name: String): String =
	envOrNull(name) ?: error(
		"Missing environmental variable '$name' - please set this by adding it to a `.env` file, or using your " +
			"system or process manager's environment management commands and tools."
	)

/**
 * Deserializes the value of an environmental variable, loading from a `.env` file in the current working directory
 * if possible.
 *
 * @param T Type to deserialize to, which must support kotlinx.serialization.
 * @param name Environmental variable to get the value for.
 *
 * @throws RuntimeException Thrown if the environmental variable can't be found.
 * @throws kotlinx.serialization.SerializationException Thrown if deserialization fails.
 *
 * @return The value of the environmental variable, deserialized to type [T].
 */
public inline fun <reified T : Any> envOfOrNull(name: String): T? {
	val value = envOrNull(name)

	if (value == null) {
		return null
	}

	@Suppress("TooGenericExceptionCaught")
	return try {
		serializer<T>().deserializeRaw(value)
	} catch (_: Exception) {
		null
	}
}

/**
 * Deserializes the value of an environmental variable, loading from a `.env` file in the current working directory
 * if possible.
 *
 * This function will throw an exception if the environmental variable can't be found, or the value can't be
 * deserialized.
 *
 * @param T Type to deserialize to, which must support kotlinx.serialization.
 * @param name Environmental variable to get the value for.
 *
 * @throws RuntimeException Thrown if the environmental variable can't be found.
 * @throws kotlinx.serialization.SerializationException Thrown if deserialization fails.
 *
 * @return The value of the environmental variable, deserialized to type [T].
 */
public inline fun <reified T : Any> envOf(name: String): T =
	serializer<T>().deserializeRaw(env(name))
