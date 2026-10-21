package in.jecrcu.placement.controller.erp;

import in.jecrcu.placement.config.CustomUserDetails;
import in.jecrcu.placement.dto.erp.*;
import in.jecrcu.placement.entity.enums.Role;
import in.jecrcu.placement.entity.erp.Semester;
import in.jecrcu.placement.entity.erp.StudentEnrollment;
import in.jecrcu.placement.repository.erp.SemesterRepository;
import in.jecrcu.placement.repository.erp.StudentEnrollmentRepository;
import in.jecrcu.placement.service.erp.AcademicCourseworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/erp/coursework")
@RequiredArgsConstructor
@Slf4j
public class AcademicCourseworkRestController {

    private final AcademicCourseworkService courseworkService;
    private final StudentEnrollmentRepository studentEnrollmentRepository;
    private final SemesterRepository semesterRepository;

    // -------------------------------------------------------------------------
    // Assignments
    // -------------------------------------------------------------------------

    @GetMapping("/assignments")
    public ResponseEntity<?> getAssignments(
            @RequestParam(value = "sectionId", required = false) Long sectionId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        if (userDetails.getRole() == Role.ROLE_STUDENT) {
            StudentEnrollment se = studentEnrollmentRepository.findByUserId(userDetails.getId()).orElse(null);
            if (se == null || se.getSection() == null) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            return ResponseEntity.ok(courseworkService.getAssignmentsForSection(se.getSection().getId(), userDetails.getId()));
        } else if (userDetails.getRole() == Role.ROLE_FACULTY) {
            if (sectionId != null) {
                return ResponseEntity.ok(courseworkService.getAssignmentsForSection(sectionId, userDetails.getId()));
            }
            return ResponseEntity.ok(courseworkService.getAssignmentsForFaculty(userDetails.getId()));
        } else {
            // ADMIN
            if (sectionId != null) {
                return ResponseEntity.ok(courseworkService.getAssignmentsForSection(sectionId, userDetails.getId()));
            }
            return ResponseEntity.ok(courseworkService.getAssignmentsForFaculty(userDetails.getId()));
        }
    }

