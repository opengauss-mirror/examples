select
	s_name,
	s_address
from
	supplier,
	nation
where
	s_suppkey in (
		select
			ps_suppkey
		from
			partsupp,
			(
				select
					l_partkey as temp_l_partkey, l_suppkey as temp_l_suppkey, 0.5 * sum(l_quantity) as temp_l_quantity
				from
					lineitem
				where
					l_shipdate >= date '1994-01-01'
					and l_shipdate < date '1994-01-01' + INTERVAL '1 year'
				group by l_partkey, l_suppkey
			) as temp
		where
		    temp_l_partkey = ps_partkey
			and temp_l_suppkey = ps_suppkey
			and ps_partkey in (
				select
					p_partkey
				from
					part
				where
					p_name like 'forest%'
			)
			and ps_availqty > temp_l_quantity
	)
	and s_nationkey = n_nationkey
	and n_name = 'CANADA'
order by
	s_name;

