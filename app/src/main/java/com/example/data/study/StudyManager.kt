package com.example.data.study

import com.example.data.model.PracticeQuestion

object StudyManager {
    val subjects = listOf(
        "Mathematics",
        "Physics",
        "Chemistry",
        "Biology",
        "Computer Science",
        "World History"
    )

    // Curated high quality practice questions for instant offline study practice
    val sampleQuestions = listOf(
        PracticeQuestion(
            subject = "Mathematics",
            question = "If 3x + 5 = 20, what is the value of x?",
            options = listOf("3", "5", "7", "15"),
            correctIndex = 1,
            explanationEn = "Subtract 5 from both sides: 3x = 15. Then divide by 3: x = 5.",
            explanationTe = "రెండు వైపుల నుండి 5 తీసివేయండి: 3x = 15. తర్వాత 3 తో భాగించండి: x = 5."
        ),
        PracticeQuestion(
            subject = "Physics",
            question = "What is Newton's Second Law of Motion represented as?",
            options = listOf("F = ma", "E = mc²", "v = u + at", "P = IV"),
            correctIndex = 0,
            explanationEn = "Force equals mass multiplied by acceleration (F = ma).",
            explanationTe = "బలం = ద్రవ్యరాశి × త్వరణం (F = ma). న్యూటన్ రెండవ నియమం."
        ),
        PracticeQuestion(
            subject = "Chemistry",
            question = "What is the chemical symbol for Gold?",
            options = listOf("Ag", "Au", "Fe", "Cu"),
            correctIndex = 1,
            explanationEn = "Au originates from the Latin word 'Aurum', meaning shining dawn or gold.",
            explanationTe = "బంగారం యొక్క రసాయన చిహ్నం Au. ఇది లాటిన్ పదం 'Aurum' నుండి వచ్చింది."
        ),
        PracticeQuestion(
            subject = "Biology",
            question = "Which organelle is widely known as the powerhouse of the cell?",
            options = listOf("Nucleus", "Ribosome", "Mitochondria", "Golgi apparatus"),
            correctIndex = 2,
            explanationEn = "Mitochondria generate most of the chemical energy needed to power the cell's biochemical reactions via ATP.",
            explanationTe = "మైటోకాండ్రియా కణానికి అవసరమైన ATP శక్తిని ఉత్పత్తి చేస్తుంది, అందుకే దీనిని 'కణ శక్తి కేంద్రం' అంటారు."
        ),
        PracticeQuestion(
            subject = "Computer Science",
            question = "What is the time complexity of searching an element in a balanced Binary Search Tree (BST)?",
            options = listOf("O(1)", "O(n)", "O(log n)", "O(n log n)"),
            correctIndex = 2,
            explanationEn = "Because each comparison halves the search space in a balanced BST, the time complexity is logarithmic O(log n).",
            explanationTe = "సమతుల్య BST లో ప్రతి పోలిక శోధన పరిధిని సగానికి తగ్గిస్తుంది, కాబట్టి సమయ సంక్లిష్టత O(log n)."
        )
    )

    fun createStudyPrompt(subject: String, topic: String, explainInTelugu: Boolean): String {
        return if (explainInTelugu) {
            "మీరు ఒక అద్భుతమైన ఉపాధ్యాయుడు/జార్విస్. దయచేసి $subject లోని '$topic' అంశాన్ని స్పష్టంగా, సరళంగా మరియు సులభంగా అర్థమయ్యేలా తెలుగులో వివరించండి. ముఖ్యమైన సూత్రాలు మరియు ఉదాహరణలతో వివరించండి."
        } else {
            "You are JARVIS operating in Study Mode. Provide a crystal-clear, pedagogically sound, and engaging explanation of the topic '$topic' in $subject. Include real-world analogies, step-by-step principles, and key takeaway summary."
        }
    }

    fun createMathSolverPrompt(problem: String, explainInTelugu: Boolean): String {
        return if (explainInTelugu) {
            "ఈ గణిత సమస్యను దశలవారీగా (step-by-step) పరిష్కరించండి మరియు ప్రతి దశను తెలుగులో స్పష్టంగా వివరించండి:\n$problem"
        } else {
            "Solve the following mathematics problem with strict step-by-step breakdown, identifying given variables, formulas used, intermediate calculations, and the verified final answer:\n$problem"
        }
    }
}
