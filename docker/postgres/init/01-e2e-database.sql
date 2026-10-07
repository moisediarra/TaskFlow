-- Runs once, when the database volume is first initialised.
-- A separate database for the Playwright end-to-end tests keeps their data out of the main one.
CREATE DATABASE taskflow_e2e OWNER taskflow;
