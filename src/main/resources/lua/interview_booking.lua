-- KEYS[1] = stock key
-- KEYS[2] = user booking key
-- ARGV[1] = studentId
-- ARGV[2] = ttl seconds

if redis.call('exists', KEYS[2]) == 1 then
    return -2
end

local stock = tonumber(redis.call('get', KEYS[1]) or '0')
if stock <= 0 then
    return -1
end

redis.call('decr', KEYS[1])
redis.call('set', KEYS[2], ARGV[1], 'EX', ARGV[2])
return stock - 1
