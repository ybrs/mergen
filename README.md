Mergen: Data grid for the rest of us.


What does this do:
----------------------- 
run a few mergen instances, they become a cluster, then use your fav. lang. and connect via redis client.
then bam you get a fully distributed, fault tolerant, low latency, super fast distributed in memory cache/data grid. 

How:
----------------------
Mergen embeds Hazelcast and serves your redis queries - converts them to hazelcast. 

Redis has a ton of clients in different languages, see http://redis.io/clients 


Why:
----------------------
Because i can. And hate not easily having a distributed memory cluster accessible from Python. Its 2013 and still
we have to shard on client side - damn. 


How to build.
------------------------
to build this you need to clone git@github.com:ybrs/hazelcastforked.git to another folder and then
edit build.sh. Sorry its a little bit hacks and all for now.


Can i use this ? Is it stable enough.
------------------------
No, this is pretty alpha, though it works, but never used in production, just for tech reviews. 


Features:
------------------------
Its fast. 
Its fault tolerant.
Its scalable.


