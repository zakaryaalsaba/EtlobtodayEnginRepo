-- Seed area "Abu Nseir + Tab Kra" and its delivery zones from the image
-- Red header = area, remaining rows = delivery zones with prices from two cells combined
-- Run from database restaurant_websites:
--   mysql -u root -p restaurant_websites < seed_abu_nseir_area_and_zones.sql
-- Or in MySQL: USE restaurant_websites; then paste this file.
-- 
-- NOTE: You need to set @delivery_company_id below (replace 1 with your actual delivery company ID)
use restaurant_websites;
SET @delivery_company_id = 1; -- CHANGE THIS to your actual delivery_company_id

-- ========== 1. Insert Area (Red Header: "أبو نصير + طاب كراع") ==========
-- INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
-- (1, 'Abu Nseir + Tab Kra', 'أبو نصير + طاب كراع', CURDATE(), CURDATE());

SET @area_id = 2 ;-- LAST_INSERT_ID();

-- ========== 2. Insert Delivery Zones (with prices from two cells combined) ==========
-- Prices: First cell + second cell = combined price (e.g., 1 + 250 = 1.250 JOD)

INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id, 'شارع رئيسي + ملاهي', 'Main Street + Amusements', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'زهور الشفا + مسجد الريان', 'Zuhour Al Shifa + Al Rayyan Mosque', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'جنة الأحلام + السوق التجاري', 'Garden of Dreams + Commercial Market', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ليدرز + حي المحبة', 'Leaders + Al Mahaba Neighborhood', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'حارة 6 + حارة 7', 'District 6 + District 7', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'حارة 1 + حارة 2', 'District 1 + District 2', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'مركز صحي + بن داوود', 'Health Center + Bin Dawood', 1.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'حارة 3 + حارة 4', 'District 3 + District 4', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'حارة 5 + حارة 8', 'District 5 + District 8', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'أجيال العلم + آخر نزول الملاهي', 'Generations of Science + Last Amusement Park Descent', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'مسجد هشام', 'Hisham Mosque', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'ديوان الصمادي', 'Al Samadi Diwan', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار الثقافة + مندرين الثقافة', 'Culture Roundabout + Mandarin Culture', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار التطبيقية + طاب كراع', 'Applied Roundabout + Tab Kraa', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'مدارس الصرح إناث + صحاری', 'Al Sarh Girls Schools + Sahara', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الصرح ذكور + دوار الزعبي', 'Al Sarh Boys + Al Za''abi Roundabout', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الداخون + الكسواني', 'Al Dakhoun + Al Kasawani', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الهنيني + نورما كيك', 'Al Haneeni + Norma Cake', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'حديقة بسمة + امانة ابو نصير', 'Basma Garden + Abu Nseir Trust', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'فقاعات + مخابز النيل', 'Bubbles + Nile Bakeries', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'مسجد الهدى + حي السعادة', 'Al Huda Mosque + Al Saada Neighborhood', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'صيدلية توتال + مخفر ابو نصير', 'Total Pharmacy + Abu Nseir Police Station', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الدفاع المدني + نادي ابو نصير', 'Civil Defense + Abu Nseir Club', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'التنقية + كشك النور', 'Purification + Al Noor Kiosk', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'كشك فرج + كشك رامي', 'Faraj Kiosk + Rami Kiosk', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'نزول المؤسسة + مندرين الشفا', 'Al Mu''assasah Descent + Mandarin Al Shifa', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'هيئة النقل + التدريب المهني', 'Transportation Authority + Vocational Training', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الفاروق', 'Al Farooq', 1.250, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'البحرية + مسجد عيسى', 'Al Bahriyah + Issa Mosque', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'بحر العلوم + مسجد يوسف', 'Sea of Science + Yusuf Mosque', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'الجسد الواحد + اسكانات المرام', 'The One Body + Al Maram Residences', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'اوكسيد + الرغيف الذهبي', 'Oxide + Golden Loaf', 1.750, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id, 'دوار الروابدة + حي الضياء', 'Al Rawabdeh Roundabout + Al Diyaa Neighborhood', 2.000, 'active', CURDATE(), CURDATE());