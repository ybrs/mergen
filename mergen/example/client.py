import redis
r = redis.Redis(host='localhost', port=6380)
r.set("foo", "bar")
r2 = redis.Redis(host="46.137.20.245", port=6380)
c = r2.get("foo")
assert c == "foo"
