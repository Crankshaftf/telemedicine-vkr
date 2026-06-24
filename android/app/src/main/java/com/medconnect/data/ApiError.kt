package com.medconnect.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private val errorJson = Json { ignoreUnknownKeys = true }

fun Throwable.toUserMessage(): String {
    return when (this) {
        is HttpException -> parseHttpError(this)
        is UnknownHostException, is ConnectException ->
            "Не удалось подключиться к серверу. Запустите backend (порт 8000) и проверьте URL в настройках."
        is SocketTimeoutException ->
            "Превышено время ожидания. Сервер не ответил за 60 секунд — попробуйте ещё раз."
        is IOException ->
            "Ошибка сети: ${message ?: "нет соединения"}"
        else -> message ?: "Неизвестная ошибка"
    }
}

private fun parseHttpError(ex: HttpException): String {
    val body = ex.response()?.errorBody()?.string()
    if (!body.isNullOrBlank()) {
        runCatching {
            val detail = errorJson.parseToJsonElement(body).jsonObject["detail"]
            val msg = detail?.let { formatDetail(it) }
            if (!msg.isNullOrBlank()) return msg
        }
    }
    return when (ex.code()) {
        401 -> "Неверный логин или пароль"
        403 -> "Недостаточно прав"
        404 -> "Не найдено"
        409 -> "Слот уже занят или конфликт данных"
        else -> "Ошибка сервера (${ex.code()})"
    }
}

private fun formatDetail(element: kotlinx.serialization.json.JsonElement): String {
    return when (element) {
        is JsonPrimitive -> element.content
        is JsonObject -> element["msg"]?.jsonPrimitive?.content ?: element.toString()
        else -> element.toString()
    }
}
