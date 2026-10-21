package in.jecrcu.placement.service.crt;

import in.jecrcu.placement.dto.crt.*;
import in.jecrcu.placement.entity.crt.*;

import java.math.BigDecimal;
import java.util.List;

public interface CrtService {

    CrtEligibilityDto checkStudentEligibility(Long studentUserId);

    CrtEnrollment optInForCrt(Long studentUserId, CrtOptInRequestDto request);

    CrtEnrollment verifyEnrollment(Long enrollmentId, Long reviewerUserId, CrtVerificationActionDto action);

    List<CrtEnrollment> getPendingEnrollments();

    List<CrtBatch> getActiveBatches();

    CrtStudentDashboardDto getStudentDashboard(Long studentUserId);

    List<CrtSessionDto> getSessionsForBatch(Long batchId);

    CrtSession createSession(Long batchId, Long trackId, Long trainerUserId, CrtSessionDto sessionDto);

    void markSessionAttendance(Long sessionId, Long trainerUserId, List<CrtAttendanceRecordDto> records);

    List<CrtAttendanceRecordDto> getSessionAttendance(Long sessionId);

    CrtAttendanceCorrection requestCorrection(Long studentUserId, CrtCorrectionRequestDto request);

    void reviewCorrection(Long correctionId, Long reviewerUserId, boolean approved, String remarks);

    List<CrtAttendanceCorrection> getPendingCorrections();

    List<CrtAssessment> getAssessmentsForBatch(Long batchId);

    void recordAssessmentScore(Long assessmentId, Long studentUserId, BigDecimal marks, String remarks, Long gradedByUserId);

    CrtAttendanceSummaryDto calculateStudentAttendance(Long studentUserId, Long batchId);
}
