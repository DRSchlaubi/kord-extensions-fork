# Phishing Extension

[![Discord: Click here](https://img.shields.io/static/v1?label=Discord&message=Click%20here&color=7289DA&style=for-the-badge&logo=discord)](https://discord.gg/gjXqqCS) [![Release](https://img.shields.io/nexus/r/com.kotlindiscord.kord.extensions/extra-phishing?nexusVersion=3&logo=gradle&color=blue&label=Release&server=https%3A%2F%2Fmaven.kotlindiscord.com&style=for-the-badge)](https://maven.kotlindiscord.com/#browse/browse:maven-releases:com%2Fkotlindiscord%2Fkord%2Fextensions%2Fextra-phishing) [![Snapshot](https://img.shields.io/nexus/s/com.kotlindiscord.kord.extensions/extra-phishing?logo=gradle&color=orange&label=Snapshot&server=https%3A%2F%2Fmaven.kotlindiscord.com&style=for-the-badge)](https://maven.kotlindiscord.com/#browse/browse:maven-snapshots:com%2Fkotlindiscord%2Fkord%2Fextensions%2Fextra-phishing)

This module contains an extension written to provide some anti-phishing protection, based on the crowdsourced [Sinking Yachts API](https://phish.sinking.yachts/docs).

# Getting Started

* **Maven repo:** `https://maven.kotlindiscord.com/repository/maven-public/`
* **Maven coordinates:** `com.kotlindiscord.kord.extensions:extra-phishing:VERSION`

At its simplest, you can add this extension directly to your bot with a minimum configuration. For example:

```kotlin
suspend fun main() {
    val bot = ExtensibleBot(System.getenv("TOKEN")) {

        extensions {
            extPhishing {
                appName = "My Bot"
            }
        }
    }

    bot.start()
}
```

This will install the extension using its default configuration. However, the extension may be configured in several ways - as is detailed below.

# Commands

This extension provides a number of commands for use on Discord.

* Slash command: `/phishing-check`, for checking whether the given argument is a phishing domain
* Message command: `Phishing Check`, for manually checking a specific message via the right-click menu

Access to both commands can be limited to a specific Discord permission. This can be configured below, but defaults to "Manage Messages".

# Configuration

To configure this module, values can be provided within the `extPhishing` builder.

* **Required:** `appName` - Give your application a name so that Sinking Yachts can identify it for statistics purposes

* `detectionAction` (default: Delete) - What to do when a message containing a phishing domain is detected
* `logChannelName` (default: "logs") - The name of the channel to use for logging; the extension will search the channels present on the current server and use the last one with an exactly-matching name
* `notifyUser` (default: True) - Whether to DM the user, letting them know they posted a phishing domain and what action was taken
* `requiredCommandPermission` (default: Manage Server) - The permission a user must have in order to run the bundled message and slash commands
* `updateDelay` (default: 15 minutes) - How often to check for new phishing domains, five minutes at minimum
* `urlRegex` (default: [The Perfect URL Regex](https://urlregex.com/)) - A regular expression used to extract domains from messages, with **exactly one capturing group containing the entire domain** (and optionally the URL path)

Additionally, the following configuration functions are available:

* `check` - Used to define checks that must pass for event handlers to be run, and thus for messages to be checked (you could use this to exempt your staff, for example)
* `regex` - A convenience function for registering a `String` as URL regex, with the case-insensitive flag
