-- JPA 엔티티가 아니라 ddl-auto가 못 만든다. 네이티브 SQL(DocumentNumberGenerator) 전용.
CREATE TABLE IF NOT EXISTS document_number_seq (
    "year"   INT PRIMARY KEY,
    last_no  BIGINT NOT NULL
);
