SELECT * FROM restaurant_websites.orders order by id desc;
SELECT * FROM restaurant_websites.orders where order_number like '%-NDGF';
SELECT * FROM restaurant_websites.orders WHERE STATUS not in ('completed', 'cancelled');
update restaurant_websites.orders set status = 'completed' where status in ('confirmed', 'pending','ready');
SELECT * FROM restaurant_websites.delivery_companies;
select * FROM restaurant_websites.delivery_zones;
SELECT * FROM restaurant_websites.orders_delivery;
SELECT * FROM restaurant_websites.restaurant_websites;
SELECT * FROM restaurant_websites.payment_methods;
