package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.study.StudyManager
import com.example.ui.JarvisViewModel
import com.example.ui.theme.JarvisBorder
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkBg
import com.example.ui.theme.JarvisGold
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisRed
import com.example.ui.theme.JarvisSky
import com.example.ui.theme.JarvisSurface
import com.example.ui.theme.JarvisSurfaceVariant
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudyModeScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val currentQuestion by viewModel.currentQuizQuestion.collectAsState()
    val selectedAnswer by viewModel.selectedQuizAnswer.collectAsState()
    val showExplanation by viewModel.showQuizExplanation.collectAsState()
    val explainInTelugu by viewModel.studyInTelugu.collectAsState()

    var selectedSubject by remember { mutableStateOf("Mathematics") }
    var mathInput by remember { mutableStateOf("") }
    var conceptTopicInput by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkBg)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- 1. TITLE & SUBTITLE ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Study Mode",
                    tint = JarvisCyan,
                    modifier = Modifier.size(26.dp)
                )
                Column {
                    Text(
                        text = "ACADEMIC & STUDY CORE",
                        color = JarvisCyan,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Mathematics Solver, Concept Explainer & Practice Quiz",
                        color = JarvisTextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // --- 2. TELUGU EXPLANATION TOGGLE ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        tint = JarvisCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Column {
                        Text(
                            text = "తెలుగు వివరణ (Telugu Explanations)",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (explainInTelugu) "Active: Concepts & math explained in Telugu" else "Inactive: English mode",
                            color = if (explainInTelugu) JarvisGreen else JarvisTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = explainInTelugu,
                    onCheckedChange = { viewModel.studyInTelugu.value = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = JarvisCyan,
                        checkedTrackColor = JarvisSurfaceVariant
                    ),
                    modifier = Modifier.testTag("study_telugu_switch")
                )
            }
        }

        // --- 3. SUBJECT SELECTOR CHIPS ---
        Text(
            text = "SELECT ACADEMIC DOMAIN",
            color = JarvisSky,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            StudyManager.subjects.forEach { subject ->
                val isSelected = selectedSubject == subject
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) JarvisCyan else JarvisSurfaceVariant)
                        .border(1.dp, if (isSelected) JarvisCyan else JarvisBorder, RoundedCornerShape(16.dp))
                        .clickable { selectedSubject = subject }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = subject,
                        color = if (isSelected) Color(0xFF003036) else JarvisTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        // --- 4. INTERACTIVE PRACTICE QUIZ CARD ---
        currentQuestion?.let { q ->
            Card(
                colors = CardDefaults.cardColors(containerColor = JarvisSurfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.dp, JarvisCyan.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "PRACTICE QUIZ // ${q.subject.uppercase()}",
                            color = JarvisCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        TextButton(
                            text = "Next Question",
                            onClick = { viewModel.nextQuizQuestion() }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = q.question,
                        color = JarvisTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Options A, B, C, D
                    q.options.forEachIndexed { index, option ->
                        val isCorrectOption = index == q.correctIndex
                        val isChosen = selectedAnswer == index

                        val cardBg = when {
                            selectedAnswer == null -> JarvisSurface
                            isChosen && isCorrectOption -> Color(0xFF0F3A22)
                            isChosen && !isCorrectOption -> Color(0xFF3E1218)
                            isCorrectOption -> Color(0xFF0F3A22)
                            else -> JarvisSurface
                        }

                        val borderColor = when {
                            selectedAnswer == null -> JarvisBorder
                            isChosen && isCorrectOption -> JarvisGreen
                            isChosen && !isCorrectOption -> JarvisRed
                            isCorrectOption -> JarvisGreen
                            else -> JarvisBorder
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(cardBg)
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .clickable(enabled = selectedAnswer == null) {
                                    viewModel.answerQuiz(index)
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val letter = ('A' + index).toString()
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(if (isChosen) JarvisCyan else JarvisBorder),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = letter,
                                        color = if (isChosen) Color(0xFF003036) else JarvisTextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Text(
                                    text = option,
                                    color = JarvisTextPrimary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                if (selectedAnswer != null) {
                                    if (isCorrectOption) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = "Correct",
                                            tint = JarvisGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else if (isChosen) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Incorrect",
                                            tint = JarvisRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Explanation Box
                    AnimatedVisibility(visible = showExplanation) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF070F1D))
                                .border(1.dp, Color(0xFF1E3A63), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = if (selectedAnswer == q.correctIndex) "✓ EXCELLENT WORK, SIR" else "✗ REVISION REQUIRED",
                                color = if (selectedAnswer == q.correctIndex) JarvisGreen else JarvisGold,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (explainInTelugu) q.explanationTe else q.explanationEn,
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 5. MATH STEP-BY-STEP SOLVER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Calculate, contentDescription = "Math", tint = JarvisCyan)
                    Text(
                        text = "MATHEMATICS STEP-BY-STEP SOLVER",
                        color = JarvisCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = mathInput,
                    onValueChange = { mathInput = it },
                    placeholder = { Text("e.g. Solve 3x + 12 = 45 or Find derivative of sin(x)*e^x", color = JarvisTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (mathInput.isNotBlank()) {
                            viewModel.solveMathProblem(mathInput)
                            mathInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisCyan),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Solve with Full Step Breakdown", color = Color(0xFF003036), fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 6. CONCEPT EXPLAINER ---
        Card(
            colors = CardDefaults.cardColors(containerColor = JarvisSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, JarvisBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "DEEP CONCEPT EXPLAINER ($selectedSubject)",
                    color = JarvisSky,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = conceptTopicInput,
                    onValueChange = { conceptTopicInput = it },
                    placeholder = { Text("Topic name (e.g. CRISPR Gene Editing, Black Holes)", color = JarvisTextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = JarvisCyan,
                        unfocusedBorderColor = JarvisBorder,
                        focusedTextColor = JarvisTextPrimary,
                        unfocusedTextColor = JarvisTextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        if (conceptTopicInput.isNotBlank()) {
                            viewModel.askStudyConcept(selectedSubject, conceptTopicInput)
                            conceptTopicInput = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = JarvisSky),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Explain Concept", color = Color(0xFF00344F), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TextButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(JarvisBorder)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            color = JarvisCyan,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Icon(
            imageVector = Icons.Default.NavigateNext,
            contentDescription = null,
            tint = JarvisCyan,
            modifier = Modifier.size(14.dp)
        )
    }
}
