cd ../../hazelcast/hazelcast/
mvn -Dmaven.test.skip=true package
cd ../../mergen/mergen/
mvn install:install-file -Dfile=/home/ybrs/projects/hazelcast/hazelcast/target/hazelcast-2.4.2-SNAPSHOT.jar \
	-DgroupId=com.hazelcast \
	-DartifactId=hazelcast -Dversion=2.4.2-SNAPSHOT -Dpackaging=jar
mvn package
