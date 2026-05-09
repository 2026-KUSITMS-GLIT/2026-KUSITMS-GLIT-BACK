ALTER TABLE projects
    ADD CONSTRAINT chk_projects_title_count_non_negative CHECK (title_count >= 0);