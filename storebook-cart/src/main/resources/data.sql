INSERT INTO customer (id, name) VALUES (1, 'John Doe');
INSERT INTO cart (id, customer_id) VALUES (1, 1);
INSERT INTO book (id, title, price, cart_id) VALUES (1, 'Spring Boot Basics', 19.90, 1)