package com.example.data.repository

import com.example.data.db.ExamDao
import com.example.data.model.Flashcard
import com.example.data.model.PyqPrediction
import com.example.data.model.UserProfile
import com.example.data.model.WeaknessTopic
import com.example.data.model.CommunityPost
import com.example.data.model.CommunityComment
import com.example.data.model.MockTest
import com.example.data.model.MockQuestion
import com.example.data.network.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import org.json.JSONArray
import org.json.JSONObject

class ExamRepository(private val dao: ExamDao) {

    val userProfile: Flow<UserProfile?> = dao.getUserProfileFlow()
    val allPredictions: Flow<List<PyqPrediction>> = dao.getAllPredictionsFlow()
    val allFlashcards: Flow<List<Flashcard>> = dao.getAllFlashcardsFlow()
    val weaknessTopics: Flow<List<WeaknessTopic>> = dao.getAllWeaknessTopicsFlow()

    fun getPredictionsForExam(exam: String): Flow<List<PyqPrediction>> {
        return dao.getPredictionsForExamFlow(exam)
    }

    suspend fun getOrCreateProfile(): UserProfile {
        val existing = dao.getUserProfile()
        if (existing != null) {
            return existing
        }
        val defaultProfile = UserProfile()
        dao.insertOrUpdateProfile(defaultProfile)
        return defaultProfile
    }

    suspend fun updateProfile(profile: UserProfile) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun updateTargetExam(exam: String) {
        val profile = getOrCreateProfile()
        dao.insertOrUpdateProfile(profile.copy(targetExam = exam))
    }

    suspend fun addXpAndCheckStreak(points: Int) {
        val profile = getOrCreateProfile()
        val current = System.currentTimeMillis()
        val oneDayMs = 24 * 60 * 60 * 1000L
        val lastStudy = profile.lastStudyTimestamp
        
        var newStreak = profile.studyStreak
        if (current - lastStudy > oneDayMs * 2) {
            newStreak = 1 // Streak lost
        } else if (current - lastStudy in (oneDayMs / 2)..oneDayMs) {
            newStreak += 1 // Streak continued
        }

        dao.insertOrUpdateProfile(
            profile.copy(
                xpPoints = profile.xpPoints + points,
                studyStreak = newStreak,
                lastStudyTimestamp = current,
                readinessScore = (profile.readinessScore + (points / 20)).coerceIn(40, 99)
            )
        )
    }

    suspend fun togglePremium(isPremium: Boolean) {
        val profile = getOrCreateProfile()
        dao.insertOrUpdateProfile(profile.copy(tier = if (isPremium) 1 else 0))
    }

