package com.example.booksy.model

data class Request(
    val id: String = "",
    val bookId: String = "",
    val fromUserId: String = "",
    val toUserId: String = "",
    val status: RequestStatus = RequestStatus.PENDING
)

enum class RequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}
