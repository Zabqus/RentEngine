-- Dane testowe katalogu. Użytkownicy dojdą w Dniu 5 (hasła BCrypt).

INSERT INTO categories (name, type, required_params)
VALUES ('Samochody osobowe', 'CAR', '["powerHp", "gearbox", "seats"]'),
       ('Minikoparki', 'EXCAVATOR', '["weightKg", "digDepthM", "powerKw"]'),
       ('Kosiarki samojezdne', 'MOWER', '["cuttingWidthCm", "powerHp"]'),
       ('Traktorki', 'TRACTOR', '["powerHp", "drive"]'),
       ('Maszyny budowlane', 'CONSTRUCTION', '["weightKg", "powerKw"]');

INSERT INTO equipment (name, category_id, daily_price, deposit, tank_capacity, params)
VALUES ('Toyota Corolla 1.8 Hybrid', (SELECT id FROM categories WHERE type = 'CAR'), 189.00, 1000.00, 43.00,
        '{"powerHp": 140, "gearbox": "AUTOMATIC", "seats": 5}'),
       ('Skoda Octavia Combi 2.0 TDI', (SELECT id FROM categories WHERE type = 'CAR'), 219.00, 1500.00, 45.00,
        '{"powerHp": 150, "gearbox": "MANUAL", "seats": 5}'),
       ('Kubota U17-3', (SELECT id FROM categories WHERE type = 'EXCAVATOR'), 450.00, 3000.00, 19.00,
        '{"weightKg": 1700, "digDepthM": 2.3, "powerKw": 12.6}'),
       ('Bobcat E35z', (SELECT id FROM categories WHERE type = 'EXCAVATOR'), 590.00, 5000.00, 39.00,
        '{"weightKg": 3500, "digDepthM": 3.1, "powerKw": 18.2}'),
       ('Husqvarna TC 242TX', (SELECT id FROM categories WHERE type = 'MOWER'), 160.00, 800.00, 8.50,
        '{"cuttingWidthCm": 108, "powerHp": 18}'),
       ('John Deere X350', (SELECT id FROM categories WHERE type = 'TRACTOR'), 240.00, 1200.00, 9.50,
        '{"powerHp": 22, "drive": "2WD"}'),
       ('Zagęszczarka Wacker Neuson DPU 2560', (SELECT id FROM categories WHERE type = 'CONSTRUCTION'), 140.00, 700.00, 3.00,
        '{"weightKg": 160, "powerKw": 3.4}'),
       ('Młot wyburzeniowy Makita HM1812', (SELECT id FROM categories WHERE type = 'CONSTRUCTION'), 90.00, 500.00, NULL,
        '{"weightKg": 32, "powerKw": 2.0}');

INSERT INTO equipment_units (equipment_id, serial_or_plate, status, mileage_or_hours)
VALUES ((SELECT id FROM equipment WHERE name = 'Toyota Corolla 1.8 Hybrid'), 'WE 1234A', 'AVAILABLE', 15230),
       ((SELECT id FROM equipment WHERE name = 'Toyota Corolla 1.8 Hybrid'), 'WE 5678B', 'AVAILABLE', 42810),
       ((SELECT id FROM equipment WHERE name = 'Skoda Octavia Combi 2.0 TDI'), 'WE 9012C', 'AVAILABLE', 68400),
       ((SELECT id FROM equipment WHERE name = 'Kubota U17-3'), 'KUB-U17-0001', 'AVAILABLE', 820),
       ((SELECT id FROM equipment WHERE name = 'Kubota U17-3'), 'KUB-U17-0002', 'IN_SERVICE', 1460),
       ((SELECT id FROM equipment WHERE name = 'Bobcat E35z'), 'BOB-E35-0001', 'AVAILABLE', 540),
       ((SELECT id FROM equipment WHERE name = 'Husqvarna TC 242TX'), 'HUS-TC242-0001', 'AVAILABLE', 210),
       ((SELECT id FROM equipment WHERE name = 'Husqvarna TC 242TX'), 'HUS-TC242-0002', 'AVAILABLE', 95),
       ((SELECT id FROM equipment WHERE name = 'John Deere X350'), 'JD-X350-0001', 'AVAILABLE', 330),
       ((SELECT id FROM equipment WHERE name = 'Zagęszczarka Wacker Neuson DPU 2560'), 'WN-DPU-0001', 'AVAILABLE', 150),
       ((SELECT id FROM equipment WHERE name = 'Młot wyburzeniowy Makita HM1812'), 'MAK-HM18-0001', 'AVAILABLE', 0),
       ((SELECT id FROM equipment WHERE name = 'Młot wyburzeniowy Makita HM1812'), 'MAK-HM18-0002', 'AVAILABLE', 0);
