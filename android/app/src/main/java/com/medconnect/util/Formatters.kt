package com.medconnect.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    .withZone(ZoneId.systemDefault())

fun formatDateTime(iso: String): String {
    if (iso.isBlank()) return iso
    // Try full ISO-8601 with Z or offset (e.g. "2026-06-15T09:00:00Z")
    runCatching {
        return displayFormatter.format(Instant.parse(iso))
    }
    // Fallback: naive local datetime string without timezone (e.g. "2026-06-15T09:00:00")
    runCatching {
        val ldt = LocalDateTime.parse(iso)
        return displayFormatter.format(ldt.toInstant(ZoneOffset.UTC))
    }
    return iso
}

fun statusLabel(status: String): String = when (status) {
    "created"     -> "Создана"
    "confirmed"   -> "Подтверждена"
    "rescheduled" -> "Перенесена"
    "cancelled"   -> "Отменена"
    "completed"   -> "Завершена"
    else          -> status
}
