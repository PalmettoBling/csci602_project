CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE activities (
                            id BIGSERIAL PRIMARY KEY,
                            user_id BIGINT NOT NULL,
                            activity_type VARCHAR(100) NOT NULL,
                            duration_minutes INTEGER NOT NULL,
                            activity_date DATE NOT NULL,

                            CONSTRAINT fk_activity_user
                                FOREIGN KEY (user_id)
                                    REFERENCES users(id)
                                    ON DELETE CASCADE
);