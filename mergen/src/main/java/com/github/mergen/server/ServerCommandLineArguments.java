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

}