ALTER TABLE activities
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN weather_local_time TIMESTAMP,
    ADD COLUMN weather_utc_offset VARCHAR(9),
    ADD COLUMN sampled_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN temperature_c DOUBLE PRECISION,
    ADD COLUMN precipitation_mm DOUBLE PRECISION,
    ADD COLUMN wind_speed_kmh DOUBLE PRECISION,
    ADD COLUMN weather_code INTEGER,
    ADD COLUMN fetched_at TIMESTAMP WITH TIME ZONE;
