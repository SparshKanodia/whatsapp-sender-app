package com.example.whatsappbulkmessenger.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.whatsappbulkmessenger.data.model.Contact
import org.apache.poi.ss.usermodel.DataFormatter
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook

data class ParseContactsResult(
    val contacts: List<Contact>,
    val skippedRows: Int,
    val message: String
)

class UploadRepository {

    fun parseContactsFromExcel(contentResolver: ContentResolver, uri: Uri): ParseContactsResult {
        contentResolver.openInputStream(uri)?.use { inputStream ->
            XSSFWorkbook(inputStream).use { workbook ->
                val sheet = workbook.getSheetAt(0)
                val formatter = DataFormatter()
                val evaluator = workbook.creationHelper.createFormulaEvaluator()

                val headerRow = sheet.getRow(0)
                    ?: return ParseContactsResult(
                        contacts = emptyList(),
                        skippedRows = 0,
                        message = "The selected file does not include a header row."
                    )

                val headers = headerRow
                    .cellIterator()
                    .asSequence()
                    .associate { cell ->
                        val header = formatter.formatCellValue(cell, evaluator).trim()
                        cell.columnIndex to header
                    }

                val nameColumn = resolveColumnIndex(headers, "name", "full name")
                val phoneColumn = resolveColumnIndex(headers, "phone", "phone number", "mobile", "number")

                if (nameColumn == null || phoneColumn == null) {
                    return ParseContactsResult(
                        contacts = emptyList(),
                        skippedRows = 0,
                        message = "Missing required columns. Expected Name and Phone Number headers."
                    )
                }

                val contacts = mutableListOf<Contact>()
                var skippedRows = 0

                for (rowIndex in 1..sheet.lastRowNum) {
                    val row = sheet.getRow(rowIndex)
                    if (row == null || row.isCompletelyEmpty()) {
                        continue
                    }

                    val name = formatter.formatCellValue(
                        row.getCell(nameColumn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL),
                        evaluator
                    ).trim()

                    val rawPhone = formatter.formatCellValue(
                        row.getCell(phoneColumn, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL),
                        evaluator
                    ).trim()

                    val normalizedPhone = normalizePhone(rawPhone)
                    if (name.isBlank() || normalizedPhone == null) {
                        skippedRows += 1
                        continue
                    }

                    val extras = headers
                        .filterKeys { it != nameColumn && it != phoneColumn }
                        .mapValues { (index, _) ->
                            formatter.formatCellValue(
                                row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL),
                                evaluator
                            ).trim()
                        }
                        .filterValues { it.isNotBlank() }

                    contacts.add(
                        Contact(
                            name = name,
                            phone = normalizedPhone,
                            extraFields = extras
                        )
                    )
                }

                val message = if (contacts.isEmpty()) {
                    "No valid contacts were found in the selected file."
                } else {
                    "Parsed ${contacts.size} contact(s). Skipped $skippedRows row(s)."
                }

                return ParseContactsResult(
                    contacts = contacts,
                    skippedRows = skippedRows,
                    message = message
                )
            }
        }

        return ParseContactsResult(
            contacts = emptyList(),
            skippedRows = 0,
            message = "Unable to open the selected file."
        )
    }

    private fun resolveColumnIndex(headers: Map<Int, String>, vararg candidates: String): Int? {
        val normalizedCandidates = candidates.map { it.lowercase() }.toSet()
        return headers.entries.firstOrNull { (_, header) ->
            header.lowercase() in normalizedCandidates
        }?.key
    }

    private fun normalizePhone(rawPhone: String): String? {
        if (rawPhone.isBlank()) return null
        val digitsOnly = rawPhone.filter { it.isDigit() }
        return digitsOnly.takeIf { it.length in 7..15 }
    }

    private fun Row.isCompletelyEmpty(): Boolean =
        this.none { cell -> cell != null && cell.toString().trim().isNotEmpty() }
}
