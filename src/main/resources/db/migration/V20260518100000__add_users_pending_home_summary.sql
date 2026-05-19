ALTER TABLE users
    ADD COLUMN pending_first_star_coach_mark BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN pending_report_modal_type     VARCHAR(10);