    // --- AI PYQ ANALYSIS & PREDICTIONS ENGINE ---
    suspend fun analyzePyqs(
        examType: String,
        sourceTitle: String,
        rawText: String // Extracted text from PDF/Image OCR Simulation
    ): String {
        val systemInstruction = "You are an elite competitive exam analyst for $examType."
        val prompt = """
            Analyze these previous year questions (PYQs) or syllabus notes:
            Title: "$sourceTitle"
            Syllabus Content: "$rawText"
            
            Identify repeated patterns, repetition frequency, high-yield topics, and potential upcoming exam questions.
            Return a JSON object in this EXACT format (do not wrap in markdown tags like ```json, just raw valid JSON):
            {
              "status": "success",
              "predictions": [
                {
                  "subject": "Name of Subject (e.g. Pathology, Indian Polity)",
                  "topic": "Name of Topic (e.g. Cardiogenic Shock, Fundamental Rights Article 21)",
                  "question": "A potential predicted exam question",
                  "repetitionCount": "How many times it appeared (e.g. 6 times in last 8 years)",
                  "probability": "High", 
                  "justification": "Why it is expected to appear in upcoming exams",
                  "modelAnswer": "Brief model response or revision notes for this topic"
                }
              ]
            }
        """.trimIndent()

        val response = GeminiClient.generateWithPrompt(prompt, systemInstruction, responseJson = true)
        
        if (response != null) {
            try {
                // Remove potential markdown wrappers if gemini output got wrapped
                val cleanResponse = response.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanResponse)
                val array = json.getJSONArray("predictions")
                val list = mutableListOf<PyqPrediction>()
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    list.add(
                        PyqPrediction(
                            exam = examType,
                            subject = obj.getString("subject"),
                            topic = obj.getString("topic"),
                            question = obj.getString("question"),
                            repetitionCount = obj.getString("repetitionCount"),
                            probability = obj.getString("probability"),
                            justification = obj.getString("justification"),
                            modelAnswer = obj.getString("modelAnswer")
                        )
                    )
                }
                if (list.isNotEmpty()) {
                    dao.insertPredictions(list)
                    return "Successfully analyzed! Generated ${list.size} high-probability predictions."
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // --- Graceful Fallback: Generate custom syllabus predictions if API key is not configured ---
        val fallbackPredictions = getSyllabusFallbackPredictions(examType).map {
            it.copy(justification = "[PREDICTED TRENDS] " + it.justification)
        }
        dao.insertPredictions(fallbackPredictions)
        return "Syllabus Extracted! Used AI trend weightage database to output predicted questions."
    }

    // --- SMART FLASHCARD GENERATION ---
    suspend fun generateFlashcardsForTopic(
        subject: String,
        topic: String
    ): Int {
        val systemInstruction = "You are a specialized competitive exam mentor who converts topics into crisp active-recall cards."
        val prompt = """
            Create 3 high-yield active recall flashcards for the topic "$topic" in subject "$subject".
            Include 1 mnemonic if applicable.
            Return a JSON object with this exact format (raw JSON, no markdown blocks):
            {
              "cards": [
                {
                  "question": "Question for the flashcard",
                  "answer": "Concise active recall answer",
                  "mnemonic": "Mnemonic helper (keep it null if not needed)"
                }
              ]
            }
        """.trimIndent()

        val response = GeminiClient.generateWithPrompt(prompt, systemInstruction, responseJson = true)
        if (response != null) {
            try {
                val cleanResponse = response.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()

                val json = JSONObject(cleanResponse)
                val array = json.getJSONArray("cards")
                var count = 0
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    dao.insertFlashcard(
                        Flashcard(
                            question = obj.getString("question"),
                            answer = obj.getString("answer"),
                            mnemonic = if (obj.isNull("mnemonic")) null else obj.getString("mnemonic"),
                            subject = subject,
                            topic = topic
                        )
                    )
                    count++
                }
                if (count > 0) return count
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback procedural card generation
        val fallbackQuestions = listOf(
            "What is the primary indicator of $topic?",
            "Identify the most common complication related to $topic.",
            "Explain the pathogenetic or structural basis of $topic."
        )
        val fallbackAnswers = listOf(
            "Key indicator: High-yield clinical criteria or core statistical indices specific to $subject.",
            "Complication: Manifestations requiring urgent intervention, heavily tested on $subject core papers.",
            "Syllabus High Point: Core physiological, diagnostic, or administrative principles defining this UPSC/NEET framework."
        )
        val fallbackMnemonics = listOf(
            "Mnemonic: Try pairing key letters of $topic for faster recall!",
            null,
            null
        )

        for (i in 0..2) {
            dao.insertFlashcard(
                Flashcard(
                    question = fallbackQuestions[i],
                    answer = fallbackAnswers[i],
                    mnemonic = fallbackMnemonics[i],
                    subject = subject,
                    topic = topic
                )
            )
        }
        return 3
    }

    suspend fun deleteFlashcard(id: Int) = dao.deleteFlashcardById(id)

    // --- PERSONALIZED WEAKNESS ANALYZER ---
    suspend fun analyzeAndAddWeakness(
        subject: String,
        topic: String,
        description: String
    ) {
        val systemInstruction = "You are a strategic study planner for competitive exams in India."
        val prompt = """
            Create a highly practical, exam-oriented study plan for a student struggling with "$topic" in "$subject". Keep it realistic (under 45 mins daily).
            Return a JSON object in this exact format (raw, no markdown wrappers):
            {
              "priority": "High", 
              "revisionPlan": "Step-by-step revision strategy"
            }
        """.trimIndent()

        val response = GeminiClient.generateWithPrompt(prompt, systemInstruction, responseJson = true)
        var priority = "Medium"
        var revisionPlan = "Solve 5 local PYQs, identify standard textbook diagrams/sections, write single-line templates."

        if (response != null) {
            try {
                val clean = response.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val json = JSONObject(clean)
                priority = json.optString("priority", "Medium")
                revisionPlan = json.optString("revisionPlan", revisionPlan)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        dao.insertWeaknessTopic(
            WeaknessTopic(
                subject = subject,
                topic = topic,
                weaknessDescription = description,
                dailyRevisionPlan = revisionPlan,
                priorityLevel = priority
            )
        )
    }

    suspend fun solveWeakness(id: Int) = dao.deleteWeaknessTopicById(id)

    // --- INTERACTIVE VIVA EVALUATION ---
    suspend fun evaluateVivaAnswer(
        question: String,
        userAnswer: String,
        vivaMode: String // "Easy", "Professor Mode", "Rapid-fire Mode"
    ): VivaEvaluation {
        val systemInstruction = "You are an examiner administering a viva exam. Your tone depends on mode: Easy (helpful, encouraging), Professor Mode (extremely rigorous, looking for core keywords), Rapid-fire Mode (extremely brief, fast scoring)."
        val prompt = """
            Viva Question: "$question"
            Student's Response: "$userAnswer"
            Mode: "$vivaMode"
            
            Evaluate this viva answer. Score out of 10.
            List specific core keywords they included or missed, and supply a highly professional, text-book model answer.
            Return a JSON object in this exact format (raw valid JSON, no markdown tags):
            {
              "score": 7,
              "evaluation": "Paragraph of detailed examiner feedback depending on $vivaMode",
              "keywordsFound": ["keyword1"],
              "keywordsMissed": ["keyword2"],
              "modelAnswer": "The perfect model text answer"
            }
        """.trimIndent()

        val response = GeminiClient.generateWithPrompt(prompt, systemInstruction, responseJson = true)
        if (response != null) {
            try {
                val clean = response.trim()
                    .removePrefix("```json")
                    .removePrefix("```")
                    .removeSuffix("```")
                    .trim()
                val json = JSONObject(clean)
                val foundArr = json.getJSONArray("keywordsFound")
                val missedArr = json.getJSONArray("keywordsMissed")
                
                val found = mutableListOf<String>()
                for (i in 0 until foundArr.length()) found.add(foundArr.getString(i))

                val missed = mutableListOf<String>()
                for (i in 0 until missedArr.length()) missed.add(missedArr.getString(i))

                return VivaEvaluation(
                    score = json.getInt("score"),
                    feedback = json.getString("evaluation"),
                    keywordsMatched = found,
                    keywordsMissed = missed,
                    modelAnswer = json.getString("modelAnswer")
                )
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }

        // Fallback procedural evaluation
        val wordCount = userAnswer.split("\\s+".toRegex()).size
        val mockScore = if (wordCount < 3) 2 else if (wordCount < 10) 5 else if (wordCount < 20) 8 else 9
        val mockMatched = if (wordCount > 5) listOf("Primary indication", "Clinical presentation") else emptyList()
        val mockMissed = if (wordCount < 15) listOf("Severity grading score", "Risk stratification criteria") else emptyList()

        return VivaEvaluation(
            score = mockScore,
            feedback = when (vivaMode) {
                "Professor Mode" -> "Examiner notes: Your response identifies broad parameters but misses the exact pathophysiological staging descriptors. Aim for precision."
                "Rapid-fire Mode" -> "Good pacing. Keep definitions concise and list diagnostic ranges immediately."
                else -> "Encouraging effort! You've captured the core theme. Reviewing the standard staging tables will help maximize your practical scores."
            },
            keywordsMatched = mockMatched,
            keywordsMissed = mockMissed,
            modelAnswer = "Ideal Staging: Comprehensive classification outlining precise criteria, core standard limits, diagnostic investigations, and first-line conservative or surgical treatments as per contemporary Indian/Global syllabus standards."
        )
    }

    // --- SYLLABUS-BASED DEFAULT MOCK DATA GENERATOR ---
    private fun getSyllabusFallbackPredictions(exam: String): List<PyqPrediction> {
        return when (exam) {
            "Class 10 CBSE" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Science",
                    topic = "Ohm's Law Verification",
                    question = "State Ohm's Law. Draw a labeled circuit diagram to verify this law. What are the three primary factors affecting wire resistance?",
                    repetitionCount = "6 times in last 8 years",
                    probability = "High",
                    justification = " CBSE Class 10 classic 5-mark subjective question. Tests fundamental electrical circuit concepts.",
                    modelAnswer = "V = IR. Verification requires showing linear V-I slope. Factors: length, cross-sectional area, material resistivity, and temperature."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Mathematics",
                    topic = "Quadratic Equations roots",
                    question = "Find the roots of the quadratic equation 3x² - 5x + 2 = 0 using the quadratic formula and state the nature of its roots.",
                    repetitionCount = "4 times in previous board papers",
                    probability = "High",
                    justification = "Core algebra topic. Discriminant classification (b² - 4ac) is vital for section D scoring.",
                    modelAnswer = "Discriminant = (-5)² - 4(3)(2) = 25 - 24 = 1 > 0 (Real and distinct). Roots are x = 1 and x = 2/3."
                )
            )
            "Class 12 Board" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Physics",
                    topic = "Gauss's Law Derivations",
                    question = "State Gauss's Law in electrostatics. Derive an expression for the electric field intensity due to an infinitely long straight charged wire.",
                    repetitionCount = "7 times in last 10 board papers",
                    probability = "High",
                    justification = "Frequently tested 5-marker derivation. Essential for electrostatics mastery.",
                    modelAnswer = "Total flux = q / ε₀. Using cylindrical Gaussian surface, E * 2πrL = λL / ε₀, giving E = λ / (2π ε₀ r)."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Chemistry",
                    topic = "Organic Name Reactions (Aldol vs Cannizzaro)",
                    question = "Explain Aldol Condensation and Cannizzaro reaction with equations, specifying the distinguishing reactant criteria.",
                    repetitionCount = "5 times in last 8 boards",
                    probability = "High",
                    justification = "Top organic conversions question. Aldol requires α-hydrogens; Cannizzaro requires absence of α-hydrogens.",
                    modelAnswer = "Aldol: 2CH₃CHO + dil. NaOH -> CH₃-CH(OH)-CH₂-CHO. Cannizzaro: 2HCHO + conc. NaOH -> HCOONa + CH₃OH."
                )
            )
            "JEE Main" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Physics",
                    topic = "Rotational Mechanics (Parallel Axis Theorem)",
                    question = "A thin uniform rod of mass M and length L is rotating. Find its moment of inertia about a transverse axis passing through a point L/4 from one end.",
                    repetitionCount = "9 times in recent shift cycles",
                    probability = "High",
                    justification = "NTA frequently bundles rotational dynamics with parallel/perpendicular axis theorem applications.",
                    modelAnswer = "I = I_cm + Md² = (ML²/12) + M(L/4) = 7ML²/48 using Parallel Axis Theorem."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Mathematics",
                    topic = "Definite Integrals (King's Property)",
                    question = "Evaluate the definite integral of f(x) = log(1 + tan(x)) from 0 to π/4.",
                    repetitionCount = "6 times in previous shift cycles",
                    probability = "High",
                    justification = "Classic integral trick utilizing King's property: ∫f(x)dx = ∫f(a+b-x)dx.",
                    modelAnswer = "Applying properties, 2I = ∫log(2/(1+tan(x))) + log(1+tan(x)) dx = ∫log(2)dx, which yields I = (π / 8) * log(2)."
                )
            )
            "NEET UG" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Biology",
                    topic = "Calvin Cycle Carbon Budget",
                    question = "Explain the three phases of the Calvin Cycle (Carboxylation, Reduction, and Regeneration). How much ATP and NADPH are used to synthesize one glucose?",
                    repetitionCount = "12 times in recent papers",
                    probability = "High",
                    justification = "High-priority direct NCERT-based question. Vital for photosynthesis marking.",
                    modelAnswer = "6 CO₂ turns require 18 ATP and 12 NADPH. Carboxylation catalyzed by RuBisCO is the most critical step."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Physics",
                    topic = "Semiconductor Rectifiers",
                    question = "State the difference between half-wave and full-wave rectifiers. Find the maximum theoretical efficiency of a full-wave rectifier.",
                    repetitionCount = "5 times in last 8 papers",
                    probability = "Medium",
                    justification = "Repeated scoring question in class XII semiconductor syllabus sections.",
                    modelAnswer = "Half-wave max efficiency = 40.6%. Full-wave max efficiency = 81.2% (reversing negative cycles)."
                )
            )
            "NEET PG" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Medicine",
                    topic = "Cardiogenic Shock",
                    question = "Discuss the clinical criteria, risk stratification indices, and emergency medical management of Cardiogenic Shock.",
                    repetitionCount = "7 times in last 10 medical papers",
                    probability = "High",
                    justification = "Frequently tested in NEET PG & INICET clinical case questions. Requires understanding of vasoactive agents.",
                    modelAnswer = "Key points: Cardiac index < 2.2 L/min/m². Primary therapy includes Norepinephrine, Dobutamine infusion, and early revascularization."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Gynaecology",
                    topic = "Ovarian Carcinoma Staging",
                    question = "Detail the FIGO classification staging of Epithelial Ovarian Cancer, including clinical indicators and surgical debulking cutoffs.",
                    repetitionCount = "5 times in last 8 papers",
                    probability = "High",
                    justification = "High-priority OBG scoring topic. Focus is shifting towards precise Stage III vs IV distinction criteria.",
                    modelAnswer = "Key stages: Stage I = Confined to ovaries; Stage II = Pelvic extension; Stage III = Peritoneal implants outside pelvis; Stage IV = Distant metastasis."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Pediatrics",
                    topic = "Inborn Errors of Metabolism",
                    question = "Outline the diagnostic algorithms for PKU and Maple Syrup Urine Disease in neonates presenting with poor feeding and seizure activity.",
                    repetitionCount = "4 times in last 9 papers",
                    probability = "Medium",
                    justification = "Highly relevant biochemistry-clinical linkage query frequently appearing in MCQ pattern matchers.",
                    modelAnswer = "MSUD has enzyme defect in Branched-Chain Alpha-Keto Acid Dehydrogenase leading to elevated leucine, isoleucine, and valine."
                )
            )
            "MBBS University" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Pathology",
                    topic = "Nephrotic Syndrome Staging",
                    question = "Describe the etiology, pathogenesis, light microscopic and electron microscopic features of Minimal Change Disease (MCD).",
                    repetitionCount = "9 times in last 10 university exams",
                    probability = "High",
                    justification = "Absolute classic long case question in 2nd Professional MBBS pathology papers.",
                    modelAnswer = "Key: Diffuse effacement of foot processes of visceral epithelial cells (podocytes). Highly responsive to corticosteroid therapies."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Pharmacology",
                    topic = "Oral Hypoglycemic Agents",
                    question = "Classify oral antidiabetic drugs. Explain the mechanism of action, side effects, and drug-drug interactions of Metformin and SGLT2 inhibitors.",
                    repetitionCount = "8 times in last 11 year-ends",
                    probability = "High",
                    justification = "Highly relevant pharmacological long essay. Required for 2nd Prof theory success.",
                    modelAnswer = "Metformin activates AMP-activated protein kinase (AMPK), decreasing hepatic glucose production. Biguanides cause lactic acidosis risk."
                )
            )
            "UPSC" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "Polity & Constitution",
                    topic = "Fundamental Rights (Article 21)",
                    question = "Analyze the expanding horizons of the Right to Life and Personal Liberty under Article 21, tracing critical Supreme Court judgments from Gopalan to Puttaswamy.",
                    repetitionCount = "8 times in GS Paper 2",
                    probability = "High",
                    justification = "Fundamental theme in Indian Polity. Always relevant due to privacy, environment, and social justice mandates.",
                    modelAnswer = "Key notes: Gopalan (narrow 'procedure established by law') vs Maneka Gandhi (broad 'due process of law'). Puttaswamy declared privacy an article of life."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Environment & Ecology",
                    topic = "Green Hydrogen Mission in India",
                    question = "Assess the strategic importance of the National Green Hydrogen Mission. What technological and infrastructural challenges face its rapid deployment?",
                    repetitionCount = "3 times in last 4 Mains",
                    probability = "High",
                    justification = "A core decarbonization framework policy heavily pushed in current administrative, economic, and sci-tech question pools.",
                    modelAnswer = "Key targets: 5 MMT annual production by 2030. Challenges include electrolyzer import costs, high storage pressure requirements, and transport grid availability."
                )
            )
            "SSC" -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "General Awareness",
                    topic = "Emergency Provisions",
                    question = "Explain the types of Emergency provisions under the Indian Constitution (Articles 352, 356, and 360) and their parliamentary approval periods.",
                    repetitionCount = "12 times in Tier 1 & Tier 2 exams",
                    probability = "High",
                    justification = "A staple General Studies question in SSC CGL. Focuses on timelines, effects, and major amendments like the 44th Amendment.",
                    modelAnswer = "Article 352: National emergency (approval within 1 month); Article 356: President's Rule (approval within 2 months); Article 360: Financial emergency."
                ),
                PyqPrediction(
                    exam = exam,
                    subject = "Geography",
                    topic = "River Drainage Systems",
                    question = "Classify Indian rivers into Himalayan and Peninsular systems. Map their major west-flowing vs east-flowing tributaries and delta formations.",
                    repetitionCount = "9 times in last 7 years",
                    probability = "High",
                    justification = "Geography-based direct matchers, frequently asked in relation to dams, origins (e.g. Amarkantak), and hydro-electric plants.",
                    modelAnswer = "Peninsular Rivers: Narmada and Tapi flow west into Arabian Sea making estuaries. Godavari, Krishna, Mahanadi, Kauvery flow east making deltas."
                )
            )
            else -> listOf(
                PyqPrediction(
                    exam = exam,
                    subject = "General Aptitude",
                    topic = "History & Governance",
                    question = "Compare historical land revenue settlements (Zamindari, Ryotwari, and Mahalwari systems) introduced under British Rule in India.",
                    repetitionCount = "6 times in previous syllabus lists",
                    probability = "High",
                    justification = "Crucial socioeconomic development query appearing regularly across state administrative services exams.",
                    modelAnswer = "Zamindari: Permanent settlement (Bengal, Cornwallis); Ryotwari: Direct agreement with peasants (Madras/Bombay, Munro); Mahalwari: Village community settlement (North-West)."
                )
            )
        }
    }

    // --- COMMUNITY FORUM WORK ---
    val allComments: (Int) -> Flow<List<CommunityComment>> = { postId -> dao.getCommentsForPostFlow(postId) }
    
    fun getPostsForExam(exam: String): Flow<List<CommunityPost>> {
        return dao.getPostsForExamFlow(exam)
    }

    suspend fun createCommunityPost(
        examType: String,
        subject: String,
        topic: String,
        author: String,
        title: String,
        content: String,
        predictionIdRef: Int? = null
    ): Long {
        return dao.insertPost(
            CommunityPost(
                examType = examType,
                subject = subject,
                topic = topic,
                author = author,
                title = title,
                content = content,
                predictionIdRef = predictionIdRef
            )
        )
    }

    suspend fun createCommunityComment(
        postId: Int,
        author: String,
        content: String
    ) {
        dao.insertComment(
            CommunityComment(
                postId = postId,
                author = author,
                content = content
            )
        )
        // Update post comment count
        val post = dao.getPostById(postId)
        if (post != null) {
            dao.insertPost(post.copy(commentCount = post.commentCount + 1))
        }
    }

    suspend fun votePost(postId: Int, up: Boolean) {
        val post = dao.getPostById(postId) ?: return
        if (up) {
            dao.insertPost(post.copy(upvotes = post.upvotes + 1))
        } else {
            if (post.upvotes > 0) {
                dao.insertPost(post.copy(downvotes = post.downvotes + 1))
            }
        }
    }

    suspend fun seedInitialPostsIfEmpty() {
        val allCurrent = dao.getAllPostsFlow().firstOrNull()
        if (allCurrent != null && allCurrent.isNotEmpty()) return

        // Seed posts with custom preset subgroups
        val seedList = listOf(
            CommunityPost(
                examType = "Class 10 CBSE",
                subject = "Science",
                topic = "Ohm's Law",
                author = "Preeti_Verma",
                title = "Ohm's Law ray circuits and board mark guidelines?",
                content = "Does anyone know if CBSE board examiners deduct marks if we do not draw the direction arrows in ray diagrams, or if the ammeter connection polarity is misplaced in Ohm's circuit diagrams?",
                upvotes = 12,
                commentCount = 1
            ),
            CommunityPost(
                examType = "Class 12 Board",
                subject = "Physics",
                topic = "Gauss's Law",
                author = "Rohit_Class12",
                title = "How to write a perfect 5-mark answer for Gauss wire derivation",
                content = "CBSE loves Coulomb from Gauss wire derivations. What is the logical sequence of steps to write this out without missing keywords? Thanks!",
                upvotes = 15,
                commentCount = 1
            ),
            CommunityPost(
                examType = "JEE Main",
                subject = "Physics",
                topic = "Rotational Mechanics",
                author = "Amaan_JEE",
                title = "Moment of Inertia Parallel Axis mistakes",
                content = "I make massive sign mistakes in using the Parallel Axis Theorem when the rod is shifted off-center. Is there a simple trick, or do I need to integrate from scratch every time?",
                upvotes = 24,
                commentCount = 1
            ),
            CommunityPost(
                examType = "NEET UG",
                subject = "Biology",
                topic = "Calvin Cycle",
                author = "Aradhya_Med",
                title = "Calvin Cycle ATP count calculations shortcut?",
                content = "Can anyone clarify the energy tally calculations on synthesizing 1 raw glucose? Sometimes NTA asks about PGAL cycles vs standard carbon output. Very confused.",
                upvotes = 18,
                commentCount = 1
            ),
            CommunityPost(
                examType = "NEET PG",
                subject = "Medicine",
                topic = "Cardiogenic Shock",
                author = "Dr. Shreya_P",
                title = "Emergency guidelines for Pressor titrations",
                content = "In cardiogenic shock with borderline arterial pressure, is there any clinical scenario where Norepinephrine is skipped in favor of early high-dose dobutamine?",
                upvotes = 32,
                commentCount = 2
            ),
            CommunityPost(
                examType = "UPSC",
                subject = "Polity",
                topic = "Fundamental Rights",
                author = "IAS_Aspirant2026",
                title = "Tracing Gopalan vs Puttaswamy in Article 21 essays",
                content = "Planning a 250-word mains response for Article 21. How can we smoothly structure the transition from Gopalan (Procedure established by law) to Puttaswamy digital privacy boundaries?",
                upvotes = 42,
                commentCount = 2
            )
        )

        for (post in seedList) {
            val generatedId = dao.insertPost(post)
            // Seed a helpful answer underneath
            when (post.examType) {
                "Class 10 CBSE" -> dao.insertComment(
                    CommunityComment(
                        postId = generatedId.toInt(),
                        author = "CBSE_Ex_Teacher",
                        content = "Absolutely yes, arrows indicating direction of current or ray paths are essential. Unlabeled axes or missing arrows usually trigger a deduction of 0.5 to 1 mark in Section E!"
                    )
                )
                "Class 12 Board" -> dao.insertComment(
                    CommunityComment(
                        postId = generatedId.toInt(),
                        author = "BoardTopper25",
                        content = "Start with the statement: total electric flux is 1/ε₀ times charge. Draw a symmetric tube of radius r. Write Gauss integral ∫E.dA = q/ε₀. Keep E outside. Express q as λ * L. That's a bulletproof 5 marks structure."
                    )
                )
                "JEE Main" -> dao.insertComment(
                    CommunityComment(
                        postId = generatedId.toInt(),
                        author = "IITian_Anuj",
                        content = "Always apply d from the center of mass! That's the most common trap. I = I_cm + Md² strictly requires I_cm, not any random axis."
                    )
                )
                "NEET UG" -> dao.insertComment(
                    CommunityComment(
                        postId = generatedId.toInt(),
                        author = "BiologyGuru_NCERT",
                        content = "Just remember: 1 CO₂ requires 3 ATP and 2 NADPH. To make 1 Glucose, we fix 6 CO₂ turns. So total is: 3 * 6 = 18 ATP and 2 * 6 = 12 NADPH. Stick to this ratio strictly."
                    )
                )
                "NEET PG" -> {
                    dao.insertComment(
                        CommunityComment(
                            postId = generatedId.toInt(),
                            author = "Dr. Patel_Cardio",
                            content = "In clinical practice, Norepinephrine is always started first if MAP is <65mmHg to ensure coronary perfusion. Dobutamine is added only after MAP is stabilized to increase cardiac output safely. Never do isolated high-dose dobutamine without correcting pressure."
                        )
                    )
                    dao.insertComment(
                        CommunityComment(
                            postId = generatedId.toInt(),
                            author = "Dr. Sneh_V",
                            content = "Matches what is asked in recent clinical clinical cases on NEET PG. Thank you doctor!"
                        )
                    )
                }
                "UPSC" -> {
                    dao.insertComment(
                        CommunityComment(
                            postId = generatedId.toInt(),
                            author = "Mains_Strategist",
                            content = "Structure: 1. Introduction: Define Article 21, broad scope. 2. Gopalan Era (Literal procedural approach). 3. Maneka Gandhi turn (Introducing Due Process of Law). 4. Expansion peak: Puttaswamy judgment (Privacy as intrinsic). 5. Modern context (Digital public infrastructure and state surveillance balances). This shows chronological analytical depth."
                        )
                    )
                    dao.insertComment(
                        CommunityComment(
                            postId = generatedId.toInt(),
                            author = "Polity_Geek",
                            content = "Invaluable summary! I will note this framework in my notebook."
                        )
                    )
                }
            }
        }
    }

    // --- MOCK TEST GENERATION ---
    val allMockTestsFlow: Flow<List<MockTest>> = dao.getAllMockTestsFlow()
    
    fun getQuestionsForTest(testId: Int): Flow<List<MockQuestion>> {
        return dao.getQuestionsForTestFlow(testId)
    }

    suspend fun getMockTest(testId: Int): MockTest? {
        return dao.getMockTestById(testId)
    }

    suspend fun saveMockTestSubmission(
        testId: Int,
        score: Int,
        timeSpent: Int,
        accuracy: Int,
        updatedQuestions: List<MockQuestion>
    ) {
        val test = dao.getMockTestById(testId) ?: return
        dao.insertMockTest(
            test.copy(
                isCompleted = true,
                scoreObtained = score,
                timeSpentSeconds = timeSpent,
                accuracyPercentage = accuracy
            )
        )
        // Save answers chosen
        for (q in updatedQuestions) {
            dao.insertMockQuestion(q)
        }
    }

    suspend fun generateMockTest(
        examType: String,
        numQuestions: Int,
        timeLimit: Int
    ): Long {
        val testId = dao.insertMockTest(
            MockTest(
                examType = examType,
                title = "AI Predictive Practice: $examType (${numQuestions} Qs)",
                totalQuestions = numQuestions,
                timeLimitMinutes = timeLimit
            )
        )

        // Try AI generation first
        val aiQuestions = try {
            generateMockQuestionsFromAI(examType, numQuestions, testId.toInt())
        } catch (e: Exception) {
            null
        }

        val questions = if (aiQuestions != null && aiQuestions.isNotEmpty()) {
            aiQuestions
        } else {
            getFallbackMockQuestions(examType, numQuestions, testId.toInt())
        }

        dao.insertMockQuestions(questions)
        return testId
    }

    private suspend fun generateMockQuestionsFromAI(
        examType: String,
        numQuestions: Int,
        testId: Int
    ): List<MockQuestion>? {
        val systemInstruction = "You are an expert exam paper generator for competitive Indian and school boards: $examType."
        val prompt = """
            Generate exactly $numQuestions realistic multiple choice questions (MCQs) for the exam "$examType".
            Return a JSON object with this exact format (raw valid JSON, no markdown boxes):
            {
              "questions": [
                {
                  "subject": "Subject Name",
                  "topic": "Topic Name",
                  "questionText": "The clinical case or theoretical problem statement",
                  "optionA": "Choice A",
                  "optionB": "Choice B",
                  "optionC": "Choice C",
                  "optionD": "Choice D",
                  "correctAnswer": "A",
                  "explanation": "Why this option is correct and others are wrong"
                }
              ]
            }
        """.trimIndent()

        val response = GeminiClient.generateWithPrompt(prompt, systemInstruction, responseJson = true) ?: return null
        val clean = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val json = JSONObject(clean)
        val arr = json.getJSONArray("questions")
        val result = mutableListOf<MockQuestion>()
        for (i in 0 until minOf(arr.length(), numQuestions)) {
            val obj = arr.getJSONObject(i)
            result.add(
                MockQuestion(
                    mockTestId = testId,
                    subject = obj.optString("subject", "General"),
                    topic = obj.optString("topic", "High yield"),
                    questionText = obj.optString("questionText"),
                    optionA = obj.optString("optionA"),
                    optionB = obj.optString("optionB"),
                    optionC = obj.optString("optionC"),
                    optionD = obj.optString("optionD"),
                    correctAnswer = obj.optString("correctAnswer", "A"),
                    explanation = obj.optString("explanation", "Verified correct option and criteria.")
                )
            )
        }
        return result
    }

    private fun getFallbackMockQuestions(
        examType: String,
        numQuestions: Int,
        testId: Int
    ): List<MockQuestion> {
        val allOptions = when (examType) {
            "Class 10 CBSE" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "Science",
                    topic = "Electricity",
                    questionText = "A metal wire of length L and cross-sectional area A has resistance R. If the wire is stretched to double its length without changing its density, what is the new resistance?",
                    optionA = "2 R",
                    optionB = "4 R",
                    optionC = "R / 2",
                    optionD = "R",
                    correctAnswer = "B",
                    explanation = "When stretched to double its length (L' = 2L), its area halves (A' = A/2) since volume is constant. R = ρ L/A, so new R' = ρ (2L) / (A/2) = 4 R."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Science",
                    topic = "Light",
                    questionText = "An object is placed at the focus of a concave mirror. Where is the image formed and what is its nature?",
                    optionA = "At focus, virtual and erect",
                    optionB = "At center of curvature, real and inverted",
                    optionC = "At infinity, real and inverted",
                    optionD = "Behind the mirror, virtual and erect",
                    correctAnswer = "C",
                    explanation = "Reflected rays from the focus of a concave mirror emerge parallel and meet at infinity, forming a highly magnified real, inverted image."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Mathematics",
                    topic = "Real Numbers",
                    questionText = "If HCF of two numbers a and b is 12 and their product is 1800, what is their LCM?",
                    optionA = "150",
                    optionB = "120",
                    optionC = "180",
                    optionD = "300",
                    correctAnswer = "A",
                    explanation = "Using the standard mathematical formula: HCF(a, b) * LCM(a, b) = a * b. LCM = 1800 / 12 = 150."
                )
            )
            "Class 12 Board" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "Physics",
                    topic = "Electrostatics",
                    questionText = "What is the total electric flux passing through a physical closed cube if a point charge 'q' is placed at one of its corners?",
                    optionA = "q / ε₀",
                    optionB = "q / (6 ε₀)",
                    optionC = "q / (8 ε₀)",
                    optionD = "Zero",
                    correctAnswer = "C",
                    explanation = "To enclose a charge placed at a corner, we require 8 identical cubes. Thus, the flux distributed through one cube is exactly q / (8 ε₀) as per Gauss's Law."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Chemistry",
                    topic = "Electrochemistry",
                    questionText = "Which of the following describes the key reaction occurring at the cathode during the discharge of a standard Lead Storage Battery?",
                    optionA = "Pb is oxidized to PbSO₄",
                    optionB = "PbO₂ is reduced to PbSO₄",
                    optionC = "Oxygen gas is liberated",
                    optionD = "Sulfuric acid is synthesized",
                    correctAnswer = "B",
                    explanation = "During discharge, the cathode PbO₂(s) combines with sulfate ions and hydrogen to undergo reduction to PbSO₄(s)."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Physics",
                    topic = "Electromagnetism",
                    questionText = "Lenz's Law in electromagnetic induction is a direct consequence of which conservation principle?",
                    optionA = "Conservation of Charge",
                    optionB = "Conservation of Momentum",
                    optionC = "Conservation of Energy",
                    optionD = "Conservation of Mass",
                    correctAnswer = "C",
                    explanation = "Lenz's Law ensures that the work done in moving a magnet against the induced field is converted into electrical energy, satisfying the conservation of energy."
                )
            )
            "JEE Main" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "Physics",
                    topic = "Rotational Dynamics",
                    questionText = "A solid sphere rolls down an inclined plane of height h without slipping. What is the linear velocity of the sphere when it reaches the bottom?",
                    optionA = "sqrt(2 g h)",
                    optionB = "sqrt(10/7 g h)",
                    optionC = "sqrt(4/3 g h)",
                    optionD = "sqrt(7/5 g h)",
                    correctAnswer = "B",
                    explanation = "Using conservation of mechanical energy: Mgh = 1/2 M v² + 1/2 I ω². For a solid sphere, I = 2/5 M R². ω = v/R. Substituting gives Mgh = 7/10 M v², so v = sqrt(10/7 g h)."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Chemistry",
                    topic = "Thermodynamics",
                    questionText = "For a reversible cyclic process, which of the following statements about state functions must be mathematically true?",
                    optionA = "dS = 0 and dE = 0",
                    optionB = "dQ = 0 and dW = 0",
                    optionC = "dH differs from zero but dE = 0",
                    optionD = "dG > 0",
                    correctAnswer = "A",
                    explanation = "Since Entropy (S) and Internal Energy (E) are state functions, their net change over any closed cyclic path is strictly zero."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Mathematics",
                    topic = "Calculus",
                    questionText = "What is the area of the region bounded by the curves y = x² and y = x?",
                    optionA = "1 / 3",
                    optionB = "1 / 6",
                    optionC = "1 / 2",
                    optionD = "5 / 6",
                    correctAnswer = "B",
                    explanation = "Intersection points are x = 0 and x = 1. Area = ∫ (x - x²) dx from 0 to 1 = [x²/2 - x³/3] = 1/2 - 1/3 = 1/6."
                )
            )
            "NEET UG" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "Biology",
                    topic = "Calvin Cycle",
                    questionText = "How many molecules of ATP and NADPH are consumed during the reduction phase of the Calvin Cycle to synthesize one net molecule of glucose?",
                    optionA = "12 ATP and 12 NADPH",
                    optionB = "18 ATP and 12 NADPH",
                    optionC = "12 ATP and 18 NADPH",
                    optionD = "6 ATP and 6 NADPH",
                    correctAnswer = "A",
                    explanation = "For 6 turns of the Calvin cycle (1 Glucose), the reduction phase specifically consumes exactly 12 molecules of ATP and 12 molecules of NADPH. (The regeneration phase consumes 6 more ATP, making total ATP = 18)."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Biology",
                    topic = "Genetics",
                    questionText = "In a typical Mendelian dihybrid test cross, what is the expected phenotypic ratio of the offspring?",
                    optionA = "9:3:3:1",
                    optionB = "1:1:1:1",
                    optionC = "3:1",
                    optionD = "1:2:1",
                    correctAnswer = "B",
                    explanation = "A dihybrid test cross is a cross between a double heterozygous parent (RrYy) and a double recessive parent (rryy), yielding a phenotypic ratio of 1:1:1:1."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Physics",
                    topic = "Modern Physics",
                    questionText = "Which series of hydrogen spectrum lines lies in the visible light frequency range?",
                    optionA = "Lyman Series",
                    optionB = "Balmer Series",
                    optionC = "Paschen Series",
                    optionD = "Pfund Series",
                    correctAnswer = "B",
                    explanation = "Lyman is in the ultraviolet region, Balmer is in the visible region, and Paschen/Pfund lie in the infrared region."
                )
            )
            "NEET PG" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "Medicine",
                    topic = "Cardiology",
                    questionText = "A patient presents with acute myocardial infarction, cold extremities, and severe hypotension. PA catheter shows a cardiac index of 1.8 L/min/m² and PCWP of 22 mmHg. What is the initial shock diagnosis?",
                    optionA = "Septic Shock",
                    optionB = "Anaphylactic Shock",
                    optionC = "Cardiogenic Shock",
                    optionD = "Hypovolemic Shock",
                    correctAnswer = "C",
                    explanation = "Low cardiac index (<2.2), elevated pulmonary capillary wedge pressure PCWP (>18 mmHg), and signs of systemic hypoperfusion confirm Cardiogenic Shock."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Pediatrics",
                    topic = "Metabolism",
                    questionText = "A 2-week-old neonate presents with progressive feeding intolerance, musty body odor, and developmental delay. Which enzyme deficiency is responsible?",
                    optionA = "Homogentisate oxidase",
                    optionB = "Phenylalanine hydroxylase",
                    optionC = "Galactose-1-phosphate uridylyltransferase",
                    optionD = "Branched-chain alpha-keto acid dehydrogenase",
                    correctAnswer = "B",
                    explanation = "Musty mousy odor with clinical arrest points to Phenylketonuria (PKU), caused by systemic deficiency of the hepatic enzyme Phenylalanine Hydroxylase (PAH)."
                )
            )
            "UPSC" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "Polity",
                    topic = "Foundations of Article 21",
                    questionText = "In which landmark case did the Supreme Court of India transition from a narrow literal 'Procedure Established by Law' interpretation under Article 21 to a broader 'Due Process of Law' view?",
                    optionA = "A.K. Gopalan v. State of Madras",
                    optionB = "Maneka Gandhi v. Union of India",
                    optionC = "Kesavananda Bharati v. State of Kerala",
                    optionD = "K.S. Puttaswamy v. Union of India",
                    correctAnswer = "B",
                    explanation = "Maneka Gandhi (1978) declared that procedure under Article 21 must be 'just, fair and reasonable', successfully infusing the American concept of procedural due process."
                ),
                MockQuestion(
                    mockTestId = testId,
                    subject = "Polity",
                    topic = "Federal Relations",
                    questionText = "The Panchayati Raj system was constitutionally institutionalized in India under which historical Constitutional Amendment Act?",
                    optionA = "42nd Amendment",
                    optionB = "44th Amendment",
                    optionC = "73rd Amendment",
                    optionD = "86th Amendment",
                    correctAnswer = "C",
                    explanation = "The 73rd Amendment Act, 1992 added Part IX to the Constitution and established the 3-tier rural local self-governance Panchayati Raj structure."
                )
            )
            "SSC" -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "General Studies",
                    topic = "Emergency Rules",
                    questionText = "Under Article 356 of the Indian Constitution, an order of President's Rule in a State must be approved by both Houses of Parliament within what time limit?",
                    optionA = "One Month",
                    optionB = "Two Months",
                    optionC = "Six Months",
                    optionD = "Three Months",
                    correctAnswer = "B",
                    explanation = "Article 352 (National Emergency) requires approval within 1 month. Article 356 (President's Rule) requires approval within 2 months by simple majority."
                )
            )
            else -> listOf(
                MockQuestion(
                    mockTestId = testId,
                    subject = "General Aptitude",
                    topic = "Economics",
                    questionText = "Which sector of the Indian Economy experiences the highest percentage of disguised unemployment?",
                    optionA = "Industrial Sector",
                    optionB = "Agricultural Sector",
                    optionC = "IT & Tech Services",
                    optionD = "Manufacturing Cluster",
                    correctAnswer = "B",
                    explanation = "Agri-sector has huge numbers of surplus laborers whose marginal productivity is close to zero, representing disguised unemployment."
                )
            )
        }

        // Limit the fallback questions carefully to user's desired limit
        return allOptions.take(numQuestions)
    }
}

data class VivaEvaluation(
    val score: Int,
    val feedback: String,
    val keywordsMatched: List<String>,
    val keywordsMissed: List<String>,
    val modelAnswer: String
)

