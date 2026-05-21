package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.model.Flashcard
import com.example.data.model.PyqPrediction
import com.example.data.model.UserProfile
import com.example.data.model.WeaknessTopic
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityComment
import com.example.data.model.MockTest
import com.example.data.model.MockQuestion
import com.example.data.repository.ExamRepository
import com.example.data.repository.VivaEvaluation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExamViewModel(application: Application) : AndroidViewModel(application) {

    private val db = Room.databaseBuilder(
        application,
        AppDatabase::class.java, "pyq_predictor_database"
    ).fallbackToDestructiveMigration(true).build()

    private val repository = ExamRepository(db.examDao())

    // Profile Flows
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Current Exam Target Flow
    val activeExam: StateFlow<String> = MutableStateFlow("Class 10 CBSE") // Initial helper

    // Flatmapped Predictions Flow depending on target exam
    private val _selectedExam = MutableStateFlow("Class 10 CBSE")
    val selectedExam = _selectedExam.asStateFlow()

    val predictions: StateFlow<List<PyqPrediction>> = _selectedExam
        .flatMapLatest { exam -> repository.getPredictionsForExam(exam) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flashcards & Weakness Lists
    val flashcardsFlow: StateFlow<List<Flashcard>> = repository.allFlashcards
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weaknessFlow: StateFlow<List<WeaknessTopic>> = repository.weaknessTopics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Screen states
    private val _uploadingStatus = MutableStateFlow<String?>(null)
    val uploadingStatus: StateFlow<String?> = _uploadingStatus.asStateFlow()

    private val _isProcessingJob = MutableStateFlow(false)
    val isProcessingJob: StateFlow<Boolean> = _isProcessingJob.asStateFlow()

    // --- VIVA SIMULATOR SYSTEM ---
    private val _vivaMode = MutableStateFlow("Professor Mode") // Easy, Professor Mode, Rapid-fire Mode
    val vivaMode: StateFlow<String> = _vivaMode.asStateFlow()

    private val _vivaQuestionIndex = MutableStateFlow(0)
    val vivaQuestionIndex: StateFlow<Int> = _vivaQuestionIndex.asStateFlow()

    private val _vivaUserDraft = MutableStateFlow("")
    val vivaUserDraft: StateFlow<String> = _vivaUserDraft.asStateFlow()

    private val _vivaEvaluationResult = MutableStateFlow<VivaEvaluation?>(null)
    val vivaEvaluationResult: StateFlow<VivaEvaluation?> = _vivaEvaluationResult.asStateFlow()

    private val _isEvaluatingViva = MutableStateFlow(false)
    val isEvaluatingViva: StateFlow<Boolean> = _isEvaluatingViva.asStateFlow()

    // --- STUDY FORUMS ENGINE ---
    private val _selectedForumSubgroup = MutableStateFlow("Class 10 CBSE")
    val selectedForumSubgroup = _selectedForumSubgroup.asStateFlow()

    val forumPosts: StateFlow<List<com.example.data.model.CommunityPost>> = _selectedForumSubgroup
        .flatMapLatest { exam -> repository.getPostsForExam(exam) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeThreadId = MutableStateFlow<Int?>(null)
    val activeThreadId = _activeThreadId.asStateFlow()

    private val _activeThreadPost = MutableStateFlow<com.example.data.model.CommunityPost?>(null)
    val activeThreadPost = _activeThreadPost.asStateFlow()

    val threadComments: StateFlow<List<com.example.data.model.CommunityComment>> = _activeThreadId
        .flatMapLatest { postId -> 
            if (postId != null) repository.allComments(postId) else kotlinx.coroutines.flow.flowOf(emptyList<com.example.data.model.CommunityComment>())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- AI MOCK TEST ENGINE STATE ---
    val mockTests: StateFlow<List<com.example.data.model.MockTest>> = repository.allMockTestsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeMockTestId = MutableStateFlow<Int?>(null)
    val activeMockTestId = _activeMockTestId.asStateFlow()

    private val _activeMockTestObj = MutableStateFlow<com.example.data.model.MockTest?>(null)
    val activeMockTestObj = _activeMockTestObj.asStateFlow()

    private val _mockQuestions = MutableStateFlow<List<com.example.data.model.MockQuestion>>(emptyList())
    val mockQuestions = _mockQuestions.asStateFlow()

    private val _currentMockQuestionIndex = MutableStateFlow(0)
    val currentMockQuestionIndex = _currentMockQuestionIndex.asStateFlow()

    private val _mockAnswersMap = MutableStateFlow<Map<Int, String>>(emptyMap())
    val mockAnswersMap = _mockAnswersMap.asStateFlow()

    private val _isGeneratingMockTest = MutableStateFlow(false)
    val isGeneratingMockTest = _isGeneratingMockTest.asStateFlow()

    private val _mockTestTimerRemainingSeconds = MutableStateFlow(0)
    val mockTestTimerRemainingSeconds = _mockTestTimerRemainingSeconds.asStateFlow()

    private var countdownJob: kotlinx.coroutines.Job? = null

    init {
        viewModelScope.launch {
            // Instantiate profile on launches to seed standard experience
            val profile = repository.getOrCreateProfile()
            _selectedExam.value = profile.targetExam
            _selectedForumSubgroup.value = profile.targetExam
            
            // Seed initial threads if forum is empty
            repository.seedInitialPostsIfEmpty()

            // Seed a few default predictions if empty
            db.examDao().getAllPredictionsFlow().collect { currentList ->
                if (currentList.isEmpty()) {
                    // Prepopulate with a helpful starter selection of questions
                    repository.analyzePyqs(profile.targetExam, "Standard PYQ Syllabus Guidelines", "Default syllabus overview")
                }
            }
        }
    }

    fun setTargetExam(exam: String) {
        _selectedExam.value = exam
        _selectedForumSubgroup.value = exam
        viewModelScope.launch {
            repository.updateTargetExam(exam)
            // Trigger auto seeding or reset of initial questions for this syllabus
            repository.analyzePyqs(exam, "Standard PYQ Syllabus Guidelines", "Default syllabus overview")
        }
    }

    fun setProfileName(name: String) {
        viewModelScope.launch {
            val prof = repository.getOrCreateProfile()
            repository.updateProfile(prof.copy(name = name))
        }
    }

    fun makePremium(isPremium: Boolean) {
        viewModelScope.launch {
            repository.togglePremium(isPremium)
        }
    }

    // --- PYQ UPLOAD METRIC SIMULATION ---
    fun simulatePyqUpload(
        sourceTitle: String,
        documentText: String
    ) {
        _isProcessingJob.value = true
        _uploadingStatus.value = "Starting AI PYQ analysis on paper..."
        viewModelScope.launch {
            try {
                val feedback = repository.analyzePyqs(_selectedExam.value, sourceTitle, documentText)
                _uploadingStatus.value = feedback
                repository.addXpAndCheckStreak(40) // Give 40 XP incentive!
            } catch (e: Exception) {
                _uploadingStatus.value = "Error parsing file: ${e.message}"
            } finally {
                _isProcessingJob.value = false
            }
        }
    }

    fun clearUploadStatus() {
        _uploadingStatus.value = null
    }

    // --- FLASHCARD ENGINE ---
    fun generateFlashcardsForPrediction(subject: String, topic: String) {
        _isProcessingJob.value = true
        viewModelScope.launch {
            try {
                repository.generateFlashcardsForTopic(subject, topic)
                repository.addXpAndCheckStreak(25) // Card creation award!
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessingJob.value = false
            }
        }
    }

    fun addCustomFlashcard(question: String, answer: String, mnemonic: String?, subject: String, topic: String) {
        viewModelScope.launch {
            db.examDao().insertFlashcard(
                Flashcard(
                    question = question,
                    answer = answer,
                    mnemonic = if (mnemonic?.trim().isNullOrEmpty()) null else mnemonic,
                    subject = subject,
                    topic = topic
                )
            )
            repository.addXpAndCheckStreak(15)
        }
    }

    fun reviewFlashcard(id: Int) {
        viewModelScope.launch {
            repository.addXpAndCheckStreak(10) // 10 XP per cards reviewed
        }
    }

    fun deleteCard(id: Int) {
        viewModelScope.launch {
            repository.deleteFlashcard(id)
        }
    }

    // --- WEAKNESS ANALYZER ENGINE ---
    fun identifyWeaknessAndGeneratePlan(subject: String, topic: String, description: String) {
        _isProcessingJob.value = true
        viewModelScope.launch {
            try {
                repository.analyzeAndAddWeakness(subject, topic, description)
                repository.addXpAndCheckStreak(20) // Diagnostic planning award!
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isProcessingJob.value = false
            }
        }
    }

    fun resolveWeaknessNow(id: Int) {
        viewModelScope.launch {
            repository.solveWeakness(id)
            repository.addXpAndCheckStreak(50) // 50 XP award for conquering a revision subject!
        }
    }

    // --- VIVA SESSION ENGINE ---
    fun changeVivaMode(mode: String) {
        _vivaMode.value = mode
        _vivaEvaluationResult.value = null
        _vivaUserDraft.value = ""
    }

    fun updateVivaDraft(draft: String) {
        _vivaUserDraft.value = draft
    }

    fun resetVivaEvaluation() {
        _vivaEvaluationResult.value = null
        _vivaUserDraft.value = ""
    }

    fun nextVivaQuestion() {
        _vivaQuestionIndex.value = (_vivaQuestionIndex.value + 1) % getVivaQuestions().size
        resetVivaEvaluation()
    }

    fun submitVivaAnswer() {
        val activeQuestion = getVivaQuestions().getOrNull(_vivaQuestionIndex.value) ?: return
        val answer = _vivaUserDraft.value.trim()
        if (answer.isEmpty()) return

        _isEvaluatingViva.value = true
        viewModelScope.launch {
            try {
                val eval = repository.evaluateVivaAnswer(activeQuestion, answer, _vivaMode.value)
                _vivaEvaluationResult.value = eval
                if (eval.score >= 7) {
                    repository.addXpAndCheckStreak(35) // High score check bonus
                } else {
                    repository.addXpAndCheckStreak(15) // Participation bonus
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isEvaluatingViva.value = false
            }
        }
    }

    // --- STUDY FORUMS INTERACTIONS ---
    fun selectForumSubgroup(exam: String) {
        _selectedForumSubgroup.value = exam
    }

    fun selectThread(post: com.example.data.model.CommunityPost?) {
        _activeThreadPost.value = post
        _activeThreadId.value = post?.id
    }

    fun upvotePost(postId: Int) {
        viewModelScope.launch {
            repository.votePost(postId, up = true)
            // Refresh single post inside thread overview
            _activeThreadPost.value?.let { current ->
                if (current.id == postId) {
                    _activeThreadPost.value = current.copy(upvotes = current.upvotes + 1)
                }
            }
        }
    }

    fun downvotePost(postId: Int) {
        viewModelScope.launch {
            repository.votePost(postId, up = false)
            _activeThreadPost.value?.let { current ->
                if (current.id == postId) {
                    _activeThreadPost.value = current.copy(downvotes = current.downvotes + 1)
                }
            }
        }
    }

    fun createPost(subject: String, topic: String, title: String, content: String, predictionId: Int? = null) {
        viewModelScope.launch {
            val author = userProfile.value?.name ?: "Aspirant"
            repository.createCommunityPost(
                examType = _selectedForumSubgroup.value,
                subject = subject,
                topic = topic,
                author = author,
                title = title,
                content = content,
                predictionIdRef = predictionId
            )
            repository.addXpAndCheckStreak(20) // Award post creation!
        }
    }

    fun postComment(content: String) {
        val postId = _activeThreadId.value ?: return
        if (content.trim().isEmpty()) return
        viewModelScope.launch {
            val author = userProfile.value?.name ?: "Aspirant"
            repository.createCommunityComment(postId, author, content)
            repository.addXpAndCheckStreak(15) // Comment contribution points!
        }
    }

    fun navigateToDiscussion(prediction: com.example.data.model.PyqPrediction) {
        viewModelScope.launch {
            // Find if existing thread matches subject/topic
            val existingList = repository.getPostsForExam(prediction.exam).first()

            val existing = existingList.find { 
                it.topic.lowercase() == prediction.topic.lowercase() || it.title.lowercase().contains(prediction.topic.lowercase())
            }

            if (existing != null) {
                _selectedForumSubgroup.value = prediction.exam
                selectThread(existing)
            } else {
                // Auto create a matching prediction discussion thread
                val author = "AI_System"
                val newId = repository.createCommunityPost(
                    examType = prediction.exam,
                    subject = prediction.subject,
                    topic = prediction.topic,
                    author = author,
                    title = "Interactive Discussion: ${prediction.topic} [PYQ Analysis]",
                    content = "Discussion hub generated for predicted topic: ${prediction.topic}. ${prediction.justification}. How do you solve this question: '${prediction.question}'?",
                    predictionIdRef = prediction.id
                )
                _selectedForumSubgroup.value = prediction.exam
                selectThread(
                    com.example.data.model.CommunityPost(
                        id = newId.toInt(),
                        examType = prediction.exam,
                        subject = prediction.subject,
                        topic = prediction.topic,
                        author = author,
                        title = "Interactive Discussion: ${prediction.topic} [PYQ Analysis]",
                        content = "Discussion hub generated for predicted topic: ${prediction.topic}. ${prediction.justification}. How do you solve this question: '${prediction.question}'?",
                        predictionIdRef = prediction.id
                    )
                )
            }
        }
    }

    // --- AI MOCK TEST ACTIONS ---
    fun startGeneratingMockTest(examType: String, numQuestions: Int, timeLimit: Int) {
        _isGeneratingMockTest.value = true
        _activeMockTestId.value = null
        _activeMockTestObj.value = null
        _mockQuestions.value = emptyList()
        _mockAnswersMap.value = emptyMap()

        viewModelScope.launch {
            try {
                val newTestId = repository.generateMockTest(examType, numQuestions, timeLimit)
                val test = repository.getMockTest(newTestId.toInt())
                _activeMockTestObj.value = test
                _activeMockTestId.value = newTestId.toInt()
                
                // Collect generated questions flow
                repository.getQuestionsForTest(newTestId.toInt()).collect { list ->
                    _mockQuestions.value = list
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isGeneratingMockTest.value = false
            }
        }
    }

    fun launchMockTestSession(test: com.example.data.model.MockTest) {
        _activeMockTestObj.value = test
        _activeMockTestId.value = test.id
        _currentMockQuestionIndex.value = 0
        _mockAnswersMap.value = emptyMap()
        _mockTestTimerRemainingSeconds.value = test.timeLimitMinutes * 60

        viewModelScope.launch {
            repository.getQuestionsForTest(test.id).collect { list ->
                // Feed chosen answers mapping from database questions if test is already completed
                _mockQuestions.value = list
                if (test.isCompleted) {
                    val prefilled = mutableMapOf<Int, String>()
                    for (q in list) {
                        q.userAnswer?.let { ans -> prefilled[q.id] = ans }
                    }
                    _mockAnswersMap.value = prefilled
                }
            }
        }

        // Start ticking timer if not already completed
        countdownJob?.cancel()
        if (!test.isCompleted) {
            countdownJob = viewModelScope.launch {
                while (_mockTestTimerRemainingSeconds.value > 0) {
                    kotlinx.coroutines.delay(1000L)
                    _mockTestTimerRemainingSeconds.value -= 1
                }
                submitMockTestResult()
            }
        }
    }

    fun selectMockOption(questionId: Int, option: String) {
        val currentAnswers = _mockAnswersMap.value.toMutableMap()
        currentAnswers[questionId] = option
        _mockAnswersMap.value = currentAnswers
    }

    fun prevMockQuestion() {
        if (_currentMockQuestionIndex.value > 0) {
            _currentMockQuestionIndex.value -= 1
        }
    }

    fun nextMockQuestion() {
        if (_currentMockQuestionIndex.value < _mockQuestions.value.size - 1) {
            _currentMockQuestionIndex.value += 1
        }
    }

    fun submitMockTestResult() {
        countdownJob?.cancel()
        val test = _activeMockTestObj.value ?: return
        val questions = _mockQuestions.value
        val answers = _mockAnswersMap.value

        var correctCount = 0
        val updatedQuestions = questions.map { q ->
            val chosen = answers[q.id]
            val holdsCorrect = chosen == q.correctAnswer
            if (holdsCorrect) correctCount++
            q.copy(userAnswer = chosen)
        }

        val totalQCount = questions.size
        val accuracy = if (totalQCount > 0) (correctCount * 100) / totalQCount else 0
        val xpReward = correctCount * 25 + 15 // 25 XP per correct answer + 15 completion bonus

        val timeSpent = (test.timeLimitMinutes * 60) - _mockTestTimerRemainingSeconds.value

        viewModelScope.launch {
            // Update db record
            repository.saveMockTestSubmission(
                testId = test.id,
                score = correctCount,
                timeSpent = if (timeSpent < 0) 0 else timeSpent,
                accuracy = accuracy,
                updatedQuestions = updatedQuestions
            )
            // Refresh local state objects
            val refreshedTest = repository.getMockTest(test.id)
            _activeMockTestObj.value = refreshedTest
            _mockQuestions.value = updatedQuestions

            // Incentive rewards!
            repository.addXpAndCheckStreak(xpReward)
        }
    }

    fun closeActiveMockTestSession() {
        countdownJob?.cancel()
        _activeMockTestId.value = null
        _activeMockTestObj.value = null
        _mockQuestions.value = emptyList()
        _mockAnswersMap.value = emptyMap()
    }

    fun getVivaQuestions(): List<String> {
        return when (_selectedExam.value) {
            "Class 10 CBSE" -> listOf(
                "State Ohm's law and sketch the expected V-I dynamic linear trace.",
                "Detail the lens formula and sign convention for convex mirrors.",
                "Why are copper and aluminum wires usually employed for electrical transmission lines?"
            )
            "Class 12 Board" -> listOf(
                "Explain the principle of electrostatics flux summation using Gauss's Law.",
                "Compare Aldol condensation and Cannizzaro reaction conditions based on alpha hydrogens.",
                "Why is a lead storage battery highly rechargeable and how does sulfuric acid concentration change?"
            )
            "JEE Main" -> listOf(
                "Under what condition can you apply the parallel axis theorem to obtain moment of inertia?",
                "How do you evaluate integrals easily using the King's reflection property?",
                "Explain the physical difference between rolling without slipping vs pure sliding motion."
            )
            "NEET UG" -> listOf(
                "Calculate total molecules of ATP/NADPH needed to produce 1 mole of glucose in Calvin Cycle.",
                "Trace standard phenotypic ratios of Mendelian dihybrid test cross offspring.",
                "Why can current only flow in one direction inside a p-n semiconductor diode?"
            )
            "NEET PG" -> listOf(
                "How would you differentiate Nephrotic vs Nephritic Syndrome on standard biochemistry and light microscopy?",
                "Outline the priority treatment targets in active Cardiogenic Shock and first-choice pressors.",
                "Detail the key clinical warnings and contraindications of SGLT2 inhibitors in heart failure patients."
            )
            "MBBS University" -> listOf(
                "Discuss the pathophysiology, cellular changes, and key biopsy features of Minimal Change Disease.",
                "What is the mechanism of action of Metformin, and why is it preferred as a first-line agent in Type 2 Diabetes?",
                "Describe the clinical classification of acute appendicitis and standard bedside assessment signs."
            )
            "UPSC" -> listOf(
                "Trace the evolution of Article 21's application from the A.K. Gopalan case to the Puttaswamy privacy judgment.",
                "Discuss the administrative and financial issues surrounding Panchayati Raj institutions under the 73rd Amendment.",
                "Evaluate the feasibility and electrolyzer costs of India's National Green Hydrogen Mission targets."
            )
            "SSC" -> listOf(
                "Identify the differences and approval timelines of National Emergency (352) vs President's Rule (356).",
                "What are the main Himalayan drainage rivers and how do they differ in deposition pattern from Peninsular rivers?",
                "Discuss the key battles and social impacts of the 1857 Indian rebellion."
            )
            else -> listOf(
                "Contrast the Zamindari land settlement system with the Ryotwari and Mahalwari rural taxation structures.",
                "What is the role of CAG (Comptroller and Auditor General) in insuring financial accountability under Article 148?"
            )
        }
    }
}
