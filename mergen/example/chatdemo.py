import cmdln
import os
import socket
import redis
import multiprocessing 
from threading import Thread

hostname = socket.gethostname()


def subscriber():
    r = redis.Redis(port=6380)
    pubsub = r.pubsub()
    pubsub.subscribe(["channel_all", "channel_%s" % hostname])
    print "sent subsc"
    for msg in pubsub.listen():
        # commander.cmd(["print", msg])
        commander.stderr.write("foo")
        commander.stderr.flush()
        commander.emptyline()
        
        
r = redis.Redis(port=6380)
class Commander(cmdln.Cmdln):
    prompt = '[%s]>' % hostname     
        
    def do_shell(self, line):
        "Run a shell command"
        print "running shell command:", line
        output = os.popen(line).read()
        print output
        self.last_output = output

    def do_say(self, line):
        r.publish("channel_all", line)
        print "(sent)"

    def do_print(self, line):
        print 
        print line
        print "============"        
    
    def do_EOF(self, line):
        return True

commander = Commander()
 
thread = multiprocessing.Process(target=subscriber)
thread.start()

if __name__ == '__main__':
    commander.cmdloop()