ALTER TABLE test1
	ADD COLUMN IF NOT EXISTS test2 TEXT DEFAULT '*/',
	ADD COLUMN IF NOT EXISTS test TEXT DEFAULT 'this /*is*/ test',
	ADD COLUMN IF NOT EXISTS test3 TEXT DEFAULT '*/';