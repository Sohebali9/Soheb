package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "user_profile")
@JsonClass(generateAdapter = true)
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "Aspirant",
    val targetExam: String = "NEET PG", // MBBS University, NEET PG, INICET, UPSC, SSC, State PCS
    val xpPoints: Int = 150,
    val studyStreak: Int = 1,
    val lastStudyTimestamp: Long = System.currentTimeMillis(),
    val tier: Int = 0, // 0 = Free, 1 = Premium
    val readinessScore: Int = 75 // Percentage estimation
)

@Entity(tableName = "pyq_predictions")
@JsonClass(generateAdapter = true)
data class PyqPrediction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val exam: String,
    val subject: String,
    val topic: String,
    val question: String,
    val repetitionCount: String, // e.g. "7 times in 10 years"
    val probability: String, // "High", "Medium", "Low"
    val justification: String,
    val modelAnswer: String,
    val isMockParsed: Boolean = false
)

@Entity(tableName = "flashcards")
@JsonClass(generateAdapter = true)
data class Flashcard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val question: String,
    val answer: String,
    val mnemonic: String? = null,
    val subject: String,
    val topic: String,
    val reviewCount: Int = 0,
    val lastReviewedTimestamp: Long = 0L
)

@Entity(tableName = "weakness_topics")
@JsonClass(generateAdapter = true)
data class WeaknessTopic(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val topic: String,
    val weaknessDescription: String,
    val dailyRevisionPlan: String,
    val timeSpentSeconds: Long = 0L,
    val wrongQuestionsCount: Int = 0,
    val priorityLevel: String = "Medium" // High, Medium, Low
)

@Entity(tableName = "community_posts")
@JsonClass(generateAdapter = true)
data class CommunityPost(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examType: String,
    val subject: String,
    val topic: String,
    val author: String,
    val title: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotes: Int = 1,
    val downvotes: Int = 0,
    val commentCount: Int = 0,
    val predictionIdRef: Int? = null
)

@Entity(tableName = "community_comments")
@JsonClass(generateAdapter = true)
data class CommunityComment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val author: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val upvotes: Int = 1
)

@Entity(tableName = "mock_tests")
@JsonClass(generateAdapter = true)
data class MockTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val examType: String,
    val title: String,
    val totalQuestions: Int,
    val timeLimitMinutes: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val scoreObtained: Int = 0,
    val timeSpentSeconds: Int = 0,
    val accuracyPercentage: Int = 0
)

@Entity(tableName = "mock_questions")
@JsonClass(generateAdapter = true)
data class MockQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mockTestId: Int,
    val subject: String,
    val topic: String,
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val userAnswer: String? = null,
    val explanation: String
)

