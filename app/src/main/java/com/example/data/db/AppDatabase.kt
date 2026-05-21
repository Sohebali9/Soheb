package com.example.data.db

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import com.example.data.model.CommunityComment
import com.example.data.model.CommunityPost
import com.example.data.model.Flashcard
import com.example.data.model.MockQuestion
import com.example.data.model.MockTest
import com.example.data.model.PyqPrediction
import com.example.data.model.UserProfile
import com.example.data.model.WeaknessTopic
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamDao {
    // User Profile
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    // PYQ Predictions
    @Query("SELECT * FROM pyq_predictions ORDER BY id DESC")
    fun getAllPredictionsFlow(): Flow<List<PyqPrediction>>

    @Query("SELECT * FROM pyq_predictions WHERE exam = :exam ORDER BY id DESC")
    fun getPredictionsForExamFlow(exam: String): Flow<List<PyqPrediction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PyqPrediction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPredictions(predictions: List<PyqPrediction>)

    @Query("DELETE FROM pyq_predictions")
    suspend fun clearAllPredictions()

    // Flashcards
    @Query("SELECT * FROM flashcards ORDER BY id DESC")
    fun getAllFlashcardsFlow(): Flow<List<Flashcard>>

    @Query("SELECT * FROM flashcards WHERE subject = :subject ORDER BY id DESC")
    fun getFlashcardsBySubjectFlow(subject: String): Flow<List<Flashcard>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlashcard(flashcard: Flashcard)

    @Query("DELETE FROM flashcards WHERE id = :id")
    suspend fun deleteFlashcardById(id: Int)

    // Weakness Analyzer
    @Query("SELECT * FROM weakness_topics ORDER BY priorityLevel DESC, id DESC")
    fun getAllWeaknessTopicsFlow(): Flow<List<WeaknessTopic>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeaknessTopic(topic: WeaknessTopic)

    @Query("DELETE FROM weakness_topics WHERE id = :id")
    suspend fun deleteWeaknessTopicById(id: Int)

    @Query("DELETE FROM weakness_topics")
    suspend fun clearAllWeaknessTopics()

    // Community Forum Posts
    @Query("SELECT * FROM community_posts ORDER BY id DESC")
    fun getAllPostsFlow(): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_posts WHERE examType = :examType ORDER BY upvotes - downvotes DESC, id DESC")
    fun getPostsForExamFlow(examType: String): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_posts WHERE id = :id LIMIT 1")
    suspend fun getPostById(id: Int): CommunityPost?

    @Query("SELECT * FROM community_posts WHERE id = :id LIMIT 1")
    fun getPostByIdFlow(id: Int): Flow<CommunityPost?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPost): Long

    @Query("DELETE FROM community_posts WHERE id = :id")
    suspend fun deletePostById(id: Int)

    // Community Forum Comments
    @Query("SELECT * FROM community_comments WHERE postId = :postId ORDER BY id ASC")
    fun getCommentsForPostFlow(postId: Int): Flow<List<CommunityComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: CommunityComment): Long

    // Mock Tests
    @Query("SELECT * FROM mock_tests ORDER BY id DESC")
    fun getAllMockTestsFlow(): Flow<List<MockTest>>

    @Query("SELECT * FROM mock_tests WHERE id = :testId LIMIT 1")
    suspend fun getMockTestById(testId: Int): MockTest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockTest(test: MockTest): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockQuestions(questions: List<MockQuestion>)

    @Query("SELECT * FROM mock_questions WHERE mockTestId = :testId ORDER BY id ASC")
    fun getQuestionsForTestFlow(testId: Int): Flow<List<MockQuestion>>

    @Query("SELECT * FROM mock_questions WHERE id = :questionId LIMIT 1")
    suspend fun getMockQuestionById(questionId: Int): MockQuestion?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMockQuestion(question: MockQuestion)
}

@Database(
    entities = [
        UserProfile::class,
        PyqPrediction::class,
        Flashcard::class,
        WeaknessTopic::class,
        CommunityPost::class,
        CommunityComment::class,
        MockTest::class,
        MockQuestion::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun examDao(): ExamDao
}
