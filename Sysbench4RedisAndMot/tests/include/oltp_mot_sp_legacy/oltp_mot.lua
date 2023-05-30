pathtest = string.match(test, "(.*/)")

if pathtest then
   dofile(pathtest .. "common_mot.lua")
else
   require("common")
end

function prepare_statements()
   for i=1, oltp_tables_count do
      rc = db_query("PREPARE ps_sp_sbtest"..i.."(INT,INT,TEXT,TEXT) AS SELECT * FROM sp_sbtest"..i.."($1,$2,$3,$4);")
      rc = db_query("PREPARE ps_select_c_from_sbtest"..i.."(INT) AS SELECT * FROM select_c_from_sbtest"..i.."($1);")
      rc = db_query("PREPARE select_c_from_sbtest"..i.."(INT) AS SELECT * FROM sbtest"..i.." WHERE id = " .. "($1);")
   end
   -- print("PREPARE STATEMENT FOR STORE PROCEDURE")
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
oltp_skip_trx = true

   table_name = "sbtest".. sb_rand_uniform(1, oltp_tables_count)

   if not oltp_read_only then
     id = sb_rand(1, oltp_table_size)
     c_val = sb_rand_str([[
###########-###########-###########-###########-###########-###########-###########-###########-###########-###########]])
     pad_val = sb_rand_str([[
###########-###########-###########-###########-###########]])

     rc = db_query("EXECUTE ps_sp_" .. table_name .. "" .. string.format("(%d, %d, '%s', '%s')",id, sb_rand(1, oltp_table_size) , c_val, pad_val) )
     --rc = db_query("EXECUTE ps_select_c_from_" .. table_name .. "" .. string.format("(%d)",id) )
   end

   if oltp_read_only then
        id = sb_rand(1, oltp_table_size)
        --rs = db_query("EXECUTE select_c_from_".. table_name .."(" .. sb_rand(1, oltp_table_size) .. ")")
        rc = db_query("EXECUTE ps_select_c_from_" .. table_name .. "" .. string.format("(%d)",id) )
   end


   if not oltp_skip_trx then
      db_query(commit_query)
   end

end

