-- Seed area "Shifa Badran + Al Koum + Beyond" and its delivery zones from the image
-- Red header = area, remaining rows = delivery zones with prices from two cells combined
-- Run from database restaurant_websites:
--   mysql -u root -p restaurant_websites < seed_shifa_badran_area_and_zones.sql
-- Or in MySQL: USE restaurant_websites; then paste this file.
-- 
-- NOTE: You need to set @delivery_company_id below (replace 1 with your actual delivery company ID)
use restaurant_websites;
SET @delivery_company_id = 1; -- CHANGE THIS to your actual delivery_company_id

-- ========== 1. Insert Area (Red Header: "شفا بدران + الكوم + ما بعدهم") ==========
-- INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
-- (1, 'Shifa Badran + Al Koum + Beyond', 'شفا بدران + الكوم + ما بعدهم', CURDATE(), CURDATE());

SET @area_id = 4; -- LAST_INSERT_ID();

-- ========== 2. Insert Delivery Zones (with prices from two cells combined) ==========
-- Prices: First cell * 1000 + second cell = total price (e.g., 2 + 500 = 2.500 JOD, 4 + 000 = 4.000 JOD)
-- Note: Row with "لوكيشن" (Location) is skipped as it's a label/separator

INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id, 'مدارس الشريف - الامم - ريماس - المبادئ)', 'Al Sharif Schools - Nations - Reemas - Principles', 2.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'سلالة مول + اشارات الكوم + مرج الفرس', 'Sulalah Mall + Al Koum Signals + Marj Al Furs', 2.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'قيادة الجيش العربي', 'Arab Army Command', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'منظمة اللاجئين السوريين', 'Syrian Refugee Organization', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الكمشة + ام المكمان', 'Al Kamshah + Umm Al Makman', 5.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'صروت + العالوك + المصطبة', 'Sarwat + Al Aalook + Al Mastabah', 3.500, 'inactive', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ام العروق + رجم الشوك', 'Umm Al Urooq + Rajm Al Shouk', 3.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ام رمانة ماجدة + السيف والدلة', 'Umm Rummanah Majida + Al Saif wal Dallah', 3.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'بلدية بيرين', 'Birain Municipality', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دفاع مدني بيرين', 'Birain Civil Defense', 5.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الفردوس + ابن باجة + الاتفاق', 'Al Firdous + Ibn Baja + Al Ittifaq', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الاتصالات + ام حجير + شاورما ميكر', 'Telecommunications + Umm Hajir + Shawarma Maker', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'جوبترول + ابو سويلم سامح مول', 'Jopetrol + Abu Suwailim Samih Mall', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الكلحة + التطبيقية + ام طفيل', 'Al Kalhah + Applied + Umm Tufeil', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الماك + برشلونة + صباح الزين', 'Al Maak + Barcelona + Sabah Al Zain', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'التطبيقات الذكية + المعالم + الدانا', 'Smart Applications + Landmarks + Al Dana', 1.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ملاعب التطبيقية + ليفانت', 'Applied Playgrounds + Levant', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'اقليم الوسط + مالية الجيش', 'Central Region + Army Finance', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'اليوبيل + الاسكان العسكري', 'Al Yubail + Military Housing', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'اشارات الشفا + الغذاء و الدواء + البابا + الدرك + مخفر الشفا', 'Al Shifa Signals + Food and Drug + Al Baba + Gendarmerie + Al Shifa Police Station', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ملاعب الشفا + دوار الحجاج + حبوب', 'Al Shifa Playgrounds + Hajjaj Roundabout + Huboub', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار العنيزات + مدرسة البنفسج', 'Al Unaizat Roundabout + Al Banafsaj School', 2.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'صحي الشفا + عيون الذيب', 'Al Shifa Health + Uyoun Al Dheeb', 3.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'السويلميين + مقبرة الشفا + الحوتري', 'Al Suwailimiyeen + Al Shifa Cemetery + Al Hawtari', 2.20, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الحديقة النباتية + سمير شما + كلية عمان + الطاقة الذرية + مقبرة شمال عمان', 'Botanical Garden + Samir Shamma + Amman College + Atomic Energy + North Amman Cemetery', 3.50, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار الترخيص + مسجد اسيا + ميراس الترخيص + اسكان العدل + الكوم الغربي', 'Licensing Roundabout + Asia Mosque + Miras Licensing + Justice Housing + West Al Koum', 3.00, 'active', CURDATE(), CURDATE());
