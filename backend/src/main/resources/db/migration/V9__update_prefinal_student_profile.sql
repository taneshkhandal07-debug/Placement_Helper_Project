-- V9: Update pre-final student profile to Tanesh Khandal with CGPA 7.69, graduation year 2027 and university email tanesh.23bcon1719@jecrcu.edu.in
UPDATE users 
SET full_name = 'Tanesh Khandal', 
    cgpa = 7.69,
    graduation_year = 2027,
    email = 'tanesh.23bcon1719@jecrcu.edu.in'
WHERE LOWER(email) IN ('rahul.sharma@jecrcu.edu.in', 'tanesh.23bcon1719@jecrcu.edu.in');
