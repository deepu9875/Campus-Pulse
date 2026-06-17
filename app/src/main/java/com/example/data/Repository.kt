package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

class CampusPulseRepository(private val context: Context) {
    private val db = CampusPulseDatabase.getDatabase(context)
    private val studentDao = db.studentDao()
    private val organizerDao = db.organizerDao()
    private val eventDao = db.eventDao()
    private val foodStallDao = db.foodStallDao()
    private val foodCouponDao = db.foodCouponDao()
    private val announcementDao = db.announcementDao()
    private val registrationDao = db.registrationDao()
    private val chatDao = db.chatDao()
    private val auditLogDao = db.auditLogDao()

    // --- Exposed Streams for MVVM VM Observables ---
    val allEventsFlow: Flow<List<EventEntity>> = eventDao.getAllEventsFlow()
    val technicalEventsFlow: Flow<List<EventEntity>> = eventDao.getEventsByTypeFlow(true)
    val nonTechnicalEventsFlow: Flow<List<EventEntity>> = eventDao.getEventsByTypeFlow(false)
    val allFoodStallsFlow: Flow<List<FoodStallEntity>> = foodStallDao.getAllFoodStallsFlow()
    val allAnnouncementsFlow: Flow<List<AnnouncementEntity>> = announcementDao.getAllAnnouncementsFlow()
    val allAuditLogsFlow: Flow<List<AuditLogEntity>> = auditLogDao.getAllAuditLogsFlow()
    val allChatsFlow: Flow<List<ChatEntity>> = chatDao.getAllChatsFlow()

    fun getRegistrationsForStudent(email: String): Flow<List<RegistrationEntity>> {
        return registrationDao.getRegistrationsForStudentFlow(email)
    }

    fun getCouponsForStall(stallId: String): Flow<List<FoodCouponEntity>> {
        return foodCouponDao.getCouponsForStallFlow(stallId)
    }

    // --- Authentication Actions ---
    suspend fun getStudentByEmail(email: String): StudentEntity? {
        return studentDao.getStudentByEmail(email)
    }

    suspend fun registerStudent(student: StudentEntity) {
        studentDao.insertStudent(student)
        logAudit("STUDENT_REGISTER", student.email, "Student registered successfully: ${student.name}")
    }

    suspend fun getOrganizerByEmail(email: String): OrganizerEntity? {
        return organizerDao.getOrganizerByEmail(email)
    }

    suspend fun registerOrganizer(organizer: OrganizerEntity) {
        organizerDao.insertOrganizer(organizer)
        logAudit("ORGANIZER_REGISTER", organizer.email, "Organizer registered successfully: ${organizer.organizerName}")
    }

    suspend fun setOrganizerVerification(email: String, verified: Boolean) {
        organizerDao.setOrganizerVerification(email, verified)
        logAudit("ADMIN_VERIFY_ORGANIZER", "Admin", "Set organizer $email verification to $verified")
    }

    fun getAllOrganizersFlow(): Flow<List<OrganizerEntity>> = organizerDao.getAllOrganizersFlow()

    // --- Core CRUD Operations with History Logging ---
    suspend fun insertEvent(event: EventEntity, editorEmail: String, isUpdate: Boolean = false) {
        eventDao.insertEvent(event)
        val action = if (isUpdate) "UPDATE_EVENT" else "CREATE_EVENT"
        logAudit(action, editorEmail, "Event of ID ${event.id} with title '${event.title}' saved/updated successfully")
    }

    suspend fun getEventById(id: String): EventEntity? {
        return eventDao.getEventById(id)
    }

    suspend fun deleteEventById(id: String, editorEmail: String) {
        val event = eventDao.getEventById(id)
        if (event != null) {
            eventDao.deleteEventById(id)
            logAudit("DELETE_EVENT", editorEmail, "Deleted event '${event.title}' (ID: $id)")
        }
    }

    // --- Announcements Board ---
    suspend fun postAnnouncement(text: String, creatorEmail: String, creatorName: String) {
        announcementDao.insertAnnouncement(AnnouncementEntity(text = text, creatorName = creatorName))
        logAudit("ANNOUNCEMENT_POST", creatorEmail, "Posted announcement: '$text'")
    }

