-- Seed area "Abu Nseir + Tab Kra" and its delivery zones from the image
-- Red header = area, remaining rows = delivery zones with prices from two cells combined
-- Run from database restaurant_websites:
--   mysql -u root -p restaurant_websites < seed_abu_nseir_area_and_zones.sql
-- Or in MySQL: USE restaurant_websites; then paste this file.
-- 
-- NOTE: You need to set @delivery_company_id below (replace 1 with your actual delivery company ID)
use restaurant_websites;

INSERT INTO restaurant_websites.delivery_zones
  (delivery_company_id, area_id, zone_name_id, price, status, created_at, updated_at)
VALUES
  (3, 10,  68, 2.000, 'active', CURDATE(), CURDATE()), -- شارع رئيسي + ملاهي
  (3, 10,  69, 2.000, 'active', CURDATE(), CURDATE()), -- زهور الشفا + مسجد الريان
  (3, 10,  70, 2.000, 'active', CURDATE(), CURDATE()), -- جنة الأحلام + السوق التجاري
  (3, 10,  71, 2.500, 'active', CURDATE(), CURDATE()), -- ليدرز + حي المحبة
  (3, 10,  72, 2.000, 'active', CURDATE(), CURDATE()), -- حارة 6 + حارة 7
  (3, 10,  73, 2.000, 'active', CURDATE(), CURDATE()), -- حارة 1 + حارة 2
  (3, 10,  74, 2.000, 'active', CURDATE(), CURDATE()), -- مركز صحي + بن داوود
  (3, 10,  75, 2.000, 'active', CURDATE(), CURDATE()), -- حارة 3 + حارة 4
  (3, 10,  76, 2.000, 'active', CURDATE(), CURDATE()), -- حارة 5 + حارة 8
  (3, 10,  77, 2.000, 'active', CURDATE(), CURDATE()), -- أجيال العلم + آخر نزول الملاهي
  (3, 10,  78, 2.000, 'active', CURDATE(), CURDATE()), -- مسجد هشام
  (3, 10,  79, 2.000, 'active', CURDATE(), CURDATE()), -- ديوان الصمادي
  (3, 10,  80, 2.000, 'active', CURDATE(), CURDATE()), -- دوار الثقافة + مندرين الثقافة
  (3, 10,  81, 2.000, 'active', CURDATE(), CURDATE()), -- دوار التطبيقية + طاب كراع
  (3, 10,  82, 2.000, 'active', CURDATE(), CURDATE()), -- مدارس الصرح إناث + صحاری
  (3, 10,  83, 2.000, 'active', CURDATE(), CURDATE()), -- الصرح ذكور + دوار الزعبي
  (3, 10,  84, 2.000, 'active', CURDATE(), CURDATE()), -- الداخون + الكسواني
  (3, 10,  85, 2.000, 'active', CURDATE(), CURDATE()), -- الهنيني + نورما كيك
  (3, 10,  86, 2.000, 'active', CURDATE(), CURDATE()), -- حديقة بسمة + امانة ابو نصير
  (3, 10,  87, 2.000, 'active', CURDATE(), CURDATE()), -- فقاعات + مخابز النيل
  (3, 10,  88, 2.000, 'active', CURDATE(), CURDATE()), -- مسجد الهدى + حي السعادة
  (3, 10,  89, 2.000, 'active', CURDATE(), CURDATE()), -- صيدلية توتال + مخفر ابو نصير
  (3, 10,  90, 2.000, 'active', CURDATE(), CURDATE()), -- الدفاع المدني + نادي ابو نصير
  (3, 10,  91, 2.000, 'active', CURDATE(), CURDATE()), -- التنقية + كشك النور
  (3, 10,  92, 2.000, 'active', CURDATE(), CURDATE()), -- كشك فرج + كشك رامي
  (3, 10,  93, 2.000, 'active', CURDATE(), CURDATE()), -- نزول المؤسسة + مندرين الشفا
  (3, 10,  94, 2.000, 'active', CURDATE(), CURDATE()), -- هيئة النقل + التدريب المهني
  (3, 10,  95, 2.000, 'active', CURDATE(), CURDATE()), -- الفاروق
  (3, 10,  96, 2.500, 'active', CURDATE(), CURDATE()), -- البحرية + مسجد عيسى
  (3, 10,  97, 2.000, 'active', CURDATE(), CURDATE()), -- بحر العلوم + مسجد يوسف
  (3, 10,  98, 2.000, 'active', CURDATE(), CURDATE()), -- الجسد الواحد + اسكانات المرام
  (3, 10,  99, 2.500, 'active', CURDATE(), CURDATE()), -- اوكسيد + الرغيف الذهبي
  (3, 10, 100, 2.500, 'active', CURDATE(), CURDATE());  -- دوار الروابدة + حي الضياء
  
 INSERT INTO restaurant_websites.delivery_zones
  (delivery_company_id, area_id, zone_name_id, price, status, created_at, updated_at)
