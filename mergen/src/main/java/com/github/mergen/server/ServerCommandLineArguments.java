package com.github.mergen.server;
import com.beust.jcommander.Parameter;
import java.util.List;
import java.util.ArrayList;


public class ServerCommandLineArguments {

	  @Parameter(names = "-hz", description = "Hazelcast cluster address [add multiple addresses for fallback, "+
	  										  " like -hz 127.0.0.1:5701 -hz 127.0.0.1:5702")
	  public List<String> hzcluster = new ArrayList<String>();


	  @Parameter(names = "-h", description = "Listening interface host")
	  public String host = "127.0.0.1";

	  @Parameter(names = "-p", description = "Listening interface port")
	  public Integer port = 6380;

}