    // --- Event Registration Flow & Coupon Pre-Purchase Validation ---
    suspend fun checkStudentRegistration(studentEmail: String, eventId: String): Boolean {
        val activeReg = registrationDao.getActiveRegistration(studentEmail, eventId)
        return activeReg != null
    }

    suspend fun registerForEvent(
        studentEmail: String,
        eventId: String,
        paymentMethod: String,
        amountPaid: Double
    ): RegistrationEntity? {
        val event = eventDao.getEventById(eventId) ?: return null
        val regId = "REG-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
        val qrCodeString = "TICKET:$studentEmail:$eventId:$regId"
        
        val registration = RegistrationEntity(
            id = regId,
            studentEmail = studentEmail,
            eventId = eventId,
            eventTitle = event.title,
            regFee = amountPaid,
            paymentMethod = paymentMethod,
            status = "PAID",
            qrCodeString = qrCodeString
        )
        
        registrationDao.insertRegistration(registration)
        
        // Decrement seats available elegantly
        val updatedEvent = event.copy(seatsLeft = (event.seatsLeft - 1).coerceAtLeast(0))
        eventDao.insertEvent(updatedEvent)

        logAudit("REGISTRATION", studentEmail, "Successfully registered for event '${event.title}'. Ticket: $regId. Paid: $amountPaid INR.")
        return registration
    }

    // Validate registration before coupon purchase
    suspend fun buyFoodCoupon(
        studentEmail: String,
        couponId: String,
        stallId: String,
        paymentMethod: String
    ): Pair<Boolean, String> {
        val coupon = foodCouponDao.getCouponById(couponId) ?: return Pair(false, "Coupon not found")
        
        // Rule check: Must be registered for at least ONE event to purchase a coupon!
        val registrations = registrationDao.getRegistrationsForStudentFlow(studentEmail).first()
        val hasActiveRegistrations = registrations.any { it.status == "PAID" }
        
        if (!hasActiveRegistrations) {
            return Pair(false, "Validation Rule Failed: You must register for at least one Campus event before pre-booking food coupons!")
        }

        // Buy coupon by creating a specialized ticket in registrations
        val regId = "COUP-${UUID.randomUUID().toString().substring(0, 8).uppercase()}"
        val qrCodeString = "FOOD_COUPON:$studentEmail:$stallId:$couponId:$regId"
        
        val couponRegistration = RegistrationEntity(
            id = regId,
            studentEmail = studentEmail,
            eventId = "FOOD_COUPON",
            eventTitle = "Food Coupon: ${coupon.itemName}",
            regFee = coupon.price,
            paymentMethod = paymentMethod,
            status = "PAID",
            qrCodeString = qrCodeString,
            isPreBookedCoupon = true,
            preBookedCouponId = couponId
        )

        registrationDao.insertRegistration(couponRegistration)
        logAudit("FOOD_COUPON_PURCHASE", studentEmail, "Pre-booked food coupon '${coupon.itemName}' from Stall ID '$stallId'. Ticket: $regId")
        return Pair(true, "Purchase approved! Food coupon QR ticket generated: $regId")
    }

    suspend fun cancelRegistration(registrationId: String, studentEmail: String): Boolean {
        // Find existing registration
        // We can check local records.
        registrationDao.updateRegistrationStatus(registrationId, "CANCELLED")
        logAudit("CANCEL_REGISTRATION", studentEmail, "Cancelled registration: $registrationId")
        return true
    }

    // --- AI Chat Helper ---
    suspend fun sendChatMessage(userText: String): String {
        // Insert user message
        chatDao.insertChat(ChatEntity(messageText = userText, isUser = true))
        
        // Fetch chat history
        val history = chatDao.getAllChatsFlow().first()
        
        // Call actual Gemini API with fallback
        val aiReply = GeminiService.askGemini(userText, history)
        
        // Insert AI reply
        chatDao.insertChat(ChatEntity(messageText = aiReply, isUser = false))
        return aiReply
    }

    suspend fun clearChat() {
        chatDao.clearChats()
    }

