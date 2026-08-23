-- Remove auto-seeded template practice questions so they regenerate
-- with improved skill-specific grading keywords.
DELETE FROM generated_questions
WHERE explanation = 'Practice question - answer from real experience for best results.';
