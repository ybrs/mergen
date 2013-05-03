# -*- coding: utf-8 -*-
import thegiant
from thegiant.helpers import OK, reply
# this is a quick and dirty Key value server that holds everything in ram
# mainly for test purposes

#from sqlitedriver import set, get, commit
from blackhole import set, get, commit

class Dispatcher(object):
    
    def get(self, k):
        r = reply(get(k).encode("utf-8"))        
        return r
    
    def set(self, k, v):
        set(k, v)
        return OK
    
    def quit(self):
        return OK
    
    def ping(self):
        return reply("PONG")

h = {}
def persista(e):
    print "==================="
    print e['REDIS_CMD']
    print "==================="
        
    dispatcher = Dispatcher()
    cmd = e['REDIS_CMD'][0]
    args = []
    try:
        args = e['REDIS_CMD'][1:]
    except Exception as e:
        print e
        
    fn = getattr(dispatcher, cmd.lower())
    if args:
        return fn(*args)
    else:
        return fn()
    
    raise Exception("unknown command")


thegiant.server.add_timer(2, commit)
thegiant.server.run(persista, '0.0.0.0', 6390)
