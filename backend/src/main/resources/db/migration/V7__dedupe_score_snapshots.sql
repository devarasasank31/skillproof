-- Older versions appended a score snapshot on every dashboard view, so the demo user
-- accumulated hundreds of identical rows per skill. Recalculation is now throttled and only
-- records a snapshot when a score actually changes; this removes the historical duplicates
-- and keeps trend charts meaningful (one point per real change).

DELETE FROM skill_scores s
WHERE s.id NOT IN (
    SELECT DISTINCT ON (user_skill_id) id
    FROM skill_scores
    ORDER BY user_skill_id, snapshot_at DESC, id DESC
);