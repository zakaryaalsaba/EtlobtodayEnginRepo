-- Seed: Insert initial regions data
-- Run once: mysql -u root -p restaurant_websites < seed_regions.sql
use restaurant_websites;
-- Insert regions for Amman (city_id = 1)
INSERT INTO regions (city_id, name, name_ar, created_at, updated_at) VALUES
(1, 'Abu Nseir', 'أبو نصير', CURDATE(), CURDATE()),
(1, 'Al Jubeiha', 'الجبيهة', CURDATE(), CURDATE())
ON DUPLICATE KEY UPDATE name = VALUES(name), name_ar = VALUES(name_ar);

INSERT INTO restaurant_websites.areas (region_id, name, name_ar, created_at, updated_at)
values(2,'Abu Nseir + Tab Kra',' أبو نصير + طاب كراع', CURDATE(),CURDATE()),
(2,'Al Dahiyah + Al Mansour + University Street','الضاحية + المنصور + ش. الجامعة', CURDATE(),CURDATE()),
(2,'Shifa Badran + Al Koum + Beyond','شفا بدران + الكوم + ما بعدهم', CURDATE(),CURDATE());

