SELECT 
  CAST(amc AS decimal(15,4)) / NULLIF(CAST(pmc AS decimal(15,4)), 0) AS am_pm_ratio
FROM 
  (SELECT count(*) amc
   FROM web_sales, household_demographics, time_dim, web_page
   WHERE ws_sold_time_sk = time_dim.t_time_sk
     AND ws_ship_hdemo_sk = household_demographics.hd_demo_sk
     AND ws_web_page_sk = web_page.wp_web_page_sk
     AND time_dim.t_hour BETWEEN 9 AND 9 + 1
     AND household_demographics.hd_dep_count = 3
     AND web_page.wp_char_count BETWEEN 5000 AND 5200) at,
  (SELECT count(*) pmc
   FROM web_sales, household_demographics, time_dim, web_page
   WHERE ws_sold_time_sk = time_dim.t_time_sk
     AND ws_ship_hdemo_sk = household_demographics.hd_demo_sk
     AND ws_web_page_sk = web_page.wp_web_page_sk
     AND time_dim.t_hour BETWEEN 16 AND 16 + 1
     AND household_demographics.hd_dep_count = 3
     AND web_page.wp_char_count BETWEEN 5000 AND 5200) pt
ORDER BY am_pm_ratio
LIMIT 100;