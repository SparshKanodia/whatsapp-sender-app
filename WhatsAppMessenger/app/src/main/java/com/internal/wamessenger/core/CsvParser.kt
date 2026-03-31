package com.internal.wamessenger.core

import android.content.Context
import android.net.Uri
import com.google.i18n.phonenumbers.NumberParseException
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.internal.wamessenger.model.Contact
import com.opencsv.CSVReaderBuilder
import java.io.InputStreamReader

data class CsvParseResult(
    val contacts: List<Contact>,
    val errors: List<String>,
    val warnings: List<String>,
    val headers: List<String>
)

object CsvParser {

    private val phoneUtil = PhoneNumberUtil.getInstance()

    fun parse(context: Context, uri: Uri, defaultCountryCode: String = "IN"): CsvParseResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val contacts = mutableListOf<Contact>()
        val seenPhones = mutableSetOf<String>()

        try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return CsvParseResult(emptyList(), listOf("Cannot open file"), emptyList(), emptyList())

            val reader = CSVReaderBuilder(InputStreamReader(inputStream)).build()
            val allRows = reader.readAll()
            reader.close()

            if (allRows.isEmpty()) {
                return CsvParseResult(emptyList(), listOf("CSV file is empty"), emptyList(), emptyList())
            }

            val headers = allRows[0].map { it.trim().lowercase() }

            if (!headers.contains("phone")) {
                return CsvParseResult(emptyList(), listOf("CSV must contain a 'phone' column"), emptyList(), headers)
            }

            val phoneIndex = headers.indexOf("phone")

            for (rowIndex in 1 until allRows.size) {
                val row = allRows[rowIndex]
                if (row.isEmpty() || row.all { it.isBlank() }) continue

                if (row.size <= phoneIndex) {
                    errors.add("Row ${rowIndex + 1}: Missing phone column")
                    continue
                }

                val rawPhone = row[phoneIndex].trim()
                if (rawPhone.isBlank()) {
                    errors.add("Row ${rowIndex + 1}: Empty phone number")
                    continue
                }

                val normalizedPhone = normalizePhone(rawPhone, defaultCountryCode)
                if (normalizedPhone == null) {
                    errors.add("Row ${rowIndex + 1}: Invalid phone number '$rawPhone'")
                    continue
                }

                if (seenPhones.contains(normalizedPhone)) {
                    warnings.add("Row ${rowIndex + 1}: Duplicate phone $normalizedPhone — skipped")
                    continue
                }
                seenPhones.add(normalizedPhone)

                val fields = mutableMapOf<String, String>()
                headers.forEachIndexed { i, header ->
                    fields[header] = if (i < row.size) row[i].trim() else ""
                }
                fields["phone"] = normalizedPhone

                contacts.add(Contact(phone = normalizedPhone, fields = fields))
            }

        } catch (e: Exception) {
            errors.add("Parse error: ${e.message}")
        }

        return CsvParseResult(
            contacts = contacts,
            errors = errors,
            warnings = warnings,
            headers = emptyList()
        )
    }

    private fun normalizePhone(raw: String, defaultRegion: String): String? {
        return try {
            val cleaned = raw.replace(Regex("[\\s\\-().+]"), "")
            val withPlus = if (!raw.startsWith("+")) "+$cleaned" else "+${cleaned}"
            val parsed = phoneUtil.parse(withPlus, defaultRegion)
            if (phoneUtil.isValidNumber(parsed)) {
                phoneUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164)
                    .removePrefix("+")
            } else null
        } catch (e: NumberParseException) {
            // Try with country code prepended
            try {
                val withRegion = phoneUtil.parse(raw, defaultRegion)
                if (phoneUtil.isValidNumber(withRegion)) {
                    phoneUtil.format(withRegion, PhoneNumberUtil.PhoneNumberFormat.E164)
                        .removePrefix("+")
                } else null
            } catch (e2: Exception) {
                null
            }
        }
    }

    fun validateTemplateAgainstHeaders(template: String, headers: List<String>): List<String> {
        val warnings = mutableListOf<String>()
        val placeholderRegex = Regex("\\{(\\w+)}")
        placeholderRegex.findAll(template).forEach { match ->
            val key = match.groupValues[1]
            if (!headers.contains(key.lowercase())) {
                warnings.add("Template placeholder '{$key}' not found in CSV headers")
            }
        }
        return warnings
    }
}
