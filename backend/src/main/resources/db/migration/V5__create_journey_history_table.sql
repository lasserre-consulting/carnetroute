CREATE TABLE IF NOT EXISTS journey_history (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    simulation_id VARCHAR(36) NOT NULL,
    fuel_type VARCHAR(50) NOT NULL,
    distance_km DOUBLE PRECISION NOT NULL,
    cost_total DOUBLE PRECISION NOT NULL,
    duration_minutes DOUBLE PRECISION NOT NULL,
    carbon_emission_kg DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    tags TEXT NOT NULL DEFAULT '[]',
    created_at VARCHAR(30) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_journey_history_user_id ON journey_history(user_id);
CREATE INDEX idx_journey_history_created_at ON journey_history(created_at);
