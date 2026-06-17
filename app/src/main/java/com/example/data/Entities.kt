package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "students")
data class StudentEntity(
    @PrimaryKey val email: String,
    val name: String,
    val collegeName: String,
    val stream: String,
    val year: String,
    val contactNo: String,
    val regNo: String,
    val profilePhoto: String = ""
)

@Entity(tableName = "organizers")
data class OrganizerEntity(
    @PrimaryKey val email: String,
    val organizerName: String,
    val collegeName: String,
    val contactNo: String,
    val eventName: String,
    val profilePicture: String = "",
    val verified: Boolean = false
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val isTechnical: Boolean, // Subcategories: Tech, Non-tech
    val collegeName: String,
    val date: String,
    val venue: String,
    val regFee: Double,
    val prizeMoney: Double,
    val capacity: Int,
    val seatsLeft: Int,
    val guestSpeakers: String, // Comma separated
    val rules: String,
    val organizerEmail: String,
    val imageResName: String = "", // Placeholders
    val createTime: Long = System.currentTimeMillis()
)

@Entity(tableName = "food_stalls")
data class FoodStallEntity(
    @PrimaryKey val id: String,
    val name: String,
    val cuisine: String,
    val rating: Double,
    val distance: String,
    val popularity: Int,
    val imageResName: String = ""
)

@Entity(tableName = "food_coupons")
data class FoodCouponEntity(
    @PrimaryKey val id: String,
    val stallId: String,
    val itemName: String,
    val price: Double,
    val offers: String,
    val availability: Boolean = true,
    val rating: Double = 4.5
)

@Entity(tableName = "announcements")
data class AnnouncementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val text: String,
    val creatorName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "registrations")
data class RegistrationEntity(
    @PrimaryKey val id: String, // "REG-" + hash
    val studentEmail: String,
    val eventId: String,
    val eventTitle: String,
    val regFee: Double,
    val paymentMethod: String,
    val status: String, // "PAID", "CANCELLED", "Not Registered", "Payment Pending", "Payment Successful", "Registered"
    val qrCodeString: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPreBookedCoupon: Boolean = false,
    val preBookedCouponId: String? = null,
    val paymentId: String? = null,
    val paymentStatus: String? = null,
    val ticketId: String? = null
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey val paymentId: String,
    val userId: String, // email
    val eventId: String,
    val amount: Double,
    val currency: String,
    val status: String, // "SUCCESS", "FAILED", "PENDING"
    val gateway: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val ticketId: String,
    val registrationId: String,
    val qrCode: String,
    val status: String // "ACTIVE", "CANCELLED"
)

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val messageText: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_log")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val actionType: String, // "CREATE_EVENT", "DELETE_EVENT", "UPDATE_EVENT", "DELETE_HISTORY", "TRANSACTION", "REGISTRATION"
    val actorEmail: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis()
)