    // --- Audit Logging Service ---
    suspend fun logAudit(actionType: String, actor: String, details: String) {
        val auditLog = AuditLogEntity(
            actionType = actionType,
            actorEmail = actor,
            details = details
        )
        auditLogDao.insertAuditLog(auditLog)
    }

    suspend fun likeFoodStall(stallId: String) {
        val list = foodStallDao.getAllFoodStallsFlow().first()
        val stall = list.firstOrNull { it.id == stallId }
        if (stall != null) {
            val updated = stall.copy(popularity = stall.popularity + 1)
            foodStallDao.insertFoodStall(updated)
            logAudit("FOOD_LIKE", "Student", "Liked stall '${stall.name}' (ID: $stallId)")
        }
    }

    suspend fun submitFoodFeedback(stallId: String, score: Double, comment: String, userEmail: String) {
        val list = foodStallDao.getAllFoodStallsFlow().first()
        val stall = list.firstOrNull { it.id == stallId }
        if (stall != null) {
            val currentRating = stall.rating
            val updatedRating = ((currentRating * 4 + score) / 5).coerceIn(1.0, 5.0)
            val formattedRating = Math.round(updatedRating * 10.0) / 10.0
            val updated = stall.copy(rating = formattedRating)
            foodStallDao.insertFoodStall(updated)
            logAudit("FOOD_FEEDBACK", userEmail, "Stall ID: $stallId | Score: $score | Review: $comment")
        }
    }

