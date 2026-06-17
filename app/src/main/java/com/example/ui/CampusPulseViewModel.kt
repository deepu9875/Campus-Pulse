package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface CurrentUser {
    object Guest : CurrentUser
    data class Student(val info: StudentEntity) : CurrentUser
    data class Organizer(val info: OrganizerEntity) : CurrentUser
    object Admin : CurrentUser
}

class CampusPulseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CampusPulseRepository(application)

    // --- State Streams ---
    private val _userState = MutableStateFlow<CurrentUser>(CurrentUser.Guest)
    val userState: StateFlow<CurrentUser> = _userState.asStateFlow()

    private val _themeMode = MutableStateFlow<String>("system") // "system", "light", "dark"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    fun setThemeMode(mode: String) {
        _themeMode.value = mode
    }

    private val _splashSoundEnabled = MutableStateFlow<Boolean>(true)
    val splashSoundEnabled: StateFlow<Boolean> = _splashSoundEnabled.asStateFlow()

    fun setSplashSoundEnabled(enabled: Boolean) {
        _splashSoundEnabled.value = enabled
    }

    private val _notifications = MutableStateFlow<List<String>>(
        listOf(
            "Welcome to Campus Pulse! Check out the featured hackathons.",
            "Reminder: Standard organizer approvals must be certified by administration coordinators."
        )
    )
    val notifications: StateFlow<List<String>> = _notifications.asStateFlow()

    // Cache pre-population status
    private val _isCacheReady = MutableStateFlow(false)
    val isCacheReady: StateFlow<Boolean> = _isCacheReady.asStateFlow()

    // --- Flows from Room Database ---
    val allEvents: StateFlow<List<EventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val technicalEvents: StateFlow<List<EventEntity>> = repository.technicalEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val nonTechnicalEvents: StateFlow<List<EventEntity>> = repository.nonTechnicalEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val foodStalls: StateFlow<List<FoodStallEntity>> = repository.allFoodStallsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val announcements: StateFlow<List<AnnouncementEntity>> = repository.allAnnouncementsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<AuditLogEntity>> = repository.allAuditLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatEntity>> = repository.allChatsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOrganizers: StateFlow<List<OrganizerEntity>> = repository.getAllOrganizersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRegistrations: StateFlow<List<RegistrationEntity>> = repository.allRegistrationsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPayments: StateFlow<List<PaymentEntity>> = repository.allPaymentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Registrations stream dependent on active student email
    val studentRegistrations: StateFlow<List<RegistrationEntity>> = _userState
        .flatMapLatest { state ->
            if (state is CurrentUser.Student) {
                repository.getRegistrationsForStudent(state.info.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studentPayments: StateFlow<List<PaymentEntity>> = _userState
        .flatMapLatest { state ->
            if (state is CurrentUser.Student) {
                repository.getPaymentsForStudent(state.info.email)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Run database caching routines early in a coroutine
        viewModelScope.launch {
            repository.populateInitialCacheIfNeeded()
            _isCacheReady.value = true
        }
    }

    // --- Notification Helper ---
    fun addNotification(text: String) {
        _notifications.update { listOf(text) + it }
    }

    // --- Authentication Actions (Student / Organizer / Admin) ---
    fun selectStudentLogin(email: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val student = repository.getStudentByEmail(email)
            if (student != null) {
                _userState.value = CurrentUser.Student(student)
                addNotification("Signed in successfully as ${student.name}.")
                onComplete(true, "Welcome back, ${student.name}!")
            } else {
                onComplete(false, "Student account not found on campus local server. Please register first!")
            }
        }
    }

    fun registerStudent(
        name: String,
        email: String,
        collegeName: String,
        stream: String,
        year: String,
        contactNo: String,
        regNo: String,
        logoUrl: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isEmpty() || email.isEmpty() || collegeName.isEmpty() || regNo.isEmpty()) {
                onComplete(false, "Please complete all mandatory student fields!")
                return@launch
            }
            val student = StudentEntity(
                email = email,
                name = name,
                collegeName = collegeName,
                stream = stream,
                year = year,
                contactNo = contactNo,
                regNo = regNo,
                profilePhoto = logoUrl
            )
            repository.registerStudent(student)
            _userState.value = CurrentUser.Student(student)
            addNotification("Account created! Student registration verified.")
            onComplete(true, "Student registered and logged in successfully!")
        }
    }

    fun selectOrganizerLogin(email: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val organizer = repository.getOrganizerByEmail(email)
            if (organizer != null) {
                _userState.value = CurrentUser.Organizer(organizer)
                addNotification("Organizer session started for ${organizer.organizerName}.")
                onComplete(true, "LoggedIn as Organizer: ${organizer.organizerName}")
            } else {
                onComplete(false, "Official organizer account not registered yet!")
            }
        }
    }

    fun registerOrganizer(
        name: String,
        email: String,
        collegeName: String,
        contactNo: String,
        eventName: String,
        photoUrl: String = "",
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (name.isEmpty() || email.isEmpty() || collegeName.isEmpty()) {
                onComplete(false, "Please fill in official college coordinates!")
                return@launch
            }
            val organizer = OrganizerEntity(
                email = email,
                organizerName = name,
                collegeName = collegeName,
                contactNo = contactNo,
                eventName = eventName,
                profilePicture = photoUrl,
                verified = false // Admin approves
            )
            repository.registerOrganizer(organizer)
            _userState.value = CurrentUser.Organizer(organizer)
            addNotification("Organizer registered. Pending admin verification.")
            onComplete(true, "Official organizer credentials submitted successfully!")
        }
    }

    fun loginAsAdmin() {
        _userState.value = CurrentUser.Admin
        addNotification("Super Admin developer console activated.")
    }

    fun logout() {
        _userState.value = CurrentUser.Guest
        addNotification("Logged out successfully.")
    }

    // --- Developer Admin actions ---
    fun toggleOrganizerVerification(email: String, verified: Boolean) {
        viewModelScope.launch {
            repository.setOrganizerVerification(email, verified)
            addNotification("Organizer ($email) verification set to $verified")
        }
    }

    // --- Event CRUD Actions (Organizer only) ---
    fun saveEvent(
        title: String,
        description: String,
        isTechnical: Boolean,
        collegeName: String,
        date: String,
        venue: String,
        regFee: Double,
        prizeMoney: Double,
        capacity: Int,
        guestSpeakers: String,
        rules: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        val user = _userState.value
        if (user !is CurrentUser.Organizer) {
            onComplete(false, "Access Denied: Only registered organizers can edit events.")
            return
        }

        viewModelScope.launch {
            if (title.isEmpty() || description.isEmpty() || date.isEmpty() || venue.isEmpty()) {
                onComplete(false, "Mandatory physical coordinates must be supplied!")
                return@launch
            }

            val eventId = "EVENT-" + UUID.randomUUID().toString().substring(0, 6).uppercase()
            val newEvent = EventEntity(
                id = eventId,
                title = title,
                description = description,
                isTechnical = isTechnical,
                collegeName = collegeName,
                date = date,
                venue = venue,
                regFee = regFee,
                prizeMoney = prizeMoney,
                capacity = capacity,
                seatsLeft = capacity,
                guestSpeakers = guestSpeakers,
                rules = rules,
                organizerEmail = user.info.email
            )
            repository.insertEvent(newEvent, user.info.email, isUpdate = false)
            addNotification("New event '${title}' posted on Campus Pulse!")
            onComplete(true, "Event successfully added!")
        }
    }

    fun deleteEvent(eventId: String) {
        val user = _userState.value
        val editorEmail = when (user) {
            is CurrentUser.Organizer -> user.info.email
            is CurrentUser.Admin -> "SuperAdmin"
            else -> return
        }
        viewModelScope.launch {
            repository.deleteEventById(eventId, editorEmail)
            addNotification("Event was successfully removed from Campus Pulse directory.")
        }
    }

    // --- Notice Board / Scrolling update (Organizer only) ---
    fun postAnnouncement(text: String, onComplete: (Boolean, String) -> Unit) {
        val user = _userState.value
        if (user !is CurrentUser.Organizer) {
            onComplete(false, "Only organizers can post live scroll updates.")
            return
        }
        if (text.isEmpty()) {
            onComplete(false, "Announcement content is empty!")
            return
        }

        viewModelScope.launch {
            repository.postAnnouncement(text, user.info.email, user.info.organizerName)
            addNotification("New Live Notice: $text")
            onComplete(true, "Notice published!")
        }
    }

    // --- Interactive Registration (Payments and Confetti validation) ---
    fun registerForEvent(eventId: String, paymentMethod: String, onComplete: (Boolean, String) -> Unit) {
        val user = _userState.value
        if (user !is CurrentUser.Student) {
            onComplete(false, "Authentication required: Please sign in as a Student to register.")
            return
        }

        viewModelScope.launch {
            // Check if already registered
            val alreadyRegistered = repository.checkStudentRegistration(user.info.email, eventId)
            if (alreadyRegistered) {
                onComplete(false, "You are already registered for this event!")
                return@launch
            }

            val event = repository.getEventById(eventId)
            if (event == null) {
                onComplete(false, "Event details not found on local instance.")
                return@launch
            }

            if (event.seatsLeft <= 0) {
                onComplete(false, "Sorry, all seats for this event are fully booked!")
                return@launch
            }

            val ticket = repository.registerForEvent(
                studentEmail = user.info.email,
                eventId = eventId,
                paymentMethod = paymentMethod,
                amountPaid = event.regFee
            )

            if (ticket != null) {
                addNotification("Registered for ${event.title}! Your ticket is ${ticket.id}.")
                onComplete(true, "Registration success! Code: ${ticket.id}")
            } else {
                onComplete(false, "Encountered server error checking payment secure logs.")
            }
        }
    }

    fun registerForEventSecure(
        eventId: String,
        paymentId: String,
        gateway: String,
        amountFilled: Double,
        paymentSuccess: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        val user = _userState.value
        if (user !is CurrentUser.Student) {
            onComplete(false, "Authentication required: Please sign in as a Student to register.")
            return
        }

        viewModelScope.launch {
            val alreadyRegistered = repository.checkStudentRegistration(user.info.email, eventId)
            if (alreadyRegistered) {
                onComplete(false, "You are already registered for this event!")
                return@launch
            }

            val event = repository.getEventById(eventId)
            if (event == null) {
                onComplete(false, "Event details not found.")
                return@launch
            }

            if (event.seatsLeft <= 0) {
                onComplete(false, "Sorry, all seats for this event are fully booked!")
                return@launch
            }

            // Perform payment verification and record details
            val ticket = repository.registerForEventWithPayment(
                studentEmail = user.info.email,
                eventId = eventId,
                paymentId = paymentId,
                gateway = gateway,
                amountPaid = amountFilled,
                paymentSuccess = paymentSuccess
            )

            if (ticket != null) {
                addNotification("Registered for ${event.title}! Your unique registration ID is ${ticket.id}.")
                onComplete(true, "Registration success! Code: ${ticket.id}")
            } else {
                onComplete(false, "Payment verification unsuccessful or cancelled.")
            }
        }
    }

    fun buyFoodCoupon(couponId: String, stallId: String, paymentMethod: String, onComplete: (Boolean, String) -> Unit) {
        val user = _userState.value
        if (user !is CurrentUser.Student) {
            onComplete(false, "Student account is required to book food coupons!")
            return
        }

        viewModelScope.launch {
            val result = repository.buyFoodCoupon(
                studentEmail = user.info.email,
                couponId = couponId,
                stallId = stallId,
                paymentMethod = paymentMethod
            )
            if (result.first) {
                addNotification("Food Coupon purchased. Secure QR added to your tickets!")
                onComplete(true, result.second)
            } else {
                onComplete(false, result.second)
            }
        }
    }

    fun buyFoodCouponSecure(
        couponId: String,
        stallId: String,
        gateway: String,
        paymentId: String,
        paymentSuccess: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        val user = _userState.value
        if (user !is CurrentUser.Student) {
            onComplete(false, "Student account is required to buy food coupons!")
            return
        }

        viewModelScope.launch {
            val result = repository.buyFoodCouponWithPayment(
                studentEmail = user.info.email,
                couponId = couponId,
                stallId = stallId,
                gateway = gateway,
                paymentId = paymentId,
                paymentSuccess = paymentSuccess
            )
            if (result.first) {
                addNotification("Food Coupon pre-booked! Under secure Pay ID: $paymentId")
                onComplete(true, result.second)
            } else {
                onComplete(false, result.second)
            }
        }
    }

    fun likeFoodStall(stallId: String) {
        viewModelScope.launch {
            repository.likeFoodStall(stallId)
            addNotification("You liked a food stall!")
        }
    }

    fun submitFoodFeedback(stallId: String, score: Double, comment: String, onComplete: (Boolean, String) -> Unit) {
        val user = _userState.value
        val email = if (user is CurrentUser.Student) user.info.email else "guest@campus.edu"
        viewModelScope.launch {
            repository.submitFoodFeedback(stallId, score, comment, email)
            onComplete(true, "Thank you! Your feedback for Stall ID $stallId has been posted.")
        }
    }

    fun requestCancelRegistration(registrationId: String) {
        val user = _userState.value
        if (user !is CurrentUser.Student) return
        viewModelScope.launch {
            repository.cancelRegistration(registrationId, user.info.email)
            addNotification("Ticket cancellation initiated. Credit refund is being processed.")
        }
    }

    // --- Interactive Chatroom (AI Assistant REST integration) ---
    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading: StateFlow<Boolean> = _isChatLoading.asStateFlow()

    fun sendChatMessage(text: String) {
        if (text.trim().isEmpty()) return
        _isChatLoading.value = true
        viewModelScope.launch {
            repository.sendChatMessage(text)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            repository.clearChat()
            addNotification("Chat history cleared.")
        }
    }

    fun getCouponsForStall(stallId: String): Flow<List<FoodCouponEntity>> {
        return repository.getCouponsForStall(stallId)
    }
}
