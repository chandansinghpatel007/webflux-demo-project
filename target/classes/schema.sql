DROP TABLE IF EXISTS product;

CREATE TABLE product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10, 2) NOT NULL
);

INSERT INTO product (name, price) VALUES ('Mechanical Keyboard', 89.99);
INSERT INTO product (name, price) VALUES ('Wireless Mouse', 29.50);
INSERT INTO product (name, price) VALUES ('4K Monitor', 349.00);
INSERT INTO product (name, price) VALUES ('USB-C Dock', 65.25);