    // --- Offline Cache Initializer ---
    suspend fun populateInitialCacheIfNeeded() {
        // Check if DB is already populated by checking events count
        val existingEvents = eventDao.getAllEventsFlow().first()
        if (existingEvents.isNotEmpty()) return

        // 1. Insert initial food stalls
        val stall1 = FoodStallEntity("stall_1", "Nordic Waffles", "Desserts & Coffee", 4.9, "150m", 98)
        val stall2 = FoodStallEntity("stall_2", "Cafe Hygge & Crepes", "Continental", 4.8, "200m", 95)
        val stall3 = FoodStallEntity("stall_3", "Mumbai Street Masala", "Indian Fast Food", 4.6, "350m", 90)
        
        foodStallDao.insertFoodStall(stall1)
        foodStallDao.insertFoodStall(stall2)
        foodStallDao.insertFoodStall(stall3)

        // 2. Insert initial coupons
        foodCouponDao.insertCoupon(FoodCouponEntity("coup_11", "stall_1", "Vanilla Waffle + Mocha Combo", 120.0, "Save 20%! Combo deal"))
        foodCouponDao.insertCoupon(FoodCouponEntity("coup_12", "stall_1", "Cinnamon Sweet Waffle", 80.0, "Early bird 10% off"))
        foodCouponDao.insertCoupon(FoodCouponEntity("coup_21", "stall_2", "Danish Hygge Platter", 180.0, "Limited Special!"))
        foodCouponDao.insertCoupon(FoodCouponEntity("coup_31", "stall_3", "Indian Samosa & Chai Combo", 60.0, "Popular selection"))
        foodCouponDao.insertCoupon(FoodCouponEntity("coup_32", "stall_3", "Cheese Pav Bhaji Special", 100.0, "No offers"))

        // 3. Insert pre-seeded awesome college events
        val event1 = EventEntity(
            id = "event_tech_1",
            title = "Global Hackathon 2026",
            description = "The ultimate 36-hour hackathon to build revolutionary AI tech for campus sustainability.",
            isTechnical = true,
            collegeName = "Nordic University of Technology",
            date = "2026-06-25",
            venue = "Main Tech Auditorium",
            regFee = 250.0,
            prizeMoney = 100000.0,
            capacity = 500,
            seatsLeft = 412,
            guestSpeakers = "Sundar Pichai (Google), Yann LeCun (Meta)",
            rules = "Teams of 2-4. Original code only. Must build an application with AI functionality.",
            organizerEmail = "admin@nordicuni.edu"
        )

        val event2 = EventEntity(
            id = "event_tech_2",
            title = "Autonomous AI Robotics Challenge",
            description = "Build and code search-and-rescue obstacle rovers to race through the dynamic physics laboratory sand-pit.",
            isTechnical = true,
            collegeName = "Massachusetts Institute of Coding & Arts",
            date = "2026-06-28",
            venue = "Robotics Lab Sandbox",
            regFee = 150.0,
            prizeMoney = 50000.0,
            capacity = 100,
            seatsLeft = 67,
            guestSpeakers = "Marc Raibert (Boston Dynamics)",
            rules = "Rovers must be self-configured. External remote control is strictly prohibited.",
            organizerEmail = "roboclub@mit.edu"
        )

        val event3 = EventEntity(
            id = "event_nontech_1",
            title = "Symphony Night: Classical & Indie Concert",
            description = "Unplugged acoustic and orchestra night. Experience campus talent creating pure bliss under solar lanterns.",
            isTechnical = false,
            collegeName = "Nordic University of Technology",
            date = "2026-06-24",
            venue = "Lakeside Amphitheater",
            regFee = 0.0,
            prizeMoney = 25000.0,
            capacity = 1000,
            seatsLeft = 890,
            guestSpeakers = "Billie Eilish (Special Guest Singer)",
            rules = "Entry passes are mandatory. Outside instruments are allowed but must be pre-registered.",
            organizerEmail = "arts@nordicuni.edu"
        )

        val event4 = EventEntity(
            id = "event_nontech_2",
            title = "Chamber of Mysteries: Treasure Hunt",
            description = "Decode 10 cryptological clues hidden across historic campus sites to locate the legendary Pulse Grail.",
            isTechnical = false,
            collegeName = "Silicon Valley Technical Institute",
            date = "2026-06-26",
            venue = "Campus Library Lawn (Start)",
            regFee = 50.0,
            prizeMoney = 15000.0,
            capacity = 300,
            seatsLeft = 143,
            guestSpeakers = "Dan Brown (Novelist)",
            rules = "Teams of up to 3. All physical items must remain in their original positions.",
            organizerEmail = "mysteryclub@svti.edu"
        )

        val event5 = EventEntity(
            id = "event_tech_3",
            title = "Poster Design Masterclass & Project Expo",
            description = "Exhibit engineering posters to industry experts and master UI/UX prototyping through interactive sessions.",
            isTechnical = true,
            collegeName = "Nordic University of Technology",
            date = "2026-06-29",
            venue = "Aesthetics Design Wing",
            regFee = 100.0,
            prizeMoney = 30000.0,
            capacity = 200,
            seatsLeft = 188,
            guestSpeakers = "Don Norman (Design Maven)",
            rules = "Standard size A1 board. Design prototypes must compile digitally.",
            organizerEmail = "design@nordicuni.edu"
        )

        eventDao.insertEvent(event1)
        eventDao.insertEvent(event2)
        eventDao.insertEvent(event3)
        eventDao.insertEvent(event4)
        eventDao.insertEvent(event5)

        // 4. Seeding some initial Announcements
        announcementDao.insertAnnouncement(AnnouncementEntity(text = "Hackathon starts in 10 minutes at the Main Tech Auditorium!", creatorName = "MIT Admin"))
        announcementDao.insertAnnouncement(AnnouncementEntity(text = "Symphony Concert sound-checks are starting at Lakeside Amphitheater.", creatorName = "Arts Coordinator"))
        announcementDao.insertAnnouncement(AnnouncementEntity(text = "Danish Hygge Platter combo is at 20% Discount for all users registered in Symphony Night!", creatorName = "Nordic Waffles Head"))

        // 5. Initial organizer registered (so users can login directly using mock or create new ones)
        val defaultOrganizer = OrganizerEntity(
            email = "admin@nordicuni.edu",
            organizerName = "Dr. Aaron Finch",
            collegeName = "Nordic University of Technology",
            contactNo = "+45 2291039",
            eventName = "Admin Coordinator",
            verified = true
        )
        organizerDao.insertOrganizer(defaultOrganizer)

        logAudit("INITIAL_SEED", "SYSTEM", "Pre-seeded food stalls, coupons, and premium college events in local cache.")
    }
}
