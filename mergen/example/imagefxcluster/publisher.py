import os
import socket
import redis
import json
import time
import sys
import wand
import pystacia
import cPickle as pickle


r = redis.Redis(port=6380)
r.execute_command("LCLEAR", "workers")
r.execute_command("LCLEAR", "completed_parts")
r.execute_command("LCLEAR", "image_parts")
# we send everyone a register command to see in debug screens
r.publish("generic", json.dumps({"fn":"register", "args":[]}))


def split():    
    image = pystacia.read(sys.args[1])
    rawdata = image.get_blob('png')
    r.set("image_dimensions", json.dumps([image.width, image.height]))
    x = 0
    y = 0     
    while x < image.width:        
        y = 0    
        while y < image.height:
            tmpimg = pystacia.read_blob(rawdata)
            x1 = x
            y1 = y
            x2 = x + 80
            if x2 > image.width:
                x2 = image.width
            y2 = y + 80
            if y2 > image.height:
                y2 = image.height
            print "r:", x1, y1, x2, y2
            # crop is resize ohhh ?
            # http://liquibits.bitbucket.org/image.html#resizing
            # params are: width, height, x, y
            data = tmpimg.resize(x2-x1, y2-y1, x1, y1)
            key = "img_%s_%s_%s_%s" % (x1, y1, x2, y2)
            r.set(key, pickle.dumps(data.get_blob('png')))
            r.lpush("image_parts", key)            
            y += 80
        x += 80
    return r.execute_command("LGETALL", 'image_parts')

def join():
    w, h = json.loads(r.get('image_dimensions'))
    image = pystacia.blank(w, h)
    for key in r.execute_command('LGETALL', 'completed_parts'):
        print "key:", key 
        data = pickle.loads(r.get(key))
#        f = open("./tmp/%s.png" % key, 'w')
#        f.write(data)
#        f.close()
        i, x1, x2, y1, y2 = key.split('_')
        print x1, x2
        t = pystacia.read_blob(data)
        image.overlay(t, int(x1), int(x2))
    image.write("foo.png")
        
        
## we split the image and push to queues        
parts = split()

#
### lets see who we have around
#workers = r.execute_command("LGETALL", "workers")
#print "workers....", workers    
#for worker in workers:    
#    r.publish("queue_%s" % worker, json.dumps({"fn":"hello", "args":[worker]}) )

# let the workers know there is a job
r.publish("generic", json.dumps({"fn":"apply_fx", "args":['image_parts']}) )

notcompleted = True
while notcompleted:
    # check if we have all the parts available
    in_queue = r.execute_command("LGETALL", "image_parts")
    completed_parts = r.execute_command("LGETALL", "completed_parts")
    print "length", len(in_queue)
    if len(in_queue) == 0 and len(completed_parts) == len(parts): 
        join()
        notcompleted = False
    else:
        print "not ready, sleeping"
        time.sleep(0.5)