    @PostMapping("/assignments")
    public ResponseEntity<?> createAssignment(
            @RequestBody AssignmentCreateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        try {
            AssignmentDto dto = courseworkService.createAssignment(request, userDetails.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to create assignment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/assignments/{id}")
    public ResponseEntity<?> getAssignmentDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        try {
            return ResponseEntity.ok(courseworkService.getAssignmentDetail(id, userDetails.getId()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    // -------------------------------------------------------------------------
    // Submissions
    // -------------------------------------------------------------------------

    @PostMapping("/assignments/{id}/submit")
    public ResponseEntity<?> submitAssignment(
            @PathVariable Long id,
            @RequestBody SubmissionSubmitRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_STUDENT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Only enrolled students can submit assignments"));
        }

        try {
            SubmissionDto dto = courseworkService.submitAssignment(id, request, userDetails.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to submit assignment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/assignments/{id}/submissions")
    public ResponseEntity<?> getAssignmentSubmissions(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty access required to view submissions"));
        }

        try {
            List<SubmissionDto> list = courseworkService.getSubmissionsForAssignment(id, userDetails.getId());
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/submissions/{submissionId}/grade")
    public ResponseEntity<?> gradeSubmission(
            @PathVariable Long submissionId,
            @RequestBody SubmissionGradeRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required to grade submissions"));
        }

        try {
            SubmissionDto dto = courseworkService.gradeSubmission(submissionId, request, userDetails.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to grade submission", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-submissions")
    public ResponseEntity<?> getMySubmissions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null || userDetails.getRole() != Role.ROLE_STUDENT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Student access required"));
        }

        return ResponseEntity.ok(courseworkService.getStudentSubmissions(userDetails.getId()));
    }

    // -------------------------------------------------------------------------
    // Assessments & Marks
    // -------------------------------------------------------------------------

    @GetMapping("/assessments")
    public ResponseEntity<?> getAssessments(
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        Long semId = semesterId;
        if (semId == null) {
            semId = semesterRepository.findFirstByIsActiveTrue().map(Semester::getId).orElse(1L);
        }

        if (userDetails.getRole() == Role.ROLE_STUDENT) {
            return ResponseEntity.ok(courseworkService.getAssessmentsForSemester(semId, userDetails.getId()));
        } else {
            return ResponseEntity.ok(courseworkService.getAssessmentsForFaculty(userDetails.getId()));
        }
    }

    @PostMapping("/assessments")
    public ResponseEntity<?> createAssessment(
            @RequestBody AssessmentCreateRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        try {
            AssessmentDto dto = courseworkService.createAssessment(request, userDetails.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to create assessment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/assessments/{id}/publish")
    public ResponseEntity<?> publishAssessment(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        try {
            AssessmentDto dto = courseworkService.publishAssessmentResults(id, userDetails.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Results published successfully and students notified",
                    "assessment", dto
            ));
        } catch (Exception e) {
            log.error("Failed to publish assessment results", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/assessments/{id}/marks")
    public ResponseEntity<?> getAssessmentMarks(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        return ResponseEntity.ok(courseworkService.getMarksForAssessment(id, userDetails.getId()));
    }

    @PostMapping("/assessments/{id}/marks")
    public ResponseEntity<?> recordMark(
            @PathVariable Long id,
            @RequestBody RecordMarkRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        request.setAssessmentId(id);
        try {
            StudentMarkDto dto = courseworkService.recordStudentMark(request, userDetails.getId());
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            log.error("Failed to record mark", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/assessments/{id}/marks/batch")
    public ResponseEntity<?> batchRecordMarks(
            @PathVariable Long id,
            @RequestBody List<RecordMarkRequestDto> marks,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        try {
            int count = courseworkService.batchRecordMarks(id, marks, userDetails.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Marks recorded successfully",
                    "updatedCount", count
            ));
        } catch (Exception e) {
            log.error("Failed to batch record marks", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/marks/{markId}/correct")
    public ResponseEntity<?> correctMark(
            @PathVariable Long markId,
            @RequestBody MarkCorrectionRequestDto request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || (userDetails.getRole() != Role.ROLE_FACULTY && userDetails.getRole() != Role.ROLE_ADMIN)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Faculty authorization required"));
        }

        request.setMarkId(markId);
        try {
            StudentMarkDto dto = courseworkService.correctStudentMark(request, userDetails.getId());
            return ResponseEntity.ok(Map.of(
                    "message", "Result corrected successfully and audit log recorded",
                    "mark", dto
            ));
        } catch (Exception e) {
            log.error("Failed to correct mark", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my-marks")
    public ResponseEntity<?> getMyMarks(
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null || userDetails.getRole() != Role.ROLE_STUDENT) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Student access required"));
        }

        Long semId = semesterId;
        if (semId == null) {
            semId = semesterRepository.findFirstByIsActiveTrue().map(Semester::getId).orElse(1L);
        }

        return ResponseEntity.ok(courseworkService.getStudentPublishedMarks(userDetails.getId(), semId));
    }

    // -------------------------------------------------------------------------
    // Semester Report Card
    // -------------------------------------------------------------------------

    @GetMapping("/report-card")
    public ResponseEntity<?> getReportCard(
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        Long targetStudentUserId = userDetails.getId();
        if (userDetails.getRole() != Role.ROLE_STUDENT) {
            if (studentId != null) {
                targetStudentUserId = studentId;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "studentId is required for staff review"));
            }
        }

        Long semId = semesterId;
        if (semId == null) {
            semId = semesterRepository.findFirstByIsActiveTrue().map(Semester::getId).orElse(1L);
        }

        try {
            SemesterReportCardDto rc = courseworkService.generateSemesterReportCard(targetStudentUserId, semId);
            return ResponseEntity.ok(rc);
        } catch (Exception e) {
            log.error("Failed to generate report card", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/report-card/download")
    public ResponseEntity<?> downloadReportCardPdf(
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "studentId", required = false) Long studentId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Authentication required"));
        }

        Long targetStudentUserId = userDetails.getId();
        if (userDetails.getRole() != Role.ROLE_STUDENT) {
            if (studentId != null) {
                targetStudentUserId = studentId;
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "studentId is required for staff report generation"));
            }
        }

        Long semId = semesterId;
        if (semId == null) {
            semId = semesterRepository.findFirstByIsActiveTrue().map(Semester::getId).orElse(1L);
        }

        try {
            byte[] pdf = courseworkService.exportReportCardPdf(targetStudentUserId, semId);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "Semester_Report_Card_Sem" + semId + ".pdf");
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed to generate report card PDF", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", e.getMessage()));
        }
    }
}
