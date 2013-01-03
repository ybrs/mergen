import redis
r = redis.StrictRedis(host='localhost', port=6380, db=0)
r.execute_command("AUTH", "foo", "bar")
r.set("foo", 1)
print r.keys()
