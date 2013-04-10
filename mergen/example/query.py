import redis
import json

r = redis.StrictRedis(host='localhost', port=6380, db=0)
r.execute_command("AUTH", "foo", "bar")

r.execute_command("OHSET", "myobjects", "foo", json.dumps({"id":1, "name":"foo", "age":36}))
r.execute_command("OHSET", "myobjects", "foo2", json.dumps({"id":2, "name":"foo", "age":[36, 12]}))


#val = r.execute_command("OHGET", "myobjects", "foo2")
#print "foo2:", json.loads(val)

values = r.execute_command("OHQUERY", "myobjects", "id in (1,2)")
print "val:", values
print "query:", json.loads(values[0])
print r.keys()

values = r.execute_command("OHQUERY", "myobjects", "id in (3) and age=3")
print values

values = r.execute_command("OHQUERY", "myobjects", "id>0")
print values