-- Konta testowe (hasła BCrypt). Tylko do developmentu i demo, opisane w README.
--   client@rentengine.local   / Client123!
--   client2@rentengine.local  / Client123!
--   employee@rentengine.local / Employee123!
--   admin@rentengine.local    / Admin123!

INSERT INTO users (email, password_hash, first_name, last_name, role)
VALUES ('client@rentengine.local', '$2a$10$m/UOOADsOEZ3Za.jSGGRGODWoAGtQ8n7BQNGeepKDSs3JcoNjfTRC', 'Jan', 'Kowalski', 'CLIENT'),
       ('client2@rentengine.local', '$2a$10$2LwgUWSbvD5x0xYZQTlcxuGmc9Pxv0IYIjpRU4Sa00omsLvLJTkaW', 'Anna', 'Nowak', 'CLIENT'),
       ('employee@rentengine.local', '$2a$10$Hyq6wOfxxHyLgCmBrGTmjummVTpnnTbk5BERWKDV8uJaJX0RRPGCq', 'Piotr', 'Wiśniewski', 'EMPLOYEE'),
       ('admin@rentengine.local', '$2a$10$un94ZI.5MlFReJ4ht.OPqeJWdUn5msqhEnW.hIk.wN4H1eWB8VK1u', 'Admin', 'RentEngine', 'ADMIN');
