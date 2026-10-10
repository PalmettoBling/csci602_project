ALTER TABLE users
    ADD COLUMN owner_account_id BIGINT,
    ADD CONSTRAINT fk_user_owner_account
        FOREIGN KEY (owner_account_id)
        REFERENCES accounts(user_id)
        ON DELETE CASCADE;
