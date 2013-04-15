# -*- coding: utf8 -*-
import unittest
import redis
import time

class TestSequenceFunctions(unittest.TestCase):

    def setUp(self):
        pass

    def test_setget(self):
        r = redis.StrictRedis(host='localhost', password="bar", port=6380, db=0)
        r.set("foo", "bar")
        c = r.get("foo")
        assert c == "bar"
        r.set("foo2", "öçığü")
        c = r.get("foo2")
        assert c == "öçığü"
        assert r.exists("foo") == True
        r.delete("foo", "foo2")     
        assert r.get("foo") == None
        assert r.exists("foo") == False

    def test_setex(self):
        r = redis.StrictRedis(host='localhost', password="bar", port=6380, db=0)
        r.setex("testexp", 1, "bar")
        assert r.get("testexp") == "bar"
        print "sleepin"
        time.sleep(2)
        assert r.exists("testexp") == False        

    def test_ping(self):
        r = redis.StrictRedis(host='localhost', password="bar", port=6380, db=0)
        c = r.ping()
        assert c

    def test_auth(self):
        r = redis.StrictRedis(host='localhost', port=6380, db=0)
        try:
            r.set("foo2", "bar")
            assert False
        except:
            assert True
        c = r.execute_command("AUTH", "foo","bar")
        assert c
        r.set("foo2", "bar")

    def test_mapcommands(self):
        r = redis.StrictRedis(host='localhost', password="bar", port=6380, db=0)
        r.hset("mymap", "foo", "12345")
        assert "12345" == r.hget("mymap", "foo")
        assert True == r.hexists("mymap", "foo")
        r.hdel("mymap", "foo")
        assert None == r.hget("mymap", "foo")
        assert False == r.hexists("mymap", "foo")
        r.hset("mymap", "foo1", "bar")
        r.hset("mymap", "foo2", "bar2")
        assert len(r.hkeys("mymap"))==2 and ("foo1" in r.hkeys("mymap")) and ("foo2" in r.hkeys("mymap"))
        assert r.hlen("mymap") == 2

        ret = r.hmget("mymap", "foo1", "foo2", "foo3")
        print ret
        assert ret[0] == "bar"
        assert ret[1] == "bar2"
        assert ret[2] == None


    def test_method_not_exists(self):
        r = redis.StrictRedis(host='localhost', password="bar", port=6380, db=0)
        p = False
        try:
            r.execute_command("hello")            
        except:
            p = True
        assert p

    def test_lists(self):
        r = redis.StrictRedis(host='localhost', password="bar", port=6380, db=0)
        for i in range(1, 100):
            r.rpush("foo", i)
        vals = r.execute_command("LGETALL", "foo")
        print vals
        assert vals[0] == "1"    



if __name__ == '__main__':
    unittest.main()