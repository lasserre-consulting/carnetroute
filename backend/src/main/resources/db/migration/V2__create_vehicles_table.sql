CREATE TABLE IF NOT EXISTS vehicles (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    fuel_type VARCHAR(50) NOT NULL,
    consumption_per_100km DOUBLE PRECISION NOT NULL,
    cost_per_unit DOUBLE PRECISION NOT NULL,
    tank_capacity DOUBLE PRECISION NOT NULL DEFAULT 50.0,
    emissions_gco2_per_km DOUBLE PRECISION NOT NULL DEFAULT 120.0,
    year_make INTEGER NOT NULL DEFAULT 2020,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at VARCHAR(30) NOT NULL,
    updated_at VARCHAR(30) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_vehicles_user_id ON vehicles(user_id);
