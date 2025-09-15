// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
                // Ktlint 1.x: disable long lines (Compose often exceeds 120 chars)
                "ktlint_standard_max-line-length" to "disabled",
                "disabled_rules" to "filename",
                // Allow PascalCase for Composable/Preview functions
                "ktlint_function_naming_ignore_when_annotated_with" to "Composable,Preview",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint().editorConfigOverride(
            mapOf(
                "indent_size" to "4",
                "continuation_indent_size" to "4",
                "ktlint_standard_max-line-length" to "disabled",
            ),
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
