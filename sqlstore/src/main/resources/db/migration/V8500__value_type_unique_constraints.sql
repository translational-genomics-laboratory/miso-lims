ALTER TABLE Indices ADD CONSTRAINT uk_index UNIQUE (indexFamilyId, position, sequence);
ALTER TABLE LibrarySelectionType ADD CONSTRAINT uk_librarySelectionType_name UNIQUE (name);
ALTER TABLE LibraryStrategyType ADD CONSTRAINT uk_libraryStrategyType_name UNIQUE (name);
ALTER TABLE LibraryType ADD CONSTRAINT uk_libraryType_byPlatform UNIQUE (platformType, description);
ALTER TABLE LibraryDesign MODIFY name varchar(255) NOT NULL;
ALTER TABLE LibraryDesign ADD CONSTRAINT uk_libraryDesign_name UNIQUE (name);
ALTER TABLE QCType ADD CONSTRAINT uk_qcType_byTarget UNIQUE (qcTarget, name);
ALTER TABLE SequencerReference ADD CONSTRAINT uk_sequencer_name UNIQUE (name);
--StartNoTest
DROP INDEX UKd5487vldy3xo0x7iw6vmspvpf ON DetailedQcStatus;
DROP INDEX UKdewdpl9hfwp6plc9gln8rtcx5 ON SampleClass;
--EndNoTest
ALTER TABLE DetailedQcStatus ADD CONSTRAINT uk_detailedQcStatus_description UNIQUE (description);
ALTER TABLE SampleClass ADD CONSTRAINT uk_sampleClass_alias UNIQUE (alias);