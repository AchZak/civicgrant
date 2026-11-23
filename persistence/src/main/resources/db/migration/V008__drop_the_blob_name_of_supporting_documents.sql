-- A document's blob name follows from its two ids, and only the document storage adapter knows the rule.
ALTER TABLE supporting_document DROP CONSTRAINT uq_supporting_document_blob;
ALTER TABLE supporting_document DROP COLUMN blob_name;
