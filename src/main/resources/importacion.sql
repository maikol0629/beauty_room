-- Insertar en StylistRoom
INSERT INTO stylist_room (name_room, address) VALUES ('Room A', '123 Main Street');
INSERT INTO stylist_room (name_room, address) VALUES ('Room B', '456 Elm Street');

-- Insertar en Stylist
INSERT INTO stylist (name_stylist, email, phone, id_stylist_room) VALUES ('John Doe', 'john@example.com', 123456789, 1);
INSERT INTO stylist (name_stylist, email, phone, id_stylist_room) VALUES ('Jane Smith', 'jane@example.com', 987654321, 2);

-- Insertar en Client
INSERT INTO client (name_client, email, phone) VALUES ('Alice Johnson', 'alice@example.com', 5551234);
INSERT INTO client (name_client, email, phone) VALUES ('Bob Brown', 'bob@example.com', 5555678);

-- Insertar en Service
INSERT INTO service (name_service, description, price, duration, id_stylist) VALUES ('Haircut', 'Basic haircut', 15.99, 30, 1);
INSERT INTO service (name_service, description, price, duration, id_stylist) VALUES ('Coloring', 'Hair coloring service', 40.00, 90, 2);

-- Insertar en Appointment
--INSERT INTO appointment (start_date, end_date, id_client, id_stylist, id_service) VALUES ('2025-04-01 10:00:00', '2025-04-01 10:30:00', 1, 1, 1);
--INSERT INTO appointment (start_date, end_date, id_client, id_stylist, id_service) VALUES ('2025-04-02 14:00:00', '2025-04-02 15:30:00', 2, 2, 2);

-- Insertar en StylistScheduleRepository (Horarios de los estilistas)
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time) VALUES (1, 'MONDAY', '09:00:00', '18:00:00');
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time) VALUES (1, 'WEDNESDAY', '10:00:00', '17:00:00');
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time) VALUES (2, 'TUESDAY', '08:30:00', '16:30:00');
INSERT INTO stylist_schedule (stylist_id, day, start_time, end_time) VALUES (2, 'THURSDAY', '12:00:00', '20:00:00');


