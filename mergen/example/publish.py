import redis
r = redis.Redis(port=6380)
# r = redis.Redis()
import time
r.execute_command("SUBSCRIBERS")
r.publish("foo", str(int(time.time())))
