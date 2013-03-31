package com.github.mergen.server;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;
import java.util.HashMap;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;


public class CommandDispatcher {
	
	private ServerCommands controller;

	class MethodElem {
		public Method method;
		public RedisCommand annot;
		public Class klass;

		public MethodElem(Method m, RedisCommand a, Class k){
			this.method = m;
			this.annot = a;
			this.klass = k;
		}
	}

	private Map<String, MethodElem> methodmap = new HashMap<String, MethodElem>();

	interface Wrapper {	    
	}

	public void parse(Class klass) {
		Method[] methods = klass.getMethods();
		for (Method method : methods) {
        	if (method.isAnnotationPresent(RedisCommand.class)) {
				// methodmap.        		
				RedisCommand annot = method.getAnnotation(RedisCommand.class);
				String cmd = annot.cmd();
				methodmap.put(cmd, new MethodElem(method, annot, klass));
        	}
        }
	}

	public Class getClassForCommand(String cmd){		
		MethodElem m = methodmap.get(cmd);
		if (m==null){
			return null;
		}
		return m.klass;
	}

	public CommandDispatcher(List< Class< ? > > klasses){
		// we build a command map, here.		
		for (Class klass: klasses){
			this.parse(klass);	
		}		
	}

	public void dispatch(Controller controller, MessageEvent e, Object[] args){		
		String cmd = new String((byte[])args[0]);
		MethodElem m = methodmap.get(cmd.toUpperCase());
			
		if (m==null){			
            ServerReply sr = new ServerReply();
            e.getChannel().write(sr.replyError("method not implemented - " + cmd));
            System.out.println("[" + cmd + "] received");
			return;
		}

		if (m.annot.authenticate() && !controller.base.isAuthenticated()){
            ServerReply sr = new ServerReply();
            e.getChannel().write(sr.replyError("You need to authenticate"));
            return;
		}

		
		try {			
			
			Object ret = m.method.invoke(controller, e, args);

			if (!m.annot.returns().equals("")){
				if (m.annot.returns().equals("OK")){
		            ServerReply sr = new ServerReply();
		            e.getChannel().write(sr.replyOK());				
				} else if (m.annot.returns().equals("status")){
		            ServerReply sr = new ServerReply();
		            e.getChannel().write(sr.replyStatus((String)ret));
				}
			}

		} catch (IllegalAccessException exc) {
			// return method not implemented error;
            ServerReply sr = new ServerReply();
            e.getChannel().write(sr.replyError("method not implemented"));
            exc.printStackTrace();
		} catch (InvocationTargetException exc){
			// return method error
            ServerReply sr = new ServerReply();
            e.getChannel().write(sr.replyError("Problem invoking method"));
			exc.printStackTrace();
		}
	}

}