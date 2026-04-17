CREATE TABLE IF NOT EXISTS fuel_prices (
    fuel_type VARCHAR(50) PRIMARY KEY,
    price_per_unit DOUBLE PRECISION NOT NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'EUR',
    unit VARCHAR(10) NOT NULL,
    source VARCHAR(100) NOT NULL,
    updated_at VARCHAR(30) NOT NULL
);

-- Default prices (April 2026)
INSERT INTO fuel_prices VALUES ('SP95', 1.85, 'EUR', 'L', 'default', NOW()::text) ON CONFLICT DO NOTHING;
INSERT INTO fuel_prices VALUES ('SP98', 1.96, 'EUR', 'L', 'default', NOW()::text) ON CONFLICT DO NOTHING;
INSERT INTO fuel_prices VALUES ('DIESEL', 2.19, 'EUR', 'L', 'default', NOW()::text) ON CONFLICT DO NOTHING;
INSERT INTO fuel_prices VALUES ('E85', 0.73, 'EUR', 'L', 'default', NOW()::text) ON CONFLICT DO NOTHING;
INSERT INTO fuel_prices VALUES ('GPL', 1.05, 'EUR', 'L', 'default', NOW()::text) ON CONFLICT DO NOTHING;
INSERT INTO fuel_prices VALUES ('ELECTRIC', 0.44, 'EUR', 'kWh', 'default', NOW()::text) ON CONFLICT DO NOTHING;
