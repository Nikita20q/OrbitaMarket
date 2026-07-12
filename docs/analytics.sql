-- Кто сколько купил
SELECT
    user_id,
    COUNT(*) AS paid_orders_count,
    SUM(price) AS total_spent_geocredits
FROM orders
WHERE order_status = 'PAID'
GROUP BY user_id
ORDER BY total_spent_geocredits DESC;

-- Статистика по типам заказов
SELECT
    product_type,
    COUNT(*) AS orders_count,
    SUM(price) AS total_revenue
FROM orders
WHERE order_status = 'PAID'
GROUP BY product_type;

-- Топ-5 пользователей по тратам
SELECT
    user_id,
    SUM(price) AS total_spent
FROM orders
WHERE order_status = 'PAID'
GROUP BY user_id
ORDER BY total_spent DESC
LIMIT 5;