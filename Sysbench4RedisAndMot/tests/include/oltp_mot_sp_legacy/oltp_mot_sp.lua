pathtest = string.match(test, "(.*/)")

if pathtest then
   dofile(pathtest .. "common_mot.lua")
else
   require("common")
end

function prepare_statements()
   for i=1, oltp_tables_count do
      rc = db_query("PREPARE ps_select_c_from_sbtest"..i.."(INT) AS SELECT * FROM select_c_from_sbtest"..i.."($1);")
      rc = db_query("PREPARE ps_select_c_where_sbtest"..i.."(INT, INT) AS SELECT * FROM select_c_where_sbtest"..i.."($1,$2);")
      rc = db_query("PREPARE ps_select_sum_sbtest"..i.."(INT, INT) AS SELECT * FROM select_sum_sbtest"..i.."($1,$2);")
      rc = db_query("PREPARE ps_select_c_where_order_sbtest"..i.."(INT, INT) AS SELECT * FROM select_c_where_order_sbtest"..i.."($1,$2);")
      rc = db_query("PREPARE ps_select_distinct_c_where_order_sbtest"..i.."(INT, INT) AS SELECT DISTINCT c FROM sbtest" ..i.. " WHERE id BETWEEN $1 AND $2 ORDER BY c;")
      rc = db_query("PREPARE ps_update_sbtest"..i.."(TEXT,INT) AS SELECT * FROM update_sbtest"..i.."($1,$2);")
      rc = db_query("PREPARE ps_delete_from_sbtest"..i.."(INT) AS SELECT * FROM delete_from_sbtest"..i.."($1);")
      rc = db_query("PREPARE ps_insert_into_sbtest"..i.."(INT,INT,TEXT,TEXT) AS SELECT * FROM insert_into_sbtest"..i.."($1,$2,$3,$4);")
   end
   print("PREPARE STATEMENT FOR STORE PROCEDURE")
end


function thread_init()
   set_vars()
   if (((db_driver == "mysql") or (db_driver == "attachsql")) and mysql_table_engine == "myisam") then
      local i
      local tables = {}
      for i=1, oltp_tables_count do
         tables[i] = string.format("sbtest%i WRITE", i)
      end
      begin_query = "LOCK TABLES " .. table.concat(tables, " ,")
      commit_query = "UNLOCK TABLES"
   else
      begin_query = "BEGIN"
      commit_query = "COMMIT"
   end
   prepare_statements()
end

function get_range_str()
   local start = sb_rand(1, oltp_table_size)
   return string.format(" WHERE id BETWEEN %u AND %u",
                        start, start + oltp_range_size - 1)
end

function event()
   local rs
   local i
   local table_name
   local c_val
   local pad_val
   local query



   table_name = "sbtest".. sb_rand_uniform(1, oltp_tables_count)
   if not oltp_skip_trx then
      db_query(begin_query)
   end

   if not oltp_write_only then

   for i=1, oltp_point_selects do
      -- rs = db_query("SELECT c FROM ".. table_name .." WHERE id=" ..
      --                  sb_rand(1, oltp_table_size))
      rs = db_query("EXECUTE ps_select_c_from_".. table_name .."(" .. sb_rand(1, oltp_table_size) .. ")")
   end

   if oltp_range_selects then

   for i=1, oltp_simple_ranges do
      -- rs = db_query("SELECT c FROM ".. table_name .. get_range_str())
      local start = sb_rand(1, oltp_table_size)
      rs = db_query("EXECUTE ps_select_c_where_".. table_name .."(" .. string.format("%u, %u", start, start + oltp_range_size - 1) .. ")")
   end

   for i=1, oltp_sum_ranges do
      -- rs = db_query("SELECT SUM(K) FROM ".. table_name .. get_range_str())
      local start = sb_rand(1, oltp_table_size)
      rs = db_query("EXECUTE ps_select_sum_".. table_name .."(" .. string.format("%u, %u", start, start + oltp_range_size - 1) .. ")")
   end

   for i=1, oltp_order_ranges do
      -- rs = db_query("SELECT c FROM ".. table_name .. get_range_str() ..
      --               " ORDER BY c")
      local start = sb_rand(1, oltp_table_size)
      rs = db_query("EXECUTE ps_select_c_where_order_".. table_name .."(" .. string.format("%u, %u", start, start + oltp_range_size - 1) .. ")")
   end

   for i=1, oltp_distinct_ranges do
      -- rs = db_query("SELECT DISTINCT c FROM ".. table_name .. get_range_str() ..
      --               " ORDER BY c")
      local start = sb_rand(1, oltp_table_size)
      rs = db_query("EXECUTE ps_select_distinct_c_where_order_".. table_name .."(" .. string.format("%u, %u", start, start + oltp_range_size - 1) .. ")")
   end

   end

   end
   
   if not oltp_read_only then
--[[
   for i=1, oltp_index_updates do
      rs = db_query("UPDATE " .. table_name .. " SET k=k+1 WHERE id=" .. sb_rand(1, oltp_table_size))
   end
--]]
   local range = oltp_table_size / threads
   local start = range * thread_id
   local stop = start + range

   for i=1, oltp_non_index_updates do
      c_val = sb_rand_str("###########-###########-###########-###########-###########-###########-###########-###########-###########-###########")
      -- query = "UPDATE " .. table_name .. " SET c='" .. c_val .. "' WHERE id=" .. sb_rand(1, oltp_table_size)
      -- rs = db_query(query)
      -- rs = db_query("EXECUTE ps_update_".. table_name .."('" .. c_val .."', " .. sb_rand(1, oltp_table_size) .. ")")
      rs = db_query("EXECUTE ps_update_".. table_name .."('" .. c_val .."', " .. sb_rand(start, stop) .. ")")
      if rs then
        print(query)
      end
   end

   

   for i=1, oltp_delete_inserts do

   -- i = sb_rand(1, oltp_table_size)
   i = sb_rand(start, stop)

   -- rs = db_query("DELETE FROM " .. table_name .. " WHERE id=" .. i)
   rs = db_query("EXECUTE ps_delete_from_".. table_name .."(" .. i .. ")")
   c_val = sb_rand_str([[
###########-###########-###########-###########-###########-###########-###########-###########-###########-###########]])
   pad_val = sb_rand_str([[
###########-###########-###########-###########-###########]])

   -- rs = db_query("INSERT INTO " .. table_name ..  " (id, k, c, pad) VALUES " .. string.format("(%d, %d, '%s', '%s')",i, sb_rand(1, oltp_table_size) , c_val, pad_val))
   rc = db_query("EXECUTE ps_insert_into_" .. table_name .. "" .. string.format("(%d, %d, '%s', '%s')",i, sb_rand(1, oltp_table_size) , c_val, pad_val) )
   end

   end -- oltp_read_only

   if not oltp_skip_trx then
      db_query(commit_query)
   end

end

