-- Seed 4 areas and their delivery zones from the image
-- Each red header = one area, remaining rows = delivery zones with prices from two cells combined
-- Run from database restaurant_websites:
--   mysql -u root -p restaurant_websites < seed_four_areas_and_zones.sql
-- Or in MySQL: USE restaurant_websites; then paste this file.
-- 
-- NOTE: You need to set @delivery_company_id below (replace 1 with your actual delivery company ID)
use restaurant_websites;
SET @delivery_company_id = 1; -- CHANGE THIS to your actual delivery_company_id

-- ========== 1. Insert Area 1: Al Jubeiha (الجبيهة) ==========
INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
(1, 'Al Jubeiha', 'الجبيهة', CURDATE(), CURDATE());

SET @area_id_1 = LAST_INSERT_ID();

-- Delivery Zones for Al Jubeiha
INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id_1, 'أم زويتينة + الرواد', 'Umm Zuwaytinah + Al Ruwad', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'جبران + ليمار', 'Jibran + Leimar', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'الفريد + اشارة ام زويتينة', 'Al Farid + Umm Zuwaytinah Signal', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'الاحصاءات+التعليم العالي', 'Statistics + Higher Education', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'شارع البلدية + خلف السيفوي', 'Municipality Street + Behind Al Saifawi', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'اشارة الجبيهة + اشارة المنهل', 'Al Jubeiha Signal + Al Manhal Signal', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'دوار بسمة + الملحقية + اللوزي', 'Basma Roundabout + Al Mulhaqiah + Al Lawzi', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'الابداع + النور + حي الريان', 'Al Ibdaa + Al Noor + Al Rayyan Neighborhood', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_1, 'متصرفية الجبيهة + مسجد زمزم + حي المخابرات', 'Al Jubeiha Governorate Building + Zamzam Mosque + Al Mukhabarat Neighborhood', 1.500, 'active', CURDATE(), CURDATE());

-- ========== 2. Insert Area 2: East Amman (عمان الشرقية) ==========
INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
(1, 'East Amman', 'عمان الشرقية', CURDATE(), CURDATE());

SET @area_id_2 = LAST_INSERT_ID();

-- Delivery Zones for East Amman
INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id_2, 'حي الخرابشة + الخزنة', 'Al Kharabsha Neighborhood + Al Khazna', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'حراج طبربور + مجمع الشمال', 'Tabarbour Market + North Complex', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'المختار مول + المدينة الرياضية', 'Al Mukhtar Mall + Sports City', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'عرجان + مستشفى الرويال', 'Arjan + Royal Hospital', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'جبل الحسين + دوار الداخلية', 'Jabal Al Hussein + Interior Roundabout', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'ض. الأمير حسن + حي النزهة', 'Prince Hassan St. + Al Nuzha Neighborhood', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'الشميساني + العبدلي + الويبدة + التخصصي', 'Al Shmeisani + Al Abdali + Al Weibdeh + Al Takhasusi', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'طبربور + المشاغل + الدبابة + دوار النخيل + الهاشمي الشمالي', 'Tabarbour + Al Mashaghel + Al Dababa + Al Nakheel Roundabout + North Al Hashemi', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'عين غزال + الهاشمي الجنوبي', 'Ain Ghazal + South Al Hashemi', 5.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_2, 'ماركا الجنوبية + الشمالية', 'South Marka + North Marka', 6.000, 'active', CURDATE(), CURDATE());

-- ========== 3. Insert Area 3: Sweileh + West Amman (صويلح + عمان الغربية) ==========
INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
(1, 'Sweileh + West Amman', 'صويلح + عمان الغربية', CURDATE(), CURDATE());

SET @area_id_3 = LAST_INSERT_ID();

