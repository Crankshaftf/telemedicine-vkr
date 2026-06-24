package com.medconnect.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    .withZone(ZoneId.systemDefault())

fun formatDateTime(iso: String): String = runCatching {
    displayFormatter.format(Instant.parse(iso))
}.getOrElse { iso }

fun statusLabel(status: String): String = when (status) {
    "created" -> "Создана"
    "confirmed" -> "Подтверждена"
    "rescheduled" -> "Перенесена"
    "cancelled" -> "Отменена"
    "completed" -> "Завершена"
    else -> status
}
