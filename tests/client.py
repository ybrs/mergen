import redis
r = redis.StrictRedis(host='localhost', port=6380, db=0)
# s = r.ping()
try:
	r.set("foo", 1)
except:
	print "auth not set"

r.execute_command("AUTH", "foo", "bar")
r.set("foo", 1)

r.execute_command("HSET", "foo", "bar", "baz")
r.execute_command("HSET", "foo", "bar2", "bazi")
print r.execute_command("HMGET", "foo", "bar", "bar2", "bar3")
print r.execute_command("HMGET", "foor", "bar", "bar2", "bar3")

print r.hmget("foo", "bar", "bar2", "bar3")

# r.set("bar", 2)

# c = r.get("foo")
# print c

# c = r.get("unexistingfoo")
# print c

# print "delete"
# c = r.delete("foo", "bar")
# print "---"
# print c

# # s = r.execute_command("hello", "foo", 1)
# # print s 