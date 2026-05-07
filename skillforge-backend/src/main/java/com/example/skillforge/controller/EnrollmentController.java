
package com.example.skillforge.controller;

import com.example.skillforge.dto.response.EnrollmentResponse;
import com.example.skillforge.model.entity.Enrollment;
import com.example.skillforge.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    @PostMapping
    public ResponseEntity<Enrollment> enrollCourse(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {
        try {
            Enrollment enrollment = enrollmentService.enrollStudent(studentId, courseId);
            return ResponseEntity.ok(enrollment);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/student/{studentId}")
    public ResponseEntity<List<EnrollmentResponse>> getStudentEnrollments(
            @PathVariable Long studentId
    ) {
        return ResponseEntity.ok(enrollmentService.getStudentEnrollments(studentId));
    }

    @PutMapping("/student/{studentId}/sync")
    public ResponseEntity<String> syncStudentEnrollments(
            @PathVariable Long studentId
    ) {
        enrollmentService.syncEnrollmentsWithProgress(studentId);
        return ResponseEntity.ok("Enrollments synced successfully");
    }

    @GetMapping
    public ResponseEntity<EnrollmentResponse> getEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.getEnrollment(studentId, courseId));
    }

    @PutMapping("/progress")
    public ResponseEntity<EnrollmentResponse> updateProgress(
            @RequestParam Long studentId,
            @RequestParam Long courseId,
            @RequestParam Integer completionPercentage
    ) {
        return ResponseEntity.ok(
                enrollmentService.updateProgress(studentId, courseId, completionPercentage)
        );
    }

    @DeleteMapping
    public ResponseEntity<String> unenrollCourse(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {
        try {
            enrollmentService.unenrollStudent(studentId, courseId);
            return ResponseEntity.ok("Unenrolled successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/check")
    public ResponseEntity<Boolean> checkEnrollment(
            @RequestParam Long studentId,
            @RequestParam Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.isEnrolled(studentId, courseId));
    }

    @GetMapping("/course/{courseId}")
    public ResponseEntity<List<EnrollmentResponse>> getCourseEnrollments(
            @PathVariable Long courseId
    ) {
        return ResponseEntity.ok(enrollmentService.getCourseEnrollments(courseId));
    }
}
