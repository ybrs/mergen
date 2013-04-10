import redis
r = redis.Redis(port=6381)
#r = redis.Redis()

#r.set("foo", "bar")
#print "setted"

pubsub = r.pubsub()

# r.execute_command("SUBSCRIBE", "ccc", "bar")

# pubsub.unsubscribe(["foo", "bar"])

pubsub.subscribe(["foo", "bar"])
print "sent subsc"
for msg in pubsub.listen():
    print "in listen mode"
    print "received >>>", msg
