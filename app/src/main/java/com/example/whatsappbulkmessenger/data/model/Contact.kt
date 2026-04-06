package com.example.whatsappbulkmessenger.data.model

data class Contact(
    val name: String,
    val phone: String,
    val extraFields: Map<String, String>
)
