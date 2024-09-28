select
	sum(l_extendedprice) / 7.0 as avg_yearly
from
	lineitem,
	part,
	(select 
	    l_partkey as temp_l_partkey, 0.2 * avg(l_quantity) as temp_avg
	 from
		lineitem
	 group by l_partkey
	) as temp
where
	p_partkey = l_partkey
	and p_brand = 'Brand#23'
	and p_container = 'MED BOX'
	and l_quantity < temp_avg
	and p_partkey = temp_l_partkey;

