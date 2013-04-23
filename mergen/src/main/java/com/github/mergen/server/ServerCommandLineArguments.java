package com.github.mergen.server;
import com.beust.jcommander.Parameter;
import java.util.List;
import java.util.ArrayList;


public class ServerCommandLineArguments {

	  @Parameter(names = "-hz", description = "Hazelcast cluster address [add multiple addresses for fallback, "+
	  										  " like -hz 127.0.0.1:5701 -hz 127.0.0.1:5702")
	  public List<String> hzcluster = new ArrayList<String>();

	  @Parameter(names = "-c", description = "hazelcast config xml path")
	  public String configpath = "";	  

	  @Parameter(names = "-multicastgroup", description = "multicast group eg: 224.2.2.3")
	  public String multicastgroup = "";	  
	  
	  @Parameter(names = "-multicastport", description = "multicast port eg: 54327")
	  public Integer multicastport = 0;	  
	  
	  @Parameter(names = "-h", description = "Listening interface host")
	  public String host = "127.0.0.1";

	  @Parameter(names = "-p", description = "Listening interface port")
	  public Integer port = 6380;
	  
	  @Parameter(names = "-publicipaddress", description = "Public ip address")
	  public String publicipaddress = "";
	  

	  @Parameter(names = "-persistence", description = "persistence")
	  public String persistence = "false";

	  @Parameter(names = "-persistence-class", description = "persistence Class")
	  public String persistence_class = "com.github.mergen.persistence.DummyStoreFactory";
	  
	  @Parameter(names = "-persistence-write-delay", description = "persistence write delay")
	  public int persistence_write_delay = 0;
	  
	  @Parameter(names = "-persistence-servers", description = "persistence servers")
	  public String persistence_servers = "localhost:6379";
	  
}