select
	100 * sum(case
		when p_type like 'PROMO%'
			then l_extendedprice * (1 - l_discount)/10
		else 0
	end) / sum(l_extendedprice * (1 - l_discount)/1000) as promo_revenue --add /1000
from
	lineitem,
	part
where
	l_partkey = p_partkey
	and l_shipdate >= date '1995-09-01'
	and l_shipdate < date '1995-09-01' + INTERVAL '1 month';

