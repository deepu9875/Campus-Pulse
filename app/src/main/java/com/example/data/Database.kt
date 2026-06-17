package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        StudentEntity::class,
        OrganizerEntity::class,
        EventEntity::class,
        FoodStallEntity::class,
        FoodCouponEntity::class,
        AnnouncementEntity::class,
        RegistrationEntity::class,
        ChatEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CampusPulseDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun organizerDao(): OrganizerDao
    abstract fun eventDao(): EventDao
    abstract fun foodStallDao(): FoodStallDao
    abstract fun foodCouponDao(): FoodCouponDao
    abstract fun announcementDao(): AnnouncementDao
    abstract fun registrationDao(): RegistrationDao
    abstract fun chatDao(): ChatDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: CampusPulseDatabase? = null

        fun getDatabase(context: Context): CampusPulseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CampusPulseDatabase::class.java,
                    "campus_pulse_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
