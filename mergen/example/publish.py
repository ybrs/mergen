import redis
r = redis.Redis(port=6380)
# r = redis.Redis()
r.publish("foo", "hello")
