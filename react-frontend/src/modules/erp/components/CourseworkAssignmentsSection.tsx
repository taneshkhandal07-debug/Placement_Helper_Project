import React, { useState, useEffect } from 'react';
import { courseworkApi } from '../api/courseworkApi';
import { Assignment, Submission, AssignmentCreateRequest } from '../types/coursework.types';
import { Badge } from '@/shared/components/Badge';
import { Alert } from '@/shared/components/Alert';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';

interface CourseworkAssignmentsSectionProps {
  isFaculty: boolean;
  facultySubjects?: Array<{ subjectId: number; subjectCode: string; subjectName: string; sectionId: number; sectionName: string }>;
}

export const CourseworkAssignmentsSection: React.FC<CourseworkAssignmentsSectionProps> = ({
  isFaculty,
  facultySubjects = [],
}) => {
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [successMsg, setSuccessMsg] = useState<string | null>(null);

  // Student Submission state
  const [selectedAssignmentForSubmit, setSelectedAssignmentForSubmit] = useState<Assignment | null>(null);
  const [mySubmissionsMap, setMySubmissionsMap] = useState<Record<number, Submission | null>>({});
  const [submissionText, setSubmissionText] = useState('');
  const [attachmentUrl, setAttachmentUrl] = useState('');
  const [submittingWork, setSubmittingWork] = useState(false);

  // Faculty Create Assignment state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createForm, setCreateForm] = useState<AssignmentCreateRequest>({
    subjectId: facultySubjects[0]?.subjectId || 1,
    sectionId: facultySubjects[0]?.sectionId || 1,
    title: '',
    description: '',
    attachmentUrl: '',
    dueDate: '',
    maxMarks: 20,
    allowLateSubmission: false,
  });
  const [creatingAssignment, setCreatingAssignment] = useState(false);

  // Faculty View Submissions & Grade state
  const [selectedAssignmentForGrading, setSelectedAssignmentForGrading] = useState<Assignment | null>(null);
  const [submissionsList, setSubmissionsList] = useState<Submission[]>([]);
  const [loadingSubmissions, setLoadingSubmissions] = useState(false);
  const [gradingSubmissionId, setGradingSubmissionId] = useState<number | null>(null);
  const [gradeMarks, setGradeMarks] = useState<number>(0);
  const [gradeFeedback, setGradeFeedback] = useState<string>('');
  const [savingGrade, setSavingGrade] = useState(false);

  const loadAssignments = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await courseworkApi.getAssignments();
      setAssignments(data);

      if (!isFaculty) {
        // Fetch submission status for student assignments
        const map: Record<number, Submission | null> = {};
        for (const a of data) {
          try {
            const sub = await courseworkApi.getMySubmission(a.id);
            map[a.id] = sub;
          } catch {
            map[a.id] = null;
          }
        }
        setMySubmissionsMap(map);
      }
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load assignments.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAssignments();
  }, [isFaculty]);

  // Handle student submit
  const handleOpenSubmit = (a: Assignment) => {
    setSelectedAssignmentForSubmit(a);
    const existing = mySubmissionsMap[a.id];
    setSubmissionText(existing?.submissionText || '');
    setAttachmentUrl(existing?.attachmentUrl || '');
  };

  const handleStudentSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAssignmentForSubmit) return;

    try {
      setSubmittingWork(true);
      setError(null);
      const result = await courseworkApi.submitAssignment(selectedAssignmentForSubmit.id, {
        submissionText,
        attachmentUrl,
      });
      setMySubmissionsMap((prev) => ({ ...prev, [selectedAssignmentForSubmit.id]: result }));
      setSuccessMsg(`Assignment "${selectedAssignmentForSubmit.title}" submitted successfully.`);
      setSelectedAssignmentForSubmit(null);
      await loadAssignments();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to submit assignment.');
    } finally {
      setSubmittingWork(false);
    }
  };

  // Handle faculty create
  const handleCreateAssignment = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setCreatingAssignment(true);
      setError(null);
      await courseworkApi.createAssignment(createForm);
      setSuccessMsg('Academic assignment created and published to students.');
      setShowCreateModal(false);
      setCreateForm({
        subjectId: facultySubjects[0]?.subjectId || 1,
        sectionId: facultySubjects[0]?.sectionId || 1,
        title: '',
        description: '',
        attachmentUrl: '',
        dueDate: '',
        maxMarks: 20,
        allowLateSubmission: false,
      });
      await loadAssignments();
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to create assignment.');
    } finally {
      setCreatingAssignment(false);
    }
  };

  // Handle faculty load submissions
  const handleOpenSubmissions = async (a: Assignment) => {
    setSelectedAssignmentForGrading(a);
    try {
      setLoadingSubmissions(true);
      const subs = await courseworkApi.getAssignmentSubmissions(a.id);
      setSubmissionsList(subs);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load student submissions.');
    } finally {
      setLoadingSubmissions(false);
    }
  };

  // Handle grade submission
  const handleOpenGradeModal = (sub: Submission) => {
    setGradingSubmissionId(sub.id);
    setGradeMarks(sub.marksObtained !== undefined ? sub.marksObtained : 0);
    setGradeFeedback(sub.facultyFeedback || '');
  };

  const handleSaveGrade = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!gradingSubmissionId) return;

    try {
      setSavingGrade(true);
      setError(null);
      const updated = await courseworkApi.gradeSubmission(gradingSubmissionId, {
        marksObtained: gradeMarks,
        facultyFeedback: gradeFeedback,
      });
      setSubmissionsList((prev) => prev.map((s) => (s.id === updated.id ? updated : s)));
      setSuccessMsg('Submission graded and feedback recorded.');
      setGradingSubmissionId(null);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to save grade.');
    } finally {
      setSavingGrade(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header and Action */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--text-color)' }}>
            Coursework Assignments
          </h2>
          <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            {isFaculty
              ? 'Create course assignments, monitor student submissions, and record academic evaluations.'
              : 'Review coursework deliverables, verify deadlines, and submit assigned laboratory or theory work.'}
          </p>
        </div>
        {isFaculty && (
          <button
            onClick={() => setShowCreateModal(true)}
            className="btn btn-primary"
            style={{ fontSize: '0.85rem' }}
          >
            Create Assignment
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

      {/* Coursework Rules Notification Banner */}
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
        <strong>Academic Policy:</strong> Assignments submitted after the due date are flagged as Late Submissions.
        Unless late submissions are explicitly permitted by the course instructor, past-due submissions are rejected by institutional rule.
      </div>

      {loading ? (
        <LoadingSpinner message="Loading coursework assignments..." />
      ) : assignments.length === 0 ? (
        <div className="card" style={{ textAlign: 'center', padding: '2.5rem' }}>
          <p style={{ color: 'var(--text-muted)', margin: 0 }}>
            No coursework assignments currently scheduled.
          </p>
        </div>
      ) : (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))', gap: '1rem' }}>
          {assignments.map((a) => {
            const mySub = mySubmissionsMap[a.id];
            const isSubmitted = !!mySub;
            const isGraded = mySub && mySub.marksObtained !== null && mySub.marksObtained !== undefined;
            const isPastDue = a.isOverdue;

            return (
              <div
                key={a.id}
                className="card"
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  borderTop: `4px solid ${isFaculty ? 'var(--primary-color)' : isSubmitted ? '#166534' : isPastDue ? '#991b1b' : 'var(--primary-color)'}`,
                }}
              >
                <div>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: '0.5rem', marginBottom: '0.5rem' }}>
                    <Badge variant="info">
                      {a.subjectCode}
                    </Badge>
                    <div style={{ display: 'flex', gap: '0.25rem' }}>
                      {a.allowLateSubmission ? (
                        <Badge variant="neutral">Late Allowed</Badge>
                      ) : (
                        <Badge variant="warning">Strict Deadline</Badge>
                      )}
                      {isPastDue && <Badge variant="danger">Overdue</Badge>}
                    </div>
                  </div>

                  <h3 style={{ fontSize: '1.05rem', fontWeight: 600, margin: '0 0 0.25rem 0', color: 'var(--text-color)' }}>
                    {a.title}
                  </h3>
                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginBottom: '0.75rem' }}>
                    {a.subjectName} | Section {a.sectionName || 'All'}
                  </div>

                  <p style={{ fontSize: '0.85rem', color: 'var(--text-color)', margin: '0 0 1rem 0', lineHeight: 1.4 }}>
                    {a.description}
                  </p>

                  {a.attachmentUrl && (
                    <div style={{ marginBottom: '1rem' }}>
                      <a
                        href={a.attachmentUrl}
                        target="_blank"
                        rel="noreferrer"
                        style={{ fontSize: '0.8rem', color: 'var(--primary-color)', textDecoration: 'underline' }}
                      >
                        View Assignment Brief / Reference
                      </a>
                    </div>
                  )}

                  <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', padding: '0.75rem', backgroundColor: '#f8fafc', borderRadius: '6px', marginBottom: '1rem' }}>
                    <div>
                      <span style={{ fontWeight: 600 }}>Due Date:</span>
                      <div>{a.dueDate ? new Date(a.dueDate).toLocaleString() : 'N/A'}</div>
                    </div>
                    <div>
                      <span style={{ fontWeight: 600 }}>Max Marks:</span>
                      <div>{a.maxMarks}</div>
                    </div>
                  </div>

                  {!isFaculty && (
                    <div style={{ marginBottom: '1rem', padding: '0.75rem', backgroundColor: isSubmitted ? '#f0fdf4' : '#fffbeb', borderRadius: '6px', border: `1px solid ${isSubmitted ? '#bbf7d0' : '#fef3c7'}` }}>
                      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                        <span style={{ fontSize: '0.8rem', fontWeight: 600, color: isSubmitted ? '#166534' : '#92400e' }}>
                          Status: {isSubmitted ? 'Submitted' : 'Pending'}
                        </span>
                        {isSubmitted && mySub?.isLate && (
                          <Badge variant="warning">Late Submission</Badge>
                        )}
                      </div>
                      {isSubmitted && mySub && (
                        <div style={{ marginTop: '0.5rem', fontSize: '0.8rem' }}>
                          <div>Submitted: {new Date(mySub.submittedAt).toLocaleString()}</div>
                          {isGraded ? (
                            <div style={{ marginTop: '0.25rem', color: '#166534', fontWeight: 600 }}>
                              Marks Awarded: {mySub.marksObtained} / {a.maxMarks}
                              {mySub.facultyFeedback && (
                                <div style={{ fontWeight: 400, marginTop: '0.2rem', color: '#334155' }}>
                                  Feedback: &quot;{mySub.facultyFeedback}&quot;
                                </div>
                              )}
                            </div>
                          ) : (
                            <div style={{ marginTop: '0.25rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>
                              Awaiting faculty evaluation
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  )}
                </div>

                <div style={{ borderTop: '1px solid var(--border-color)', paddingTop: '0.75rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                  {isFaculty ? (
                    <>
                      <span style={{ fontSize: '0.8rem', color: 'var(--text-muted)' }}>
                        {a.submissionCount} Submissions
                      </span>
                      <button
                        onClick={() => handleOpenSubmissions(a)}
                        className="btn btn-secondary"
                        style={{ fontSize: '0.8rem' }}
                      >
                        View &amp; Grade
                      </button>
                    </>
                  ) : (
                    <>
                      <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                        Instructor: {a.facultyName}
                      </span>
                      <button
                        onClick={() => handleOpenSubmit(a)}
                        disabled={isPastDue && !a.allowLateSubmission && !isSubmitted}
                        className={`btn ${isSubmitted ? 'btn-secondary' : 'btn-primary'}`}
                        style={{ fontSize: '0.8rem' }}
                      >
                        {isSubmitted ? 'Update Submission' : 'Submit Work'}
                      </button>
                    </>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* MODAL 1: Student Submit Work */}
      {selectedAssignmentForSubmit && (
        <Modal
          title={`Submit Work: ${selectedAssignmentForSubmit.title}`}
          isOpen={true}
          onClose={() => setSelectedAssignmentForSubmit(null)}
        >
          <form onSubmit={handleStudentSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            {selectedAssignmentForSubmit.isOverdue && selectedAssignmentForSubmit.allowLateSubmission && (
              <Alert type="warning">
                Notice: The deadline for this assignment has passed. Your submission will be recorded and marked as Late Submission.
              </Alert>
            )}

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Submission Notes / Solution Text
              </label>
              <textarea
                rows={4}
                value={submissionText}
                onChange={(e) => setSubmissionText(e.target.value)}
                placeholder="Enter answers, implementation summary, or Git repository details..."
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Deliverable Document / Cloud Storage Link
              </label>
              <input
                type="url"
                value={attachmentUrl}
                onChange={(e) => setAttachmentUrl(e.target.value)}
                placeholder="https://drive.google.com/... or https://github.com/..."
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>
                Provide a shareable URL to your report, PDF, or codebase.
              </span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                onClick={() => setSelectedAssignmentForSubmit(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={submittingWork || (!submissionText.trim() && !attachmentUrl.trim())}
                className="btn btn-primary"
              >
                {submittingWork ? 'Submitting...' : 'Confirm Submission'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 2: Faculty Create Assignment */}
      {showCreateModal && (
        <Modal
          title="Create New Academic Assignment"
          isOpen={true}
          onClose={() => setShowCreateModal(false)}
        >
          <form onSubmit={handleCreateAssignment} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Teaching Subject
                </label>
                <select
                  value={createForm.subjectId}
                  onChange={(e) => setCreateForm({ ...createForm, subjectId: Number(e.target.value) })}
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
                  Section
                </label>
                <select
                  value={createForm.sectionId}
                  onChange={(e) => setCreateForm({ ...createForm, sectionId: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                >
                  <option value={1}>Section A</option>
                  <option value={2}>Section B</option>
                </select>
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Assignment Title
              </label>
              <input
                type="text"
                required
                value={createForm.title}
                onChange={(e) => setCreateForm({ ...createForm, title: e.target.value })}
                placeholder="e.g. Lab Exercise 4: Terraform AWS Infrastructure"
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Description &amp; Problem Statement
              </label>
              <textarea
                rows={3}
                required
                value={createForm.description}
                onChange={(e) => setCreateForm({ ...createForm, description: e.target.value })}
                placeholder="Provide detailed instructions, grading rubric, or expected deliverables..."
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Due Date &amp; Time
                </label>
                <input
                  type="datetime-local"
                  required
                  value={createForm.dueDate}
                  onChange={(e) => setCreateForm({ ...createForm, dueDate: e.target.value })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                  Max Marks
                </label>
                <input
                  type="number"
                  min="1"
                  max="100"
                  required
                  value={createForm.maxMarks}
                  onChange={(e) => setCreateForm({ ...createForm, maxMarks: Number(e.target.value) })}
                  style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
                />
              </div>
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Assignment Document / Brief URL (Optional)
              </label>
              <input
                type="url"
                value={createForm.attachmentUrl || ''}
                onChange={(e) => setCreateForm({ ...createForm, attachmentUrl: e.target.value })}
                placeholder="https://institutional-cloud/briefs/lab4.pdf"
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <input
                type="checkbox"
                id="allowLateSubmissions"
                checked={createForm.allowLateSubmission}
                onChange={(e) => setCreateForm({ ...createForm, allowLateSubmission: e.target.checked })}
              />
              <label htmlFor="allowLateSubmissions" style={{ fontSize: '0.85rem', color: 'var(--text-color)' }}>
                Allow late submissions after due date (will be stamped as Late)
              </label>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={creatingAssignment}
                className="btn btn-primary"
              >
                {creatingAssignment ? 'Publishing...' : 'Publish Assignment'}
              </button>
            </div>
          </form>
        </Modal>
      )}

      {/* MODAL 3: Faculty View Submissions List */}
      {selectedAssignmentForGrading && (
        <Modal
          title={`Submissions: ${selectedAssignmentForGrading.title} (Max: ${selectedAssignmentForGrading.maxMarks} pts)`}
          isOpen={true}
          onClose={() => setSelectedAssignmentForGrading(null)}
        >
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', minWidth: '550px' }}>
            {loadingSubmissions ? (
              <LoadingSpinner message="Loading submissions..." />
            ) : submissionsList.length === 0 ? (
              <p style={{ color: 'var(--text-muted)', textAlign: 'center', padding: '1.5rem 0' }}>
                No submissions received for this assignment yet.
              </p>
            ) : (
              <div style={{ overflowX: 'auto' }}>
                <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                  <thead>
                    <tr style={{ borderBottom: '2px solid var(--border-color)', textAlign: 'left' }}>
                      <th style={{ padding: '0.5rem' }}>Roll No</th>
                      <th style={{ padding: '0.5rem' }}>Student Name</th>
                      <th style={{ padding: '0.5rem' }}>Submission Date</th>
                      <th style={{ padding: '0.5rem' }}>Content / URL</th>
                      <th style={{ padding: '0.5rem' }}>Marks Awarded</th>
                      <th style={{ padding: '0.5rem', textAlign: 'right' }}>Action</th>
                    </tr>
                  </thead>
                  <tbody>
                    {submissionsList.map((s) => (
                      <tr key={s.id} style={{ borderBottom: '1px solid var(--border-color)' }}>
                        <td style={{ padding: '0.5rem', fontWeight: 600 }}>{s.studentRollNumber}</td>
                        <td style={{ padding: '0.5rem' }}>{s.studentName}</td>
                        <td style={{ padding: '0.5rem' }}>
                          <div>{new Date(s.submittedAt).toLocaleDateString()}</div>
                          {s.isLate && <Badge variant="warning">Late</Badge>}
                        </td>
                        <td style={{ padding: '0.5rem' }}>
                          {s.submissionText && (
                            <div style={{ maxWidth: '180px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                              {s.submissionText}
                            </div>
                          )}
                          {s.attachmentUrl && (
                            <a
                              href={s.attachmentUrl}
                              target="_blank"
                              rel="noreferrer"
                              style={{ color: 'var(--primary-color)', fontSize: '0.75rem', textDecoration: 'underline' }}
                            >
                              Attachment Link
                            </a>
                          )}
                        </td>
                        <td style={{ padding: '0.5rem' }}>
                          {s.marksObtained !== null && s.marksObtained !== undefined ? (
                            <span style={{ fontWeight: 600, color: '#166534' }}>
                              {s.marksObtained} / {selectedAssignmentForGrading.maxMarks}
                            </span>
                          ) : (
                            <span style={{ color: 'var(--text-muted)' }}>Not Graded</span>
                          )}
                        </td>
                        <td style={{ padding: '0.5rem', textAlign: 'right' }}>
                          <button
                            onClick={() => handleOpenGradeModal(s)}
                            className="btn btn-secondary"
                            style={{ fontSize: '0.75rem', padding: '0.2rem 0.5rem' }}
                          >
                            Grade
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
                onClick={() => setSelectedAssignmentForGrading(null)}
                className="btn btn-secondary"
              >
                Close
              </button>
            </div>
          </div>
        </Modal>
      )}

      {/* MODAL 4: Faculty Grade Submission Input */}
      {gradingSubmissionId && (
        <Modal
          title="Grade Student Submission"
          isOpen={true}
          onClose={() => setGradingSubmissionId(null)}
        >
          <form onSubmit={handleSaveGrade} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Marks Awarded (Max: {selectedAssignmentForGrading?.maxMarks || 20})
              </label>
              <input
                type="number"
                step="0.5"
                min="0"
                max={selectedAssignmentForGrading?.maxMarks || 100}
                required
                value={gradeMarks}
                onChange={(e) => setGradeMarks(Number(e.target.value))}
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div>
              <label style={{ display: 'block', fontSize: '0.85rem', fontWeight: 600, marginBottom: '0.25rem' }}>
                Evaluation Feedback &amp; Remarks
              </label>
              <textarea
                rows={3}
                value={gradeFeedback}
                onChange={(e) => setGradeFeedback(e.target.value)}
                placeholder="Specific comments on code design, test coverage, or areas for improvement..."
                style={{ width: '100%', padding: '0.5rem', borderRadius: '4px', border: '1px solid var(--border-color)', fontSize: '0.85rem' }}
              />
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.5rem', marginTop: '1rem' }}>
              <button
                type="button"
                onClick={() => setGradingSubmissionId(null)}
                className="btn btn-secondary"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={savingGrade}
                className="btn btn-primary"
              >
                {savingGrade ? 'Saving Grade...' : 'Save Evaluation'}
              </button>
            </div>
          </form>
        </Modal>
      )}
    </div>
  );
};
