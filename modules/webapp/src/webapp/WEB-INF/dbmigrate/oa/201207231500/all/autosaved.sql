ALTER TABLE contentversion ADD AutoSaved INTEGER;
UPDATE contentversion SET AutoSaved = 0;