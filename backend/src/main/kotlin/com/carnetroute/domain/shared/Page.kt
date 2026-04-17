package com.carnetroute.domain.shared

@kotlinx.serialization.Serializable
data class Page<T>(
    val content: List<T>,
    val totalElements: Long,
    val page: Int,
    val size: Int,
    val totalPages: Int = if (size == 0) 0 else ((totalElements + size - 1) / size).toInt()
)
