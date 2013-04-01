/**
 * npm install hiredis redis 
 */

var redis = require("redis");

client = redis.createClient(6380, 'localhost', {no_ready_check:true, should_buffer:false});
//redis.debug_mode = true

client.send_command('AUTH', ['foo', 'bar'], function (err, val){
	// console.log('err:' + err)
});

client.send_command("SET", ["mykey", "string val"], redis.print);
client.send_command("GET", ["mykey"], redis.print);

var myobj = {id: 3, name: 'foo'}
client.send_command("OHSET", ["myobjects", "fromnode", JSON.stringify(myobj)]);
client.send_command("OHGET", ["myobjects", "fromnode"], redis.print);

setInterval(function(){ 
	client.send_command("OHGET", ["myobjects", "fromnode"], redis.print);
}, 1000);
