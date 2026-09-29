CREATE TABLE verification
(
    id         serial PRIMARY KEY,
    code       UUID        NOT NULL DEFAULT gen_random_uuid(),
    created_on TIMESTAMP   NOT NULL DEFAULT now()
);

INSERT INTO verification (code) VALUES (gen_random_uuid());
