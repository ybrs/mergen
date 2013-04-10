import redis
import json
import time

r = redis.StrictRedis(host='localhost', port=6381, db=0)
r.execute_command("AUTH", "foo", "bar")
t2 = time.time()
for i in xrange(1, 1000000):    
    r.execute_command("OHSET", "myobjects", "foo_%s" % i, json.dumps({"id": i, "name":"foo_%s" % i, "age":i }))
    if i % 1000 == 0:
        print i
        print ">>>", time.time() - t2
        t2 = time.time()
        