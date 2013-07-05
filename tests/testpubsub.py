import subprocess
import os
import sys
import unittest
import time
import redis
from threading import Thread

def pubsubthread(identifier="testthread"):
    conn = redis.Redis(host="localhost", port=6380)
    conn.execute_command("IDENTIFY", identifier)
    pubsub = conn.pubsub()
    pubsub.subscribe(["FOO", "BAR"])
    try:
        for msg in pubsub.listen():
            print "in listen mode"
            print "received >>>", msg
    except:
        pass

def execute(command):
    process = subprocess.Popen(command, shell=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)

    # Poll process for new output until finished
    while True:
        nextline = process.stdout.readline()
        if nextline == '' and process.poll() != None:
            break
        sys.stdout.write(nextline)
        sys.stdout.flush()

    output = process.communicate()[0]
    exitCode = process.returncode

    if (exitCode == 0):
        return output
    else:
        raise ProcessException(command, exitCode, output)

def subscribe_channels():
    pass

class PubSubTestCase(unittest.TestCase):

    def setUp(self):
        self.mergen = os.path.realpath(
                os.path.join(
                    os.path.realpath(os.path.dirname(__file__)), 
                            '..', '..', 'mergen','mergen','run.sh'))

    def test_subscribers(self):
        mythread = Thread(target=execute, args=[self.mergen])
        mythread.daemon = True
        mythread.start()
        time.sleep(5)
        conn = redis.Redis(host="localhost", port=6380)
        psbth = Thread(target=pubsubthread, args=["testthread"])
        psbth.daemon = True
        psbth.start()
        time.sleep(3)

        subscribers = conn.execute_command("SUBSCRIBERS", "FOO")
        print subscribers
        assert len(subscribers) == 2
        assert subscribers[1] == "testthread"

        for i in range(1,10):
            psbth = Thread(target=pubsubthread, args=["testthread-%s" % i])
            psbth.daemon = True
            psbth.start()
        
        time.sleep(1)

        subscribers = conn.execute_command("SUBSCRIBERS", "FOO")
        print subscribers
        #assert len(subscribers) == 22
        for i in range(1,10):
            assert "testthread-%s" % i in subscribers

        try:
            conn.execute_command("SHUTDOWN")
        except Exception as err:
            print err




if __name__ == '__main__':
    unittest.main()