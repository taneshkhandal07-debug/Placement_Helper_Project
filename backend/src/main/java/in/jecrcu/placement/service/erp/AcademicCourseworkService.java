package in.jecrcu.placement.service.erp;

import in.jecrcu.placement.dto.erp.*;

import java.util.List;

public interface AcademicCourseworkService {

    // Assignments
    AssignmentDto createAssignment(AssignmentCreateRequestDto request, Long facultyUserId);
    List<AssignmentDto> getAssignmentsForSection(Long sectionId, Long currentUserId);
    List<AssignmentDto> getAssignmentsForFaculty(Long facultyUserId);
    AssignmentDto getAssignmentDetail(Long assignmentId, Long currentUserId);

    // Submissions
    SubmissionDto submitAssignment(Long assignmentId, SubmissionSubmitRequestDto request, Long studentUserId);
    List<SubmissionDto> getSubmissionsForAssignment(Long assignmentId, Long facultyUserId);
    SubmissionDto gradeSubmission(Long submissionId, SubmissionGradeRequestDto request, Long facultyUserId);
    List<SubmissionDto> getStudentSubmissions(Long studentUserId);

    // Assessments
    AssessmentDto createAssessment(AssessmentCreateRequestDto request, Long facultyUserId);
    List<AssessmentDto> getAssessmentsForFaculty(Long facultyUserId);
    List<AssessmentDto> getAssessmentsForSemester(Long semesterId, Long currentUserId);
    AssessmentDto publishAssessmentResults(Long assessmentId, Long facultyUserId);

    // Marks
    StudentMarkDto recordStudentMark(RecordMarkRequestDto request, Long facultyUserId);
    int batchRecordMarks(Long assessmentId, List<RecordMarkRequestDto> marks, Long facultyUserId);
    List<StudentMarkDto> getMarksForAssessment(Long assessmentId, Long facultyUserId);
    StudentMarkDto correctStudentMark(MarkCorrectionRequestDto request, Long facultyUserId);
    List<StudentMarkDto> getStudentPublishedMarks(Long studentUserId, Long semesterId);

    // Report Cards
    SemesterReportCardDto generateSemesterReportCard(Long studentUserId, Long semesterId);
    byte[] exportReportCardPdf(Long studentUserId, Long semesterId) throws Exception;
}
