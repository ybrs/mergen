This is a hazelcast proxy, you can access to your hazelcast cluster via any redis client.

To build the package, simply run:
mvn package

To run:
mvn exec:java -Dexec.mainClass="com.github.hzrds.server.Server"

Note: you need to have a working hazelcast cluster on localhost for now.
