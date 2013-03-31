import redis
import json
import time

r = redis.StrictRedis(host='localhost', port=6380, db=0)
r.execute_command("AUTH", "foo", "bar")


s= json.dumps({"id":1, "name":"foo", "age":36}) 

cnt = 50000
step = 10000

t1 = time.time()
for i in range(1, cnt):
    r.execute_command("OHSET", "myobjects", "foo%s" % (t1 + i), s)
    # r.execute_command("OHDUMMY2", "myobjects", "foo%s" % (t1 + i), s)
    # r.execute_command("SET", "foo%s" % i, s)
    if i % step == 0:
        t2 = time.time()
        print ">>>>", (t2-t1), step/(t2-t1), "per second single connection"
        t1 = time.time() 

#r.execute_command("OHSET", "myobjects", "foo2", json.dumps({"id":2, "name":"foo", "age":[36, 12]}))
#
##val = r.execute_command("OHGET", "myobjects", "foo2")
##print "foo2:", json.loads(val)
#
#values = r.execute_command("OHQUERY", "myobjects", "id in (1,2)")
#print "val:", values
#print "query:", json.loads(values[0])
#
#
## print r.keys()
