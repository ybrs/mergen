import os
import sys
import socket
import redis
import json
import pickle
import pystacia
from effects import noise

def get_name():
    return '%s::%s' % (socket.gethostname(), os.getpid())
    
r = redis.Redis(port=6380)

class Worker(object):
    
    def do_register(self):
        print "registering me..."        
        r.lpush("workers", get_name())

    def do_apply_fx(self, key):        
        while True:
            part = r.lpop(key)
            if not part:
                return
            print "part", part, key
            data = pickle.loads(r.get(part))
            image = pystacia.read_blob(data)
            image.fx(noise)
            retkey = part.replace('img_', 'completed_')
            r.set(retkey, pickle.dumps(image.get_blob('png')))
            r.lpush('completed_parts', retkey)

    def do_hello(self, p):
        print "======================="
        print p
        print "======================="
                
    def do_exit(self):
        sys.exit(0)

worker = Worker()
pubsub = r.pubsub()
pubsub.subscribe(["generic", "queue_%s" % get_name()])

def consume(msg):
    print msg['type']
    if msg['type'] == 'message':
        try:
            payload = json.loads(msg['data'])
            print "payload", payload
        except Exception as e:
            print "message parse exception"
            return
        
        fn = getattr(worker, "do_%s" % payload['fn'])    
        if fn:
            try:
                print "calling fn"
                args = payload['args']
                ret = fn(*args)
                print ret
            except Exception as e:
                import traceback
                traceback.print_exc()                
    else:
        print "received >>>", msg

for msg in pubsub.listen():
    print "going into listen mode"
    print msg
    consume(msg)
    