VALUES
  (3, 11,  1, 2.00, 'active', CURDATE(), CURDATE()), -- حي المنصور / دورية + رقمية
  (3, 11,  2, 2.50, 'active', CURDATE(), CURDATE()), -- المنصور / المهندسين الملكة عليا
  (3, 11,  3, 2.00, 'active', CURDATE(), CURDATE()), -- حي القدس + ابو سويلم مواد بناء
  (3, 11,  4, 2.00, 'active', CURDATE(), CURDATE()), -- دوار اقرأ + ريتال
  (3, 11,  5, 2.00, 'active', CURDATE(), CURDATE()), -- دوار المغناطيس + صحي الجبيهة
  (3, 11,  6, 2.00, 'active', CURDATE(), CURDATE()), -- دوار الكرز + البوابة شمالية
  (3, 11,  7, 2.00, 'active', CURDATE(), CURDATE()), -- خلف الأردنية + سكن أميمة
  (3, 11,  8, 2.00, 'active', CURDATE(), CURDATE()), -- سكن جرش + حضرموت
  (3, 11,  9, 2.50, 'active', CURDATE(), CURDATE()), -- إشارات الدفاع + ماي ماركت
  (3, 11, 10, 2.50, 'active', CURDATE(), CURDATE()), -- ض. الرشيد الحاووز + الجامعة الأردنية
  (3, 11, 11, 2.50, 'active', CURDATE(), CURDATE()), -- الحسين للسرطان + مستشفى الجامعة
  (3, 11, 12, 2.50, 'active', CURDATE(), CURDATE()), -- ضاحية الروضة + شارع الجامعة
  (3, 11, 13, 2.50, 'active', CURDATE(), CURDATE()), -- معلومات جنائية + طلوع نيفين
  (3, 11, 14, 2.00, 'active', CURDATE(), CURDATE()), -- مجدي مول + كازية المناصير
  (3, 11, 15, 1.50, 'active', CURDATE(), CURDATE()), -- محكمة الجمرك + شمال عمان
  (3, 11, 16, 1.50, 'active', CURDATE(), CURDATE()), -- شرطة شمال عمان + هافانا
  (3, 11, 17, 2.50, 'active', CURDATE(), CURDATE()), -- السابلة + الاوائل + دوار الكوم
  (3, 11, 18, 2.50, 'active', CURDATE(), CURDATE()); -- مدرسة العلوم التطبيقية
  
  
  INSERT INTO delivery_zones
  (delivery_company_id, area_id, zone_name_id, price, status, created_at, updated_at)
VALUES
  -- From existing names 101–105 (prices from image / seed)
  (3, 12, 101, 3.000, 'active',   CURDATE(), CURDATE()), -- مدارس الشريف - الامم - ريماس - المبادئ)
  (3, 12, 102, 2.500, 'active',   CURDATE(), CURDATE()), -- سلالة مول + اشارات الكوم + مرج الفرس
  (3, 12, 103, 3.500, 'active',   CURDATE(), CURDATE()), -- قيادة الجيش العربي
  (3, 12, 104, 4.000, 'active',   CURDATE(), CURDATE()), -- منظمة اللاجئين السوريين
  (3, 12, 105, 5.000, 'active',   CURDATE(), CURDATE()), -- الكمشة + ام المكمان

  -- Remaining rows 106–126 (same prices/status as seed_shifa_badran_area_and_zones.sql)
  (3, 12, 106, 3.500, 'inactive', CURDATE(), CURDATE()), -- صروت + العالوك + المصطبة
  
  (3, 12, 107, 4.000, 'active',   CURDATE(), CURDATE()), -- ام العروق + رجم الشوك
  (3, 12, 108, 4.500, 'active',   CURDATE(), CURDATE()), -- ام رمانة ماجدة + السيف والدلة
  (3, 12, 109, 5.000, 'active',   CURDATE(), CURDATE()), -- بلدية بيرين
  
  (3, 12, 110, 6.000, 'active',   CURDATE(), CURDATE()), -- دفاع مدني بيرين
  
  (3, 12, 111, 2.000, 'active',   CURDATE(), CURDATE()), -- الفردوس + ابن باجة + الاتفاق
  (3, 12, 112, 2.000, 'active',   CURDATE(), CURDATE()), -- الاتصالات + ام حجير + شاورما ميكر
  (3, 12, 113, 2.000, 'active',   CURDATE(), CURDATE()), -- جوبترول + ابو سويلم سامح مول
  (3, 12, 114, 2.000, 'active',   CURDATE(), CURDATE()), -- الكلحة + التطبيقية + ام طفيل
  (3, 12, 115, 2.000, 'active',   CURDATE(), CURDATE()), -- الماك + برشلونة + صباح الزين
  (3, 12, 116, 2.000, 'active',   CURDATE(), CURDATE()), -- التطبيقات الذكية + المعالم + الدانا
  (3, 12, 117, 2.000, 'active',   CURDATE(), CURDATE()), -- ملاعب التطبيقية + ليفانت
  (3, 12, 118, 2.000, 'active',   CURDATE(), CURDATE()), -- اقليم الوسط + مالية الجيش
  (3, 12, 119, 2.000, 'active',   CURDATE(), CURDATE()), -- اليوبيل + الاسكان العسكري
  (3, 12, 120, 2.000, 'active',   CURDATE(), CURDATE()), -- اشارات الشفا + الغذاء و الدواء + البابا + الدرك + مخفر الشفا
  
  (3, 12, 121, 2.500, 'active',   CURDATE(), CURDATE()), -- ملاعب الشفا + دوار الحجاج + حبوب
  (3, 12, 122, 3.000, 'active',   CURDATE(), CURDATE()), -- دوار العنيزات + مدرسة البنفسج
  
  (3, 12, 123, 3.500, 'active',   CURDATE(), CURDATE()), -- صحي الشفا + عيون الذيب
  (3, 12, 124, 2.500, 'active',   CURDATE(), CURDATE()), -- السويلميين + مقبرة الشفا + الحوتري
  (3, 12, 125, 4.000, 'active',   CURDATE(), CURDATE()), -- الحديقة النباتية + سمير شما + كلية عمان + الطاقة الذرية + مقبرة شمال عمان
  (3, 12, 126, 4.000, 'active',   CURDATE(), CURDATE()); -- دوار الترخيص + مسجد اسيا + ميراس الترخيص + اسكان العدل + الكوم الغربي
  