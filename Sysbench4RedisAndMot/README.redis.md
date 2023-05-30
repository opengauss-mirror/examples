

1. complile sysbench binary by README.md
2. cd src/lua
3. modify oltp_common_redis.lua. change require path to your real path, and lua script for redis is in Sysbench4RedisAndMot/lib
4. start redis server, and set IP and PORT in oltp_common_redis.lua, besides, you should start mysql or openGauss server
4. you can run oltp_read_write.lua for test, and you can change its require lua script to 'oltp_common.lua' when you want to run sysbench without redis


