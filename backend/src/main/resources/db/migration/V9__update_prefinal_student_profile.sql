-- V9: Update pre-final student profile to Tanesh Khandal with CGPA 7.69
UPDATE users 
SET full_name = 'Tanesh Khandal', 
    cgpa = 7.69 
WHERE LOWER(email) = 'rahul.sharma@jecrcu.edu.in';
