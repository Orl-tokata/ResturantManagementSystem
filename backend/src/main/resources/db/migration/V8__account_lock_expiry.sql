-- ---------------------------------------------------------------------------
-- V8 — account locks that expire
--
-- Five wrong passwords already locked an account. What it did not do was ever
-- unlock it: the only way back was a password reset by email, and the message
-- the user saw — "Contact an administrator" — named a person who had no button
-- to press.
--
-- That made a permanent denial of service out of five wrong guesses. Anyone
-- who knows a cashier's username could take that till out of service during
-- dinner, and an administrator could be locked out of their own system with no
-- way back but editing this table by hand.
--
-- locked_until carries the distinction:
--
--   lock_yn = 'Y', locked_until = <a time>  → automatic, expires by itself
--   lock_yn = 'Y', locked_until IS NULL     → set by an administrator, stays
--   lock_yn = 'N'                           → not locked
--
-- The rate limiter is what actually stops brute force: 10 login attempts a
-- minute per caller. A lock that lifts after fifteen minutes still cuts a
-- targeted attacker to roughly twenty guesses an hour, which defeats guessing
-- just as thoroughly as a permanent one and does not leave a till dead.
-- ---------------------------------------------------------------------------

ALTER TABLE users_infm ADD COLUMN locked_until TIMESTAMP;

-- Anything locked before this migration was locked with no way out. Give those
-- accounts the new behaviour rather than leaving them stranded: they are far
-- more likely to be a mistyped password than an attack, and an administrator
-- can lock one again deliberately if it was not.
UPDATE users_infm SET lock_yn = 'N', login_failed_cnt = 0 WHERE lock_yn = 'Y';
