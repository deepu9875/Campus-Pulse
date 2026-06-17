package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE email = :email LIMIT 1")
    suspend fun getStudentByEmail(email: String): StudentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudent(student: StudentEntity)

    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<StudentEntity>>
}

@Dao
interface OrganizerDao {
    @Query("SELECT * FROM organizers WHERE email = :email LIMIT 1")
    suspend fun getOrganizerByEmail(email: String): OrganizerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrganizer(organizer: OrganizerEntity)

    @Query("SELECT * FROM organizers")
    fun getAllOrganizersFlow(): Flow<List<OrganizerEntity>>

    @Query("UPDATE organizers SET verified = :verified WHERE email = :email")
    suspend fun setOrganizerVerification(email: String, verified: Boolean)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY createTime DESC")
    fun getAllEventsFlow(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    suspend fun getEventById(id: String): EventEntity?

    @Query("SELECT * FROM events WHERE isTechnical = :isTech ORDER BY createTime DESC")
    fun getEventsByTypeFlow(isTech: Boolean): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Delete
    suspend fun deleteEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteEventById(id: String)
}

@Dao
interface FoodStallDao {
    @Query("SELECT * FROM food_stalls ORDER BY popularity DESC")
    fun getAllFoodStallsFlow(): Flow<List<FoodStallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodStall(stall: FoodStallEntity)
}

@Dao
interface FoodCouponDao {
    @Query("SELECT * FROM food_coupons WHERE stallId = :stallId")
    fun getCouponsForStallFlow(stallId: String): Flow<List<FoodCouponEntity>>

    @Query("SELECT * FROM food_coupons WHERE id = :id LIMIT 1")
    suspend fun getCouponById(id: String): FoodCouponEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCoupon(coupon: FoodCouponEntity)
}

@Dao
interface AnnouncementDao {
    @Query("SELECT * FROM announcements ORDER BY timestamp DESC")
    fun getAllAnnouncementsFlow(): Flow<List<AnnouncementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: AnnouncementEntity)
}

@Dao
interface RegistrationDao {
    @Query("SELECT * FROM registrations ORDER BY timestamp DESC")
    fun getAllRegistrationsFlow(): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE studentEmail = :studentEmail ORDER BY timestamp DESC")
    fun getRegistrationsForStudentFlow(studentEmail: String): Flow<List<RegistrationEntity>>

    @Query("SELECT * FROM registrations WHERE eventId = :eventId")
    suspend fun getRegistrationsForEvent(eventId: String): List<RegistrationEntity>

    @Query("SELECT * FROM registrations WHERE studentEmail = :email AND eventId = :eventId AND status = 'PAID' LIMIT 1")
    suspend fun getActiveRegistration(email: String, eventId: String): RegistrationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRegistration(registration: RegistrationEntity)

    @Query("UPDATE registrations SET status = :status WHERE id = :registrationId")
    suspend fun updateRegistrationStatus(registrationId: String, status: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY timestamp ASC")
    fun getAllChatsFlow(): Flow<List<ChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("DELETE FROM chats")
    suspend fun clearChats()
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_log ORDER BY timestamp DESC")
    fun getAllAuditLogsFlow(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)
}

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments ORDER BY timestamp DESC")
    fun getAllPaymentsFlow(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPaymentsForUserFlow(userId: String): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE eventId = :eventId")
    suspend fun getPaymentsForEvent(eventId: String): List<PaymentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)
}

@Dao
interface TicketDao {
    @Query("SELECT * FROM tickets WHERE registrationId = :registrationId LIMIT 1")
    suspend fun getTicketForRegistration(registrationId: String): TicketEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)
}
