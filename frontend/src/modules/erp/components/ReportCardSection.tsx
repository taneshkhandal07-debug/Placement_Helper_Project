import React, { useState, useEffect } from 'react';
import { courseworkApi } from '../api/courseworkApi';
import { SemesterReportCard } from '../types/coursework.types';
import { Badge } from '@/shared/components/Badge';
import { Alert } from '@/shared/components/Alert';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';

interface ReportCardSectionProps {
  semesterId?: number;
}

export const ReportCardSection: React.FC<ReportCardSectionProps> = ({ semesterId }) => {
  const [reportCard, setReportCard] = useState<SemesterReportCard | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [downloading, setDownloading] = useState(false);

  const loadReportCard = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await courseworkApi.getReportCard(semesterId);
      setReportCard(data);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to load official report card.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReportCard();
  }, [semesterId]);

  const handleDownloadPdf = async () => {
    try {
      setDownloading(true);
      const blob = await courseworkApi.downloadReportCardPdf(semesterId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `Report_Card_Semester_${reportCard?.semesterNumber || '7'}_${reportCard?.rollNumber || 'Student'}.pdf`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      setError(err.response?.data?.error || err.message || 'Failed to download report card PDF.');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Header and Download Button */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h2 style={{ fontSize: '1.25rem', fontWeight: 700, margin: 0, color: 'var(--text-color)' }}>
            Official Semester Report Card &amp; Grade Sheet
          </h2>
          <p style={{ margin: '0.25rem 0 0 0', fontSize: '0.875rem', color: 'var(--text-muted)' }}>
            Institutional credit-based semester grading sheet calculated according to approved Indian University 10-point scale standards.
          </p>
        </div>

        {reportCard && (
          <button
            onClick={handleDownloadPdf}
            disabled={downloading}
            className="btn btn-primary"
            style={{ fontSize: '0.85rem' }}
          >
            {downloading ? 'Generating PDF...' : 'Download Official PDF Report Card'}
          </button>
        )}
      </div>

      {error && (
        <Alert type="danger" onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {loading ? (
        <LoadingSpinner message="Calculating semester grade points and compiling official report card..." />
      ) : !reportCard ? (
        <div className="card" style={{ textAlign: 'center', padding: '2.5rem' }}>
          <p style={{ color: 'var(--text-muted)', margin: 0 }}>
            No report card record available for the selected semester.
          </p>
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
          {/* Institutional Report Card Container */}
          <div
            className="card"
            style={{
              padding: '1.75rem',
              border: '2px solid var(--border-color)',
              boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.05)',
            }}
          >
            {/* University & Degree Title */}
            <div style={{ textAlign: 'center', borderBottom: '2px solid var(--border-color)', paddingBottom: '1rem', marginBottom: '1.25rem' }}>
              <h2 style={{ margin: 0, fontSize: '1.25rem', letterSpacing: '0.05em', fontWeight: 800, color: 'var(--primary-color)' }}>
                JECRC UNIVERSITY, JAIPUR
              </h2>
              <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)', fontWeight: 600, marginTop: '0.25rem' }}>
                OFFICIAL SEMESTER GRADE REPORT
              </div>
              <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.15rem' }}>
                Academic Session: {reportCard.academicYear} | Semester {reportCard.semesterNumber}
              </div>
            </div>

            {/* Student Metadata */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))',
                gap: '0.75rem',
                backgroundColor: '#f8fafc',
                padding: '1rem',
                borderRadius: '8px',
                marginBottom: '1.5rem',
                fontSize: '0.85rem',
              }}
            >
              <div>
                <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Student Name:</span>{' '}
                <span style={{ fontWeight: 700 }}>{reportCard.studentName}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Roll Number:</span>{' '}
                <span style={{ fontWeight: 700 }}>{reportCard.rollNumber}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Department:</span>{' '}
                <span style={{ fontWeight: 700 }}>{reportCard.departmentName}</span>
              </div>
              <div>
                <span style={{ color: 'var(--text-muted)', fontWeight: 600 }}>Standing:</span>{' '}
                <span style={{ fontWeight: 700, color: '#166534' }}>{reportCard.academicStanding}</span>
              </div>
            </div>

            {/* SGPA & Credit Performance Summary Highlight */}
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
                gap: '1rem',
                marginBottom: '1.5rem',
              }}
            >
              <div
                style={{
                  padding: '1rem',
                  backgroundColor: '#f0fdf4',
                  border: '1px solid #bbf7d0',
                  borderRadius: '8px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: '#166534', textTransform: 'uppercase' }}>
                  Semester GPA (SGPA)
                </div>
                <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#166534', marginTop: '0.25rem' }}>
                  {reportCard.sgpa.toFixed(2)}
                </div>
                <div style={{ fontSize: '0.75rem', color: '#166534' }}>out of 10.00 scale</div>
              </div>

              <div
                style={{
                  padding: '1rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: '8px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Credits Registered
                </div>
                <div style={{ fontSize: '1.75rem', fontWeight: 800, color: 'var(--text-color)', marginTop: '0.25rem' }}>
                  {reportCard.totalCreditsRegistered}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Approved courses</div>
              </div>

              <div
                style={{
                  padding: '1rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: '8px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Credits Earned
                </div>
                <div style={{ fontSize: '1.75rem', fontWeight: 800, color: '#166534', marginTop: '0.25rem' }}>
                  {reportCard.totalCreditsEarned}
                </div>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Passed requirements</div>
              </div>

              <div
                style={{
                  padding: '1rem',
                  backgroundColor: '#f8fafc',
                  border: '1px solid var(--border-color)',
                  borderRadius: '8px',
                  textAlign: 'center',
                }}
              >
                <div style={{ fontSize: '0.75rem', fontWeight: 600, color: 'var(--text-muted)', textTransform: 'uppercase' }}>
                  Academic Result
                </div>
                <div style={{ fontSize: '1.25rem', fontWeight: 800, color: '#166534', marginTop: '0.5rem' }}>
                  {reportCard.academicStanding}
                </div>
              </div>
            </div>

            {/* Subject Grade Sheet Table */}
            <div style={{ overflowX: 'auto', marginBottom: '1.5rem' }}>
              <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: '0.85rem' }}>
                <thead>
                  <tr style={{ borderBottom: '2px solid var(--border-color)', textAlign: 'left', backgroundColor: '#f8fafc' }}>
                    <th style={{ padding: '0.6rem 0.5rem' }}>Course Code</th>
                    <th style={{ padding: '0.6rem 0.5rem' }}>Subject Name</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Credits</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Max Marks</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Obtained</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Percentage</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Grade</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Grade Point</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>Credit Points</th>
                    <th style={{ padding: '0.6rem 0.5rem', textAlign: 'right' }}>Status</th>
                  </tr>
                </thead>
                <tbody>
                  {reportCard.subjectResults.map((sr) => (
                    <tr key={sr.subjectId} style={{ borderBottom: '1px solid var(--border-color)' }}>
                      <td style={{ padding: '0.6rem 0.5rem', fontWeight: 700 }}>{sr.subjectCode}</td>
                      <td style={{ padding: '0.6rem 0.5rem' }}>{sr.subjectName}</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>{sr.credits}</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>{sr.totalMaxMarks}</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center', fontWeight: 600 }}>{sr.totalMarksObtained}</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>{sr.percentage.toFixed(1)}%</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center', fontWeight: 700, color: sr.passed ? '#166534' : '#991b1b' }}>
                        {sr.letterGrade}
                      </td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center' }}>{sr.gradePoint}</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'center', fontWeight: 600 }}>{sr.creditPoints}</td>
                      <td style={{ padding: '0.6rem 0.5rem', textAlign: 'right' }}>
                        {sr.passed ? <Badge variant="success">PASS</Badge> : <Badge variant="danger">FAIL</Badge>}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* University 10-point scale reference legend */}
            <div style={{ backgroundColor: '#f8fafc', padding: '0.75rem 1rem', borderRadius: '6px', fontSize: '0.75rem', color: 'var(--text-muted)' }}>
              <strong>Approved 10-Point Indian University Grading System:</strong>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.75rem', marginTop: '0.35rem' }}>
                <span>O: 90-100% (10 pts)</span>
                <span>A+: 80-89% (9 pts)</span>
                <span>A: 70-79% (8 pts)</span>
                <span>B+: 60-69% (7 pts)</span>
                <span>B: 50-59% (6 pts)</span>
                <span>C: 40-49% (5 pts)</span>
                <span>F: Below 40% (0 pts - Re-appear)</span>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
