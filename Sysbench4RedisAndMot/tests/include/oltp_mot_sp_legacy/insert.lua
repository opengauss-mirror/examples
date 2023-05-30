-- pathtest = string.match(test, "(.*/)")
pathtest = "/home/gauss/mot_tests/tools/sysbench-master/tests/include/oltp_mot_sp_legacy/"
-- print(pathtest)
if pathtest then
   dofile(pathtest .. "common_mot.lua")
else
   require("common")
end

function prepare_statements()
   for i=1, oltp_tables_count do
      rc = db_query("PREPARE ps1_insert_into_sbtest"..i.."(INT,TEXT,TEXT) AS SELECT * FROM insert_into1_sbtest"..i.."($1,$2,$3);")
      rc = db_query("PREPARE ps2_insert_into_sbtest"..i.."(INT,INT,TEXT,TEXT) AS SELECT * FROM insert_into2_sbtest"..i.."($1,$2,$3,$4);")
   end
   print("PREPARE STATEMENT FOR STORE PROCEDURE INSERT")
end

function thread_init(thread_id)
   set_vars()
   prepare_statements()
end

function event()
   local table_name
   local i
   local c_val
   local k_val
   local pad_val

   table_name = "sbtest".. sb_rand_uniform(1, oltp_tables_count)

   k_val = sb_rand(1, oltp_table_size)
   c_val = sb_rand_str([[
###########-###########-###########-###########-###########-###########-###########-###########-###########-###########]])
   pad_val = sb_rand_str([[
###########-###########-###########-###########-###########]])

   if (db_driver == "pgsql" and oltp_auto_inc) then
      -- rs = db_query("INSERT INTO " .. table_name .. " (k, c, pad) VALUES " ..
      --                  string.format("(%d, '%s', '%s')", k_val, c_val, pad_val))
      rc = db_query("EXECUTE ps1_insert_into_" .. table_name .. "" .. string.format("(%d, '%s', '%s')", k_val, c_val, pad_val .. "") )
      -- rc = 0
   else
      if (oltp_auto_inc) then
         i = 0
      else
         i = sb_rand_uniq()
      end
      -- rs = db_query("INSERT INTO " .. table_name ..
      --                  " (id, k, c, pad) VALUES " ..
      --                  string.format("(%d, %d, '%s', '%s')", i, k_val, c_val,
      --                                pad_val))
      rc = db_query("EXECUTE ps2_insert_into_" .. table_name .. "" .. string.format("(%d, %d, '%s', '%s')", i, k_val, c_val, pad_val .. "") )   
      -- rc = 0
   end
end
