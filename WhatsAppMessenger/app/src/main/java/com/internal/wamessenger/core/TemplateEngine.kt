package com.internal.wamessenger.core

import com.internal.wamessenger.model.Contact

object TemplateEngine {

    private val placeholderRegex = Regex("\\{(\\w+)}")

    /**
     * Renders a template for a given contact.
     * Missing keys produce empty string (not error).
     */
    fun render(template: String, contact: Contact): String {
        return placeholderRegex.replace(template) { match ->
            val key = match.groupValues[1].lowercase()
            contact.fields[key] ?: ""
        }
    }

    /**
     * Returns list of placeholder names used in template
     */
    fun extractPlaceholders(template: String): List<String> {
        return placeholderRegex.findAll(template)
            .map { it.groupValues[1] }
            .toList()
    }

    /**
     * Validates template is not empty and has balanced braces
     */
    fun validate(template: String): List<String> {
        val errors = mutableListOf<String>()
        if (template.isBlank()) {
            errors.add("Template cannot be empty")
        }
        // Check for unclosed braces
        val openCount = template.count { it == '{' }
        val closeCount = template.count { it == '}' }
        if (openCount != closeCount) {
            errors.add("Template has unmatched braces: $openCount open, $closeCount close")
        }
        return errors
    }

    /**
     * Pick random template from a list
     */
    fun pickRandom(templates: List<String>): String {
        return templates.random()
    }

    /**
     * Returns preview with missing fields highlighted
     */
    fun renderWithHighlights(template: String, contact: Contact): Pair<String, List<String>> {
        val missing = mutableListOf<String>()
        val rendered = placeholderRegex.replace(template) { match ->
            val key = match.groupValues[1].lowercase()
            val value = contact.fields[key]
            if (value == null || value.isBlank()) {
                missing.add(key)
                "[MISSING: $key]"
            } else value
        }
        return Pair(rendered, missing)
    }
}
