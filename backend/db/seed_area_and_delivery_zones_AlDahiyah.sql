-- Seed one area and its delivery zones from the image
-- Red header = area, remaining rows = delivery zones
-- Run from database restaurant_websites:
--   mysql -u root -p restaurant_websites < seed_area_and_delivery_zones.sql
-- Or in MySQL: USE restaurant_websites; then paste this file.
-- 
-- NOTE: You need to set @delivery_company_id below (replace 1 with your actual delivery company ID)
use restaurant_websites;
SET @delivery_company_id = 1; -- CHANGE THIS to your actual delivery_company_id

-- ========== 1. Insert Area (Red Header) ==========
INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
(1, 'Al Dahiyah + Al Mansour + University Street', 'الضاحية + المنصور + ش. الجامعة', CURDATE(), CURDATE());

SET @area_id = LAST_INSERT_ID();

-- ========== 2. Insert Delivery Zones (18 rows) ==========
-- Note: 
--   - Leftmost column (2 or 3) doesn't match any delivery_zones column - may be category/type not in schema (ignored)
--   - Middle column (000 or 500) = price (delivery fee): 000 = 0.00 JOD, 500 = 5.00 JOD
--   - Rightmost column = zone_name_ar (Arabic zone name)
--   - Prices below are set to 0.00 - UPDATE them based on the image (000 = 0.00, 500 = 5.00)

INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id, 'حي المنصور / دورية + رقمية', 'Al Mansour Neighborhood / Patrol + Digital', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'المنصور / المهندسين الملكة عليا', 'Al Mansour / Engineers Queen Alia', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'حي القدس + ابو سويلم مواد بناء', 'Al Quds Neighborhood + Abu Suwailim Building Materials', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار اقرأ + ريتال', 'Iqra Roundabout + Rital', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار المغناطيس + صحي الجبيهة', 'Magnet Roundabout + Al Jubaiha Health Center', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار الكرز + البوابة شمالية', 'Cherry Roundabout + Northern Gate', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'خلف الأردنية + سكن أميمة', 'Behind Al Urduniyah + Umaima Housing', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'سكن جرش + حضرموت', 'Jerash Housing + Hadramout', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'إشارات الدفاع + ماي ماركت', 'Defense Signals + My Market', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ض. الرشيد الحاووز + الجامعة الأردنية', 'Al Rasheed Al Hawawoz Housing + University of Jordan', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الحسين للسرطان + مستشفى الجامعة', 'Al Hussein Cancer Center + University Hospital', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ضاحية الروضة + شارع الجامعة', 'Al Rawdah Suburb + University Street', 3.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'معلومات جنائية + طلوع نيفين', 'Criminal Information + Talou'' Nifin', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'مجدي مول + كازية المناصير', 'Majdi Mall + Al Manaseer Gas Station', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'محكمة الجمرك + شمال عمان', 'Customs Court + North Amman', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'شرطة شمال عمان + هافانا', 'North Amman Police + Havana', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'السابلة + الاوائل + دوار الكوم', 'Al Sablah + Al Awa''el + Al Koum Roundabout', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'مدرسة العلوم التطبيقية', 'Applied Science School', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'منتزه الامير حمزة + اشارات النبعة', 'Prince Hamza Park + Al Nab''ah Signals', 3.00, 'active', CURDATE(), CURDATE());
