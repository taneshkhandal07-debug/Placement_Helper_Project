package in.jecrcu.placement.service.integration;

import in.jecrcu.placement.dto.integration.*;

import java.util.List;

public interface PlacementIntegrationService {

    List<ScheduleConflictDto> detectAndRecordConflicts(Long upcomingDriveId);

    List<ScheduleConflictDto> detectAndRecordConflictsForStudent(Long upcomingDriveId, Long studentUserId);

    int sendAdvanceConflictNotifications(Long upcomingDriveId, boolean forceResend);

    boolean sendNotificationForConflict(Long conflictId);

    PlacementParticipationDto verifyStudentParticipation(ParticipationVerifyRequestDto request, Long verifiedByUserId);

    List<DutyExemptionDto> applyDutyExemptionForParticipation(Long participationId, Long approvedByUserId, String remarks);

    DutyExemptionDto revokeDutyExemption(Long exemptionId, Long revokedByUserId, String reason);

    IntegrationOverviewDto getOverview();

    List<ScheduleConflictDto> getConflicts(Long driveId, Long studentId, Long facultyId);

    List<PlacementParticipationDto> getParticipationsByDrive(Long driveId);

    List<PlacementParticipationDto> getParticipationsByStudent(Long studentId);

    List<DutyExemptionDto> getExemptionsByStudent(Long studentId);

    List<DutyExemptionDto> getAllExemptions();

    void handleDriveTimingChangeOrCancellation(Long upcomingDriveId, String action, String reason, Long actorUserId);
}
