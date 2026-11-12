-- Fixed dates, updated on conflict: Flyway reruns a repeatable script only when its text changes.
INSERT INTO funding_call (id, reference, title_nl, title_fr, opens_at, closes_at,
                          total_budget, committed_amount, max_amount_per_application, version, created_at)
VALUES ('6f1c2b9e-4a3d-4c1e-9b7a-1d2e3f4a5b6c', 'CALL-2026-01',
        'Buurtprojecten 2026', 'Projets de quartier 2026',
        '2026-09-30T22:00:00Z', '2027-12-31T23:00:00Z',
        50000.00, 0.00, 15000.00, 0, now())
ON CONFLICT (id) DO UPDATE SET opens_at = EXCLUDED.opens_at, closes_at = EXCLUDED.closes_at;

INSERT INTO organisation (id, enterprise_number, name, legal_form, created_at)
VALUES ('a3b4c5d6-1e2f-4a5b-8c9d-0e1f2a3b4c5d', '0123456749', 'Buurthuis De Linde', 'VZW_ASBL', now()),
       ('b4c5d6e7-2f3a-4b5c-9d0e-1f2a3b4c5d6e', '0987654394', 'Maison de Quartier Les Tilleuls', 'VZW_ASBL', now())
ON CONFLICT (id) DO NOTHING;