-- Delivery Zones for Sweileh + West Amman
INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id_3, 'الحي الشرقي + الدوريات', 'East Neighborhood + Patrols', 2.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'دوار صويلح + حي الفضيلة', 'Sweileh Roundabout + Al Fadhila Neighborhood', 2.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'حي الإرسال + طلوع الذهب + الرحمانية', 'Al Irsaal Neighborhood + Taloo Al Dahab + Al Rahmaniya', 3.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'مسبح تولين + بناة الغد', 'Toleen Pool + Benaat Al Ghad', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'مقام ابن عوف + الإمام الغزالي', 'Maqam Ibn Awf + Imam Al Ghazali', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'خلدا + تلاع العلي + الجاردنز +ش. المدينة', 'Khalda + Talaa Al Ali + Al Gardens + City Street', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'المدينة الطبية + دابوق + ش. مكة', 'Medical City + Dabouq + Makkah Street', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'الرابية + ام السماق + أم أذينة', 'Al Rabiya + Umm Al Samaq + Umm Adaina', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'عبدون + دير غبار + السابع + الثامن + صويفية + جبل عمان', 'Abdoun + Deir Ghbar + Seventh + Eighth + Sweifieh + Jabal Amman', 5.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'تقسيم + قصر الجبل + رويال فيو', 'Taqseem + Jabal Palace + Royal View', 3.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'ديوان زمان + الصرح 2', 'Diwan Zaman + Al Sarh 2', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'الكنيسة + مستشفى الرشيد', 'The Church + Al Rashid Hospital', 1.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'نزول القرية + نزول مستشفى الرشيد', 'Nazool Al Qariya + Nazool Al Rashid Hospital', 2.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'نبعة القرية + قرية ابو نصير', 'Nabaa Al Qariya + Abu Nseir Village', 2.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_3, 'سوق الحلال + الدرباني + هنجر شهوان + ديوان الوريكات', 'Al Halal Market + Al Darbani + Hangar Shahwan + Diwan Al Wureikat', 3.000, 'active', CURDATE(), CURDATE());

-- ========== 4. Insert Area 4: Al Baqa''a + Ain Al Basha + Sharea Al Ordon (البقعة + عين الباشا + شارع الأردن) ==========
INSERT INTO areas (city_id, name, name_ar, created_at, updated_at) VALUES
(1, 'Al Baqa''a + Ain Al Basha + Sharea Al Ordon', 'البقعة + عين الباشا + شارع الأردن', CURDATE(), CURDATE());

SET @area_id_4 = LAST_INSERT_ID();

-- Delivery Zones for Al Baqa'a + Ain Al Basha + Sharea Al Ordon
INSERT INTO delivery_zones (delivery_company_id, area_id, zone_name_ar, zone_name_en, price, status, created_at, updated_at) VALUES
(@delivery_company_id, @area_id_4, 'الفيصلية + مخيم البقعة القديم', 'Al Faisaliyah + Old Baqa''a Camp', 3.00, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'اتصالات البقعة + دوار عين الباشا', 'Al Baqa''a Telecom + Ain Al Basha Roundabout', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'التطوير الحضري + الفروسية', 'Urban Development + Al Furousiya', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'مثلث المدارس + كنيسة صافوط', 'Schools Triangle + Safout Church', 3.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'الكسارات + عمان الاهلية', 'Al Kasarat + Amman Ahlia', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'الحنو', 'Al Hanu', 5.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'مستشفى الامير حسين + الشويخ الغربي', 'Prince Hussein Hospital + West Shweikh', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'سوق البقعة + مسجد غزة هاشم', 'Al Baqa''a Market + Gaza Hashem Mosque', 3.500, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'ام الدنانير + اشارة الصحية', 'Umm Al Dananir + Health Signal', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'حي الامير ماجد + المضمار', 'Prince Majed Neighborhood + Al Midmar', 4.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'الجامعة العربية + اكاديمية التميز', 'Arab University + Al Tamyeez Academy', 5.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'مرصع + قرية حامد', 'Mursaa + Hamed Village', 5.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'مرصع الشرقي + الغربي', 'East Mursaa + West Mursaa', 7.000, 'active', CURDATE(), CURDATE()),
(@delivery_company_id, @area_id_4, 'سلحوب + رميمين', 'Salhoub + Rumeimin', 3.000, 'active', CURDATE(), CURDATE());
