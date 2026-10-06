-- Club Registration Portal - MySQL 8 schema (reference)
-- You do NOT need to run this: Spring Boot creates/updates these tables automatically
-- (spring.jpa.hibernate.ddl-auto=update) and creates the database itself (createDatabaseIfNotExist=true).
-- It's here to read the design, draw the ER diagram, or create the database by hand.

CREATE DATABASE IF NOT EXISTS club_portal CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE club_portal;

CREATE TABLE users (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    name          VARCHAR(100) NOT NULL,
    email         VARCHAR(150) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,            -- BCrypt hash, never the plain password
    role          VARCHAR(20)  NOT NULL,            -- STUDENT | COORDINATOR | ADMIN
    department    VARCHAR(100),
    roll_number   VARCHAR(30),
    active        BIT          NOT NULL DEFAULT 1,
    created_at    DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_email UNIQUE (email)
);

CREATE TABLE clubs (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    name              VARCHAR(100)  NOT NULL,
    category          VARCHAR(30)   NOT NULL,       -- TECHNICAL | CULTURAL | SPORTS | LITERARY | ARTS | SOCIAL_SERVICE
    tagline           VARCHAR(150),
    description       VARCHAR(2000) NOT NULL,
    meeting_schedule  VARCHAR(150),
    venue             VARCHAR(150),
    capacity          INT           NOT NULL,       -- max approved members
    registration_open BIT           NOT NULL DEFAULT 1,
    coordinator_id    BIGINT,
    created_at        DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_clubs_name UNIQUE (name),
    CONSTRAINT fk_clubs_coordinator FOREIGN KEY (coordinator_id) REFERENCES users (id)
);

-- One row per student per club; re-applying moves the same row back to PENDING.
CREATE TABLE memberships (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    club_id        BIGINT       NOT NULL,
    status         VARCHAR(20)  NOT NULL,   -- PENDING | APPROVED | REJECTED | WITHDRAWN | LEFT | REMOVED
    motivation     VARCHAR(500),
    remark         VARCHAR(300),            -- coordinator's reason for reject / remove
    applied_at     DATETIME(6)  NOT NULL,
    decided_at     DATETIME(6),
    decided_by_id  BIGINT,
    PRIMARY KEY (id),
    CONSTRAINT uq_memberships_user_club UNIQUE (user_id, club_id),
    CONSTRAINT fk_memberships_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_memberships_club FOREIGN KEY (club_id) REFERENCES clubs (id),
    CONSTRAINT fk_memberships_decided_by FOREIGN KEY (decided_by_id) REFERENCES users (id),
    INDEX idx_memberships_club_status (club_id, status),
    INDEX idx_memberships_user_status (user_id, status)
);

CREATE TABLE events (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    club_id       BIGINT        NOT NULL,
    title         VARCHAR(150)  NOT NULL,
    description   VARCHAR(1000),
    starts_at     DATETIME(6)   NOT NULL,
    ends_at       DATETIME(6)   NOT NULL,
    venue         VARCHAR(150)  NOT NULL,
    capacity      INT           NOT NULL,
    members_only  BIT           NOT NULL,
    cancelled     BIT           NOT NULL,
    created_by_id BIGINT        NOT NULL,
    created_at    DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_events_club FOREIGN KEY (club_id) REFERENCES clubs (id),
    CONSTRAINT fk_events_created_by FOREIGN KEY (created_by_id) REFERENCES users (id),
    INDEX idx_events_starts_at (starts_at),
    INDEX idx_events_club (club_id)
);

-- A student's seat at an event: one per student per event.
CREATE TABLE event_registrations (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    event_id   BIGINT      NOT NULL,
    user_id    BIGINT      NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_event_registrations_event_user UNIQUE (event_id, user_id),
    CONSTRAINT fk_event_registrations_event FOREIGN KEY (event_id) REFERENCES events (id),
    CONSTRAINT fk_event_registrations_user FOREIGN KEY (user_id) REFERENCES users (id)
);
