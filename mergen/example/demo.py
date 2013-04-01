import redis
from time import sleep

pool = redis.ConnectionPool(host='localhost', port=6379, db=0)

r = redis.Redis(port=6380)
r.execute_command("AUTH", "foo", "bar")
for i in range(1,10):
    r.set("foo_%s" % i, "bar_%s" % i)

while True:
    try:
        r.execute_command("AUTH", "foo", "bar")
        k = r.keys()
        k.sort()
        print k        
    except redis.exceptions.ConnectionError as e:
        print "node down"
    sleep(1)
        
