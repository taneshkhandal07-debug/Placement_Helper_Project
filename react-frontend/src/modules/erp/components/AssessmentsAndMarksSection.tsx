import React, { useState, useEffect } from 'react';
import { courseworkApi } from '../api/courseworkApi';
import {
  Assessment,
  StudentMark,
  SubjectResult,
  AssessmentCreateRequest,
  RecordMarkRequest,
  MarkCorrectionRequest,
  MarkAuditLog,
} from '../types/coursework.types';
import { Badge } from '@/shared/components/Badge';
import { Alert } from '@/shared/components/Alert';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';

interface AssessmentsAndMarksSectionProps {
  isFaculty: boolean;
  facultySubjects?: Array<{ subjectId: number; subjectCode: string; subjectName: string; sectionId: number; sectionName: string }>;
}

export const AssessmentsAndMarksSection: React.FC<AssessmentsAndMarksSectionProps> = ({
  isFaculty,
  facultySubjects = [],
}) => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Student state
  const [studentResults, setStudentResults] = useState<SubjectResult[]>([]);

  // Faculty state
  const [assessments, setAssessments] = useState<Assessment[]>([]);
  const [selectedAssessmentForMarks, setSelectedAssessmentForMarks] = useState<Assessment | null>(null);
  const [assessmentMarks, setAssessmentMarks] = useState<StudentMark[]>([]);
  const [loadingMarks, setLoadingMarks] = useState(false);

  // Faculty Schedule Modal
  const [showScheduleModal, setShowScheduleModal] = useState(false);
  const [scheduleForm, setScheduleForm] = useState<AssessmentCreateRequest>({
    subjectId: facultySubjects[0]?.subjectId || 1,
    semesterId: 1,
    name: '',
    assessmentType: 'INTERNAL',
    maxMarks: 50,
    passingMarks: 20,
    weightagePercent: 30,
    assessmentDate: new Date().toISOString().split('T')[0],
  });
  const [scheduling, setScheduling] = useState(false);

  // Faculty Record Mark Modal
  const [showRecordModal, setShowRecordModal] = useState(false);
  const [recordForm, setRecordForm] = useState<RecordMarkRequest>({
    assessmentId: 0,
    studentEnrollmentId: 1,
    marksObtained: 0,
    isAbsent: false,
    remarks: '',
  });
  const [recording, setRecording] = useState(false);

  // Faculty Correct Mark Modal
  const [correctingMark, setCorrectingMark] = useState<StudentMark | null>(null);
  const [correctForm, setCorrectForm] = useState<MarkCorrectionRequest>({
    markId: 0,
    newMarks: 0,
    isAbsent: false,
    reason: '',
  });
  const [savingCorrection, setSavingCorrection] = useState(false);

  // Audit Logs Modal
  const [viewingAuditMarkId, setViewingAuditMarkId] = useState<number | null>(null);
  const [auditLogs, setAuditLogs] = useState<MarkAuditLog[]>([]);
  const [loadingAudit, setLoadingAudit] = useState(false);

  const loadData = async () => {
    try {
      setLoading(true);
      setError(null);
      if (isFaculty) {
        const data = await courseworkApi.getAssessments();
        setAssessments(data);
      } else {
        const results = await courseworkApi.getMyResults();
        setStudentResults(results);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load assessment records.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, [isFaculty]);

  // Faculty: Schedule Assessment
  const handleScheduleAssessment = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setScheduling(true);
      setError(null);
      await courseworkApi.createAssessment(scheduleForm);
      setSuccessMsg(`Assessment "${scheduleForm.name}" scheduled successfully.`);
      setShowScheduleModal(false);
      setScheduleForm({
        subjectId: facultySubjects[0]?.subjectId || 1,
        semesterId: 1,
        name: '',
        assessmentType: 'INTERNAL',
        maxMarks: 50,
        passingMarks: 20,
        weightagePercent: 30,
        assessmentDate: new Date().toISOString().split('T')[0],
      });
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to schedule assessment.');
    } finally {
      setScheduling(false);
    }
  };

  // Faculty: Load Marks for Assessment
  const handleOpenMarks = async (a: Assessment) => {
    setSelectedAssessmentForMarks(a);
    try {
      setLoadingMarks(true);
      const marks = await courseworkApi.getAssessmentMarks(a.id);
      setAssessmentMarks(marks);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load marks for assessment.');
    } finally {
      setLoadingMarks(false);
    }
  };

  // Faculty: Publish Assessment
  const handlePublishAssessment = async (assessmentId: number) => {
    try {
      setError(null);
      const res = await courseworkApi.publishAssessment(assessmentId);
      setAssessments((prev) => prev.map((a) => (a.id === res.id ? res : a)));
      if (selectedAssessmentForMarks?.id === res.id) {
        setSelectedAssessmentForMarks(res);
      }
      setSuccessMsg(`Assessment results published. Enrolled students are notified.`);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to publish results.');
    }
  };

  // Faculty: Open Record Mark Modal
  const handleOpenRecordModal = (a: Assessment) => {
    setRecordForm({
      assessmentId: a.id,
      studentEnrollmentId: 1,
      marksObtained: 0,
      isAbsent: false,
      remarks: '',
    });
    setShowRecordModal(true);
  };

  // Faculty: Submit Record Mark
  const handleRecordMark = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setRecording(true);
      setError(null);
      const mark = await courseworkApi.recordMark(recordForm);
      setAssessmentMarks((prev) => {
        const idx = prev.findIndex((m) => m.id === mark.id);
        if (idx >= 0) {
          const updated = [...prev];
          updated[idx] = mark;
          return updated;
        }
        return [...prev, mark];
      });
      setSuccessMsg(`Mark recorded for student.`);
      setShowRecordModal(false);
      await loadData();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to record student mark.');
    } finally {
      setRecording(false);
    }
  };

  // Faculty: Open Correct Mark Modal
  const handleOpenCorrect = (m: StudentMark) => {
    setCorrectingMark(m);
    setCorrectForm({
      markId: m.id,
      newMarks: m.marksObtained !== null && m.marksObtained !== undefined ? m.marksObtained : 0,
      isAbsent: !!m.isAbsent,
      reason: '',
    });
  };

  // Faculty: Submit Mark Correction
  const handleSaveCorrection = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!correctingMark) return;

    try {
      setSavingCorrection(true);
      setError(null);
      const updated = await courseworkApi.correctMark(correctForm);
      setAssessmentMarks((prev) => prev.map((m) => (m.id === updated.id ? updated : m)));
      setSuccessMsg(`Mark updated with mandatory audit entry.`);
      setCorrectingMark(null);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to correct mark.');
    } finally {
      setSavingCorrection(false);
    }
  };

  // Faculty: View Audit Logs
  const handleViewAudit = async (markId: number) => {
    setViewingAuditMarkId(markId);
    try {
      setLoadingAudit(true);
      const logs = await courseworkApi.getMarkAuditLogs(markId);
      setAuditLogs(logs);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load audit trail.');
    } finally {
      setLoadingAudit(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header and Controls */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--text-color)' }}>
            {isFaculty ? 'Academic Assessments & Marks Management' : 'Academic Marks & Assessment Results'}
          </h2>
          <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            {isFaculty
              ? 'Schedule examinations, enter student marks, manage authorized corrections with audit trails, and publish results.'
              : 'Official university assessment scores, component marks, and performance breakdown by subject.'}
          </p>
        </div>

        {isFaculty && (
          <button
            onClick={() => setShowScheduleModal(true)}
            className="btn btn-primary"
            style={{ fontSize: '0.85rem' }}
          >
            Schedule Assessment
          </button>
        )}
      </div>

      {successMsg && (
        <Alert type="success" onClose={() => setSuccessMsg(null)}>
          {successMsg}
        </Alert>
      )}

      {error && (
        <Alert type="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Information Banner */}
      <div
        style={{
          padding: '0.75rem 1rem',
          backgroundColor: '#f8fafc',
          border: '1px solid var(--border-color)',
          borderRadius: '8px',
          fontSize: '0.85rem',
          color: 'var(--text-muted)',
        }}
      >
        <strong>Confidentiality Policy:</strong> In accordance with university regulations, student marks remain confidential
        until officially released and published by the assigned course instructor.
        All post-publication score modifications require an authorized audit justification.
      </div>

      {loading ? (
        <LoadingSpinner message="Loading academic assessments and scores..." />
      ) : isFaculty ? (
        /* FACULTY VIEW */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {assessments.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '2.5rem' }}>
              <p style={{ color: 'var(--text-muted)', margin: 0 }}>
                No assessments scheduled for your assigned subjects.
              </p>
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
              {assessments.map((a) => (
                <div
                  key={a.id}
                  className="card"
                  style={{
                    display: 'flex',
                    flexDirection: 'column',
                    justifyContent: 'space-between',
                    borderTop: `4px solid ${a.isPublished ? '#166534' : '#eab308'}`,
                  }}
                >
                  <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <Badge variant="info">{a.subjectCode}</Badge>
                      {a.isPublished ? (
                        <Badge variant="success">Published to Students</Badge>
                      ) : (
                        <Badge variant="warning">Draft / Unreleased</Badge>
                      )}
                    </div>

                    <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 0.25rem 0' }}>
                      {a.name}
                    </h3>
                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                      {a.subjectName} | Semester {a.semesterNumber}
                    </div>

                    <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', padding: '0.75rem', backgroundColor: '#f8fafc', borderRadius: '6px', marginBottom: '1rem' }}>
                      <div>
                        <span style={{ fontWeight: 600 }}>Type:</span>
                        <div>{a.assessmentType}</div>
                      </div>
                      <div>
                        <span style={{ fontWeight: 600 }}>Exam Date:</span>
                        <div>{a.assessmentDate || 'N/A'}</div>
                      </div>
                      <div>
                        <span style={{ fontWeight: 600 }}>Max Marks:</span>
                        <div>{a.maxMarks} (Pass: {a.passingMarks})</div>
                      </div>
                      <div>
                        <span style={{ fontWeight: 600 }}>Weightage:</span>
                        <div>{a.weightagePercent}%</div>
                      </div>
                    </div>
                  </div>

                  <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.5rem' }}>
                    <button
                      onClick={() => handleOpenMarks(a)}
                      className="btn btn-secondary"
                      style={{ fontSize: '0.8rem', flex: 1 }}
                    >
                      Marks ({a.evaluatedCount})
                    </button>

                    {!a.isPublished && (
                      <button
                        onClick={() => handlePublishAssessment(a.id)}
                        className="btn btn-primary"
                        style={{ fontSize: '0.8rem', flex: 1 }}
                      >
                        Publish Results
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      ) : (
        /* STUDENT VIEW */
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {studentResults.length === 0 ? (
            <div className="card" style={{ textAlign: 'center', padding: '2.5rem' }}>
              <p style={{ color: 'var(--text-muted)', margin: 0 }}>
                No published examination results available for your current semester yet.
              </p>
            </div>
          ) : (
            studentResults.map((sr) => (
              <div key={sr.subjectId} className="card" style={{ padding: '1.25rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '0.5rem', marginBottom: '1rem' }}>
                  <div>
                    <span style={{ fontSize: '0.8rem', fontWeight: 600, color: 'var(--primary-color)' }}>
                      {sr.subjectCode} ({sr.credits} Credits)
                    </span>
                    <h3 style={{ fontSize: '1.15rem', fontWeight: 700, margin: '0.2rem 0 0 0' }}>
                      {sr.subjectName}
                    </h3>
                  </div>

                  <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
                    <div style={{ textAlign: 'right' }}>
                      <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>Aggregate Score</div>
                      <div style={{ fontSize: '1.15rem', fontWeight: 700, color: sr.passed ? '#166534' : '#991b1b' }}>
                        {sr.totalMarksObtained} / {sr.totalMaxMarks} ({sr.percentage.toFixed(1)}%)
                      </div>
                    </div>
                    <Badge variant={sr.passed ? 'success' : 'danger'}>
                      Grade: {sr.letterGrade} ({sr.gradePoint} pts)
                    </Badge>
                  </div>
                </div>

                {/* Component assessments table */}
                <div style={{ overflowX: 'auto' }}>
                  <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                    <thead>
                      <tr style={{ borderBottom: '2px solid var(--border-color)', textAlign: 'left' }}>
                        <th style={{ padding: '0.5rem' }}>Component Assessment</th>
                        <th style={{ padding: '0.5rem' }}>Type</th>
                        <th style={{ padding: '0.5rem' }}>Weightage</th>
                        <th style={{ padding: '0.5rem' }}>Max Marks</th>
                        <th style={{ padding: '0.5rem' }}>Pass Marks</th>
                        <th style={{ padding: '0.5rem' }}>Marks Obtained</th>
                        <th style={{ padding: '0.5rem', textAlign: 'right' }}>Status</th>
                      </tr>
                    </thead>
                    <tbody>
                      {sr.componentMarks.map((cm) => (
                        <tr key={cm.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                          <td style={{ padding: '0.5rem', fontWeight: 600 }}>{cm.assessmentName}</td>
                          <td style={{ padding: '0.5rem' }}>{cm.assessmentType}</td>
                          <td style={{ padding: '0.5rem' }}>{cm.weightagePercent}%</td>
                          <td style={{ padding: '0.5rem' }}>{cm.maxMarks}</td>
                          <td style={{ padding: '0.5rem' }}>{cm.passingMarks}</td>
                          <td style={{ padding: '0.5rem', fontWeight: 600 }}>
                            {cm.isAbsent ? (
                              <span style={{ color: '#991b1b' }}>Absent</span>
                            ) : cm.marksObtained !== null && cm.marksObtained !== undefined ? (
                              `${cm.marksObtained} / ${cm.maxMarks}`
                            ) : (
                              'N/A'
                            )}
                          </td>
                          <td style={{ padding: '0.5rem', textAlign: 'right' }}>
                            {cm.isAbsent ? (
                              <Badge variant="danger">Absent</Badge>
                            ) : cm.isPassed ? (
                              <Badge variant="success">Passed</Badge>
                            ) : (
                              <Badge variant="danger">Failed</Badge>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            ))
          )}
        </div>
      )}

      {/* MODAL 1: Schedule Assessment Modal */}
      {showScheduleModal && (
        <Modal
          title="Schedule New Academic Assessment"
          isOpen={true}
          onClose={() => setShowScheduleModal(false)}
        >
          <form onSubmit={handleScheduleAssessment} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Teaching Subject
                </label>
                <select
                  value={scheduleForm.subjectId}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, subjectId: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                  required
                >
                  {facultySubjects.map((s) => (
                    <option key={`${s.subjectId}-${s.sectionId}`} value={s.subjectId}>
                      {s.subjectCode} - {s.subjectName}
                    </option>
                  ))}
                  {facultySubjects.length === 0 && <option value={1}>CS701 - Cloud Computing</option>}
                </select>
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Semester
                </label>
                <select
                  value={scheduleForm.semesterId}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, semesterId: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                  required
                >
                  <option value={1}>Semester 7 (Autumn 2026-27)</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Assessment Title
                </label>
                <input
                  type="text"
                  required
                  value={scheduleForm.name}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, name: e.target.value })}
                  placeholder="e.g. Mid-Term Examination 1"
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Type
                </label>
                <select
                  value={scheduleForm.assessmentType}
                  onChange={(e) =>
                    setScheduleForm({
                      ...scheduleForm,
                      assessmentType: e.target.value as any,
                    })
                  }
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                >
                  <option value="INTERNAL">INTERNAL</option>
                  <option value="MID_TERM">MID_TERM</option>
                  <option value="END_TERM">END_TERM</option>
                  <option value="PRACTICAL">PRACTICAL</option>
                  <option value="ASSIGNMENT">ASSIGNMENT</option>
                </select>
              </div>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Max Marks
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  required
                  value={scheduleForm.maxMarks}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, maxMarks: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Passing Marks
                </label>
                <input
                  type="number"
                  min="1"
                  max={scheduleForm.maxMarks}
                  required
                  value={scheduleForm.passingMarks}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, passingMarks: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Weightage (%)
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  required
                  value={scheduleForm.weightagePercent}
                  onChange={(e) => setScheduleForm({ ...scheduleForm, weightagePercent: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Assessment Date
              </label>
              <input
                type="date"
                required
                value={scheduleForm.assessmentDate}
                onChange={(e) => setScheduleForm({ ...scheduleForm, assessmentDate: e.target.value })}
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                onClick={() => setShowScheduleModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={scheduling}
                className="btn btn-primary"
              >
                {scheduling ? 'Scheduling...' : 'Save Assessment'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 2: Faculty View Assessment Marks & Enter / Correct */}
      {selectedAssessmentForMarks && (
        <Modal
          title={`Marks: ${selectedAssessmentForMarks.name} (Max: ${selectedAssessmentForMarks.maxMarks}, Pass: ${selectedAssessmentForMarks.passingMarks})`}
          isOpen={true}
          onClose={() => setSelectedAssessmentForMarks(null)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '600px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                Status:{' '}
                {selectedAssessmentForMarks.isPublished ? (
                  <Badge variant="success">Published</Badge>
                ) : (
                  <Badge variant="warning">Unpublished</Badge>
                )}
              </div>
              <button
                onClick={() => handleOpenRecordModal(selectedAssessmentForMarks)}
                className="btn btn-primary"
                style={{ fontSize: '0.8rem' }}
              >
                Record Student Mark
              </button>
            </div>

            {loadingMarks ? (
              <LoadingSpinner message="Loading marks list..." />
            ) : assessmentMarks.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1.5rem 0' }}>
                No student marks recorded for this assessment yet.
              </p>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid var(--border-color)', textAlign: 'left' }}>
                      <th style={{ padding: '0.5rem' }}>Roll No</th>
                      <th style={{ padding: '0.5rem' }}>Student Name</th>
                      <th style={{ padding: '0.5rem' }}>Score</th>
                      <th style={{ padding: '0.5rem' }}>Pass / Fail</th>
                      <th style={{ padding: '0.5rem' }}>Evaluated By</th>
                      <th style={{ padding: '0.5rem', textAlign: 'right' }}>Actions</th>
                    </tr>
                  </thead>
                  <tbody>
                    {assessmentMarks.map((m) => (
                      <tr key={m.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '0.5rem', fontWeight: 600 }}>{m.studentRollNumber}</td>
                        <td style={{ padding: '0.5rem' }}>{m.studentName}</td>
                        <td style={{ padding: '0.5rem', fontWeight: 600 }}>
                          {m.isAbsent ? (
                            <span style={{ color: '#991b1b' }}>Absent</span>
                          ) : (
                            `${m.marksObtained} / ${selectedAssessmentForMarks.maxMarks}`
                          )}
                        </td>
                        <td style={{ padding: '0.5rem' }}>
                          {m.isAbsent ? (
                            <Badge variant="danger">Absent</Badge>
                          ) : m.isPassed ? (
                            <Badge variant="success">Passed</Badge>
                          ) : (
                            <Badge variant="danger">Failed</Badge>
                          )}
                        </td>
                        <td style={{ padding: '0.5rem', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                          {m.gradedByName || 'Faculty'}
                        </td>
                        <td style={{ padding: '0.5rem', textAlign: 'right', whiteSpace: 'nowrap' }}>
                          <button
                            onClick={() => handleOpenCorrect(m)}
                            className="btn btn-secondary"
                            style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem', marginRight: '0.35rem' }}
                          >
                            Correct
                          </button>
                          <button
                            onClick={() => handleViewAudit(m.id)}
                            className="btn btn-secondary"
                            style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
                          >
                            Audit
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
              <button
                onClick={() => setSelectedAssessmentForMarks(null)}
                className="btn btn-secondary"
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL 3: Faculty Record New Mark */}
      {showRecordModal && (
        <Modal
          title="Record Student Assessment Mark"
          isOpen={true}
          onClose={() => setShowRecordModal(false)}
        >
          <form onSubmit={handleRecordMark} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Student Enrollment
              </label>
              <select
                value={recordForm.studentEnrollmentId}
                onChange={(e) => setRecordForm({ ...recordForm, studentEnrollmentId: Number(e.target.value) })}
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                required
              >
                <option value={1}>2021BTCS001 - Rahul Sharma</option>
                <option value={2}>2021BTCS002 - Priya Verma</option>
              </select>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                id="isStudentAbsent"
                checked={recordForm.isAbsent}
                onChange={(e) => setRecordForm({ ...recordForm, isAbsent: e.target.checked })}
              />
              <label htmlFor="isStudentAbsent" style={{ fontSize: '0.85rem', color: 'var(--text-color)' }}>
                Mark as Absent (Student did not appear for exam)
              </label>
            </div>

            {!recordForm.isAbsent && (
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Marks Awarded (Max: {selectedAssessmentForMarks?.maxMarks || 50})
                </label>
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  max={selectedAssessmentForMarks?.maxMarks || 100}
                  required
                  value={recordForm.marksObtained}
                  onChange={(e) => setRecordForm({ ...recordForm, marksObtained: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Examiner Remarks (Optional)
              </label>
              <input
                type="text"
                value={recordForm.remarks || ''}
                onChange={(e) => setRecordForm({ ...recordForm, remarks: e.target.value })}
                placeholder="Good performance on architectural design questions."
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                onClick={() => setShowRecordModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={recording}
                className="btn btn-primary"
              >
                {recording ? 'Saving...' : 'Record Mark'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 4: Faculty Correct Mark with Audit Reason */}
      {correctingMark && (
        <Modal
          title={`Authorized Mark Correction: ${correctingMark.studentName} (${correctingMark.studentRollNumber})`}
          isOpen={true}
          onClose={() => setCorrectingMark(null)}
        >
          <form onSubmit={handleSaveCorrection} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <Alert type="warning">
              All mark corrections are permanently recorded in the institutional audit log with your faculty credentials.
            </Alert>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Current Score
              </label>
              <div style={{ fontSize: '0.9rem', color: 'var(--text-muted)' }}>
                {correctingMark.isAbsent ? 'Absent' : `${correctingMark.marksObtained} / ${correctingMark.maxMarks}`}
              </div>
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                id="correctIsAbsent"
                checked={correctForm.isAbsent}
                onChange={(e) => setCorrectForm({ ...correctForm, isAbsent: e.target.checked })}
              />
              <label htmlFor="correctIsAbsent" style={{ fontSize: '0.85rem', color: 'var(--text-color)' }}>
                Mark as Absent
              </label>
            </div>

            {!correctForm.isAbsent && (
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Corrected Marks (Max: {correctingMark.maxMarks})
                </label>
                <input
                  type="number"
                  step="0.5"
                  min="0"
                  max={correctingMark.maxMarks}
                  required
                  value={correctForm.newMarks}
                  onChange={(e) => setCorrectForm({ ...correctForm, newMarks: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Mandatory Correction Reason / Justification
              </label>
              <textarea
                rows={3}
                required
                value={correctForm.reason}
                onChange={(e) => setCorrectForm({ ...correctForm, reason: e.target.value })}
                placeholder="e.g. Recounting of Section B question 4 after student verification."
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                onClick={() => setCorrectingMark(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={savingCorrection || !correctForm.reason.trim()}
                className="btn btn-primary"
              >
                {savingCorrection ? 'Saving...' : 'Authorize Correction'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 5: Audit Trail Logs */}
      {viewingAuditMarkId && (
        <Modal
          title="Authorized Mark Correction Audit History"
          isOpen={true}
          onClose={() => setViewingAuditMarkId(null)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '500px' }}>
            {loadingAudit ? (
              <LoadingSpinner message="Loading audit history..." />
            ) : auditLogs.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1.5rem 0' }}>
                No prior corrections recorded for this mark entry.
              </p>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid var(--border-color)', textAlign: 'left' }}>
                      <th style={{ padding: '0.5rem' }}>Date &amp; Time</th>
                      <th style={{ padding: '0.5rem' }}>Authorized By</th>
                      <th style={{ padding: '0.5rem' }}>Change</th>
                      <th style={{ padding: '0.5rem' }}>Justification</th>
                    </tr>
                  </thead>
                  <tbody>
                    {auditLogs.map((log) => (
                      <tr key={log.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '0.5rem' }}>{new Date(log.createdAt).toLocaleString()}</td>
                        <td style={{ padding: '0.5rem', fontWeight: 600 }}>{log.actionByUserName}</td>
                        <td style={{ padding: '0.5rem' }}>
                          <span style={{ textDecoration: 'line-through', color: '#991b1b', marginRight: '0.5rem' }}>
                            {log.previousMarks !== null && log.previousMarks !== undefined ? log.previousMarks : 'None'}
                          </span>
                          <span style={{ fontWeight: 600, color: '#166534' }}>
                            {log.newMarks !== null && log.newMarks !== undefined ? log.newMarks : 'Absent'}
                          </span>
                        </td>
                        <td style={{ padding: '0.5rem', color: 'var(--text-muted)' }}>{log.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1rem' }}>
              <button
                onClick={() => setViewingAuditMarkId(null)}
                className="btn btn-secondary"
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      )}
    </div>
  );
};
