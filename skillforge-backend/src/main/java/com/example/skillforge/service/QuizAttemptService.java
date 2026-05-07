package com.example.skillforge.service;

import com.example.skillforge.model.entity.*;
import com.example.skillforge.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class QuizAttemptService {

    private final QuizAttemptRepository attemptRepository;
    private final AttemptAnswerRepository attemptAnswerRepository;
    private final QuestionRepository questionRepository;
    private final TopicQuizProgressRepository topicQuizProgressRepository;
    private final TopicProgressRepository topicProgressRepository;
    private final TopicRepository topicRepository;
    private final QuizRepository quizRepository;
    private final CourseProgressService courseProgressService;
    private final CompletionService completionService;
    private final UserActivityService userActivityService;
    private final StudentRepository studentRepository;

    @Transactional
    public QuizAttempt evaluateAndSaveAttempt(
            Long studentId,
            Long quizId,
            List<AnswerSubmission> answers,
            int timeSpentSeconds,
            Long topicId) {

        System.out.println("DEBUG: Evaluate Attempt - TimeSpent: " + timeSpentSeconds + "s, TopicId: " + topicId);
        
        // 1. Load quiz
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new RuntimeException("Quiz not found"));

        // 2. Load questions of this quiz
        List<Question> questions = questionRepository.findByQuizId(quizId);

        Map<Long, Question> qmap = new HashMap<>();
        double totalPoints = 0.0;

        for (Question q : questions) {
            qmap.put(q.getId(), q);
            totalPoints += q.getPoints() == null ? 1 : q.getPoints();
        }

        // 3. Evaluate each submitted answer
        double earned = 0.0;
        Map<Long, Integer> pointsMap = new HashMap<>();

        for (AnswerSubmission sub : answers) {

            Question q = qmap.get(sub.getQuestionId());
            int pts = 0;

            if (q != null && q.getCorrectAnswer() != null) {

                String correct = q.getCorrectAnswer().trim();
                String submitted = sub.getAnswerText() == null ? "" : sub.getAnswerText().trim();

                System.out.println("[QuizEval] QID: " + q.getId());
                System.out.println("[QuizEval] Correct: '" + correct + "'");
                System.out.println("[QuizEval] Submitted: '" + submitted + "'");

                if (correct.equalsIgnoreCase(submitted)) {
                    pts = q.getPoints() == null ? 1 : q.getPoints();
                    System.out.println("[QuizEval] Match! Points: " + pts);
                } else {
                    System.out.println("[QuizEval] Mismatch.");
                }
            }

            pointsMap.put(sub.getQuestionId(), pts);
            earned += pts;
        }

        double scorePercent = totalPoints == 0 ? 0 : (earned / totalPoints) * 100;

        // 4. Save QuizAttempt
        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudentId(studentId);
        attempt.setScore(scorePercent);
        attempt.setAttemptTime(LocalDateTime.now());
        attempt.setTimeSpent(timeSpentSeconds);
        attempt.setStatus("COMPLETED");

        attempt = attemptRepository.save(attempt);

        // 5. Save all answers
        for (AnswerSubmission sub : answers) {

            Question q = qmap.get(sub.getQuestionId());
            if (q == null)
                continue; // skip invalid questionIds

            AttemptAnswer aa = new AttemptAnswer();
            aa.setAttempt(attempt);
            aa.setQuestion(q);
            aa.setStudentAnswer(sub.getAnswerText());
            aa.setIsCorrect(pointsMap.get(sub.getQuestionId()) > 0);
            aa.setPointsEarned(pointsMap.get(sub.getQuestionId()));

            attemptAnswerRepository.save(aa);
        }

        // 6. Update progress via CompletionService (Strict Rules)
        if (topicId != null) {
            // Check Topic Completion (Passing marks required)
            boolean topicCompleted = completionService.checkTopicCompletion(studentId, topicId, attempt.getId());

            // If topic completed, or always, check Course Completion
            Topic topic = topicRepository.findById(topicId).orElse(null);
            if (topic != null) {
                completionService.checkCourseCompletion(studentId, topic.getCourse().getId());
                
                // 🔥 Add Time Spent to Course Progress
                if (timeSpentSeconds > 0) {
                    int mins = (int) Math.ceil(timeSpentSeconds / 60.0);
                    courseProgressService.addTimeSpent(studentId, topic.getCourse().getId(), mins);
                }
            }
        } else {
            // This might be a Course Quiz (Final Quiz)
            Long courseId = quiz.getCourse().getId();
            completionService.checkCourseCompletion(studentId, courseId);
            
            // 🔥 Add Time Spent to Course Progress
            if (timeSpentSeconds > 0) {
                int mins = (int) Math.ceil(timeSpentSeconds / 60.0);
                courseProgressService.addTimeSpent(studentId, courseId, mins);
            }
        }

        // 🔥 Log Activity Time for Analytics
        if (timeSpentSeconds > 0) {
            int mins = (int) Math.ceil(timeSpentSeconds / 60.0);
            try {
                studentRepository.findById(studentId).ifPresent(student -> {
                     userActivityService.logTime(student.getUser().getId(), mins);
                });
            } catch (Exception e) {
                System.err.println("Failed to log activity time: " + e.getMessage());
            }
        }

        return attempt;
    }

    public List<QuizAttempt> getAttempts(Long studentId, Long quizId) {
        return attemptRepository.findByQuizIdAndStudentId(quizId, studentId);
    }

    // DTO for incoming answers
    public static class AnswerSubmission {
        private Long questionId;
        private String answerText;

        public Long getQuestionId() {
            return questionId;
        }

        public void setQuestionId(Long id) {
            this.questionId = id;
        }

        public String getAnswerText() {
            return answerText;
        }

        public void setAnswerText(String a) {
            this.answerText = a;
        }
    }
}
