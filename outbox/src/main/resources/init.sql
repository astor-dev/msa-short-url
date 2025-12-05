CREATE TABLE IF NOT EXISTS event_publication
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    bindingName  VARCHAR(512)           NOT NULL,
    message      VARCHAR(4000)          NOT NULL,
    created_at   TIMESTAMP              NOT NULL,
    published_at TIMESTAMP DEFAULT NULL NULL,
    INDEX event_publication_by_published_at_idx (published_at),
    INDEX event_publication_by_published_at_created_at_idx (published_at, created_at)
);
