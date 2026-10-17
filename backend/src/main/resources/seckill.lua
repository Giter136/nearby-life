local userId=ARGV[1]
local voucherId=ARGV[2]

local stockKey="seckill:stock:"..voucherId
local orderListKey="seckill:order:"..voucherId

--校验库存
local storage = redis.call('get',stockKey)
if(not storage or tonumber(storage)<=0) then
    return 1
end
if(redis.call('sismember',orderListKey,userId)==1) then
    return 2
end
redis.call('incrby',stockKey,-1)
redis.call('sadd',orderListKey,userId)
return 0