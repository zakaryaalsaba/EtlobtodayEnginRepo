USE restaurant_websites;

DROP PROCEDURE IF EXISTS set_all_restaurants_7am_10pm_7days;
DELIMITER //

CREATE PROCEDURE set_all_restaurants_7am_10pm_7days()
BEGIN
  REPLACE INTO business_hours
    (website_id, day_of_week, open_time, close_time, is_closed, updated_at)
  SELECT
    rw.id,
    d.d,
    '07:00:00',
    '22:00:00',
    0,
    CURRENT_TIMESTAMP
  FROM restaurant_websites rw
  CROSS JOIN (
    SELECT 0 AS d UNION ALL
    SELECT 1 UNION ALL
    SELECT 2 UNION ALL
    SELECT 3 UNION ALL
    SELECT 4 UNION ALL
    SELECT 5 UNION ALL
    SELECT 6
  ) d;
END //

DELIMITER ;




-- Run the procedure to apply 7am–10pm for all 7 days for every restaurant
CALL set_all_restaurants_7am_10pm_7days();
