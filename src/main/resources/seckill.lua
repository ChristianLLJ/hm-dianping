---
--- Created by 39836.
--- DateTime: 6/17/2024 2:34 PM
---
--- 获取数据
local voucherId = ARGV[1]
local userId = ARGV[2]
local orderId = ARGV[3]

local stockKey = 'seckill:stock:' .. voucherId
local orderKey = 'seckill:order:' .. voucherId

--- 判断库存
if(tonumber(redis.call('get', stockKey)) <= 0) then
	return 1
end
---判断是否重复下单
if (redis.call('sismember', orderKey, userId) == 1) then
	return 2
end

---扣库存
redis.call('incrby', stockKey, -1)
---下单
redis.call('sadd', orderKey, userId)
---发送订单id到消息队列
redis.call('xadd', 'stream.orders', '*', 'userId', userId, 'voucherId', voucherId, 'id', orderId)

return 0
