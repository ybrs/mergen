import redis
r = redis.Redis(port=6380)
# r = redis.Redis()

r.set("foo", "bar")
print "setted"

pubsub = r.pubsub()
pubsub.subscribe("foo")
for msg in pubsub.listen():
    print "received >>>", msg
