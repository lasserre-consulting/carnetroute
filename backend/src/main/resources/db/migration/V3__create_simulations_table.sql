CREATE TABLE IF NOT EXISTS simulations (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36),
    vehicle_id VARCHAR(36),
    from_lat DOUBLE PRECISION NOT NULL,
    from_lng DOUBLE PRECISION NOT NULL,
    from_label VARCHAR(500) NOT NULL DEFAULT '',
    to_lat DOUBLE PRECISION NOT NULL,
    to_lng DOUBLE PRECISION NOT NULL,
    to_label VARCHAR(500) NOT NULL DEFAULT '',
    distance_km DOUBLE PRECISION NOT NULL,
    duration_minutes DOUBLE PRECISION NOT NULL,
    geometry TEXT,
    traffic_mode VARCHAR(20) NOT NULL DEFAULT 'manual',
    traffic_factor DOUBLE PRECISION NOT NULL DEFAULT 1.0,
    fuel_type VARCHAR(50) NOT NULL,
    price_per_unit DOUBLE PRECISION NOT NULL,
    consumption_per_100km DOUBLE PRECISION NOT NULL,
    fuel_consumed_total DOUBLE PRECISION NOT NULL,
    cost_total DOUBLE PRECISION NOT NULL,
    duration_adjusted_minutes DOUBLE PRECISION NOT NULL,
    comparison_json TEXT NOT NULL DEFAULT '{}',
    created_at VARCHAR(30) NOT NULL
);
CREATE INDEX idx_simulations_user_id ON simulations(user_id);
CREATE INDEX idx_simulations_created_at ON simulations(created_at);
