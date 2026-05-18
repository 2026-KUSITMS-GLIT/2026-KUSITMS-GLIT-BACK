ALTER TABLE reports
    ADD COLUMN IF NOT EXISTS selected_star_record_ids jsonb;
