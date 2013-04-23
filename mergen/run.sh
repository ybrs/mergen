mvn exec:java -Dexec.mainClass=com.github.mergen.server.Server -Dexec.args="-hz 127.0.0.1:5701 -hz 127.0.0.1:5702 -persistence true -persistence-servers 127.0.0.1:6379"
