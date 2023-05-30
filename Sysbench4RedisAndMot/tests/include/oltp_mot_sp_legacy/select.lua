pathtest = string.match(test, "(.*/)")

if pathtest then
   dofile(pathtest .. "common_mot.lua")
else
   require("common")
end

function prepare_statements()
   for i=1, oltp_tables_count do
      rc = db_query("PREPARE ps_select_from_sbtest"..i.."(INT) AS SELECT * FROM select_c_from_sbtest"..i.."($1);")
   end
   print("PREPARE STATEMENT FOR STORE PROCEDURE SELECT")
end


function thread_init()
   set_vars()
   prepare_statements()
end

function event()
   local table_name
   table_name = "sbtest".. sb_rand_uniform(1, oltp_tables_count)
   rs = db_query("EXECUTE ps_select_from_".. table_name .."(" .. sb_rand(1, oltp_table_size) .. ")")
end

