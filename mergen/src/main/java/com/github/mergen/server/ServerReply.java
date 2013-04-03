package com.github.mergen.server;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channels;
import java.nio.charset.Charset;

class ServerReply {
	/**
	* This is the main response class...
	**/

	private ChannelBuffer replyOK;
	private ChannelBuffer replyNone;

    public ChannelBuffer replyOK(){          
        if (replyOK == null){
            replyOK = ChannelBuffers.dynamicBuffer();
            replyOK.writeBytes("+OK\r\n".getBytes());            
        }
        return replyOK;
    }

    public ChannelBuffer replyNone(){          
        if (replyNone == null){
            replyNone = ChannelBuffers.dynamicBuffer();
            replyNone.writeBytes("*-1\r\n".getBytes());            
        }
        return replyNone;
    }

	public ChannelBuffer replyInt(int i){
		ChannelBuffer reply = ChannelBuffers.dynamicBuffer();
		String r = ":"+i+"\r\n";
        reply.writeBytes(r.getBytes());            
        return reply;
	}

	public ChannelBuffer replyError(String s){
		ChannelBuffer reply = ChannelBuffers.dynamicBuffer();
		String r = "-ERR "+s+"\r\n";
        reply.writeBytes(r.getBytes());            
        return reply;
	}		

	public ChannelBuffer replyStatus(String s){
		ChannelBuffer reply = ChannelBuffers.dynamicBuffer();
		String r = "+"+s+"\r\n";
        reply.writeBytes(r.getBytes());            
        return reply;
	}

    /**
    *
    * *<number of arguments> CR LF
    * $<number of bytes of argument 1> CR LF
    * <argument data> CR LF
    */
    public ChannelBuffer replyMulti(String[] s){
        ChannelBuffer reply = ChannelBuffers.dynamicBuffer();
        reply.writeBytes( ("*" + s.length + "\r\n").getBytes() );
        for (String str: s){
            String r = "$"+str.getBytes().length+"\r\n";    
            reply.writeBytes(r.getBytes());            
            reply.writeBytes(str.getBytes());
            reply.writeBytes("\r\n".getBytes());
        }
        // System.out.println(reply.toString(Charset.defaultCharset()));
        return reply;
    }


    public class MultiReply {
        private ChannelBuffer buffer;
        private int elemcnt = 0;

        public MultiReply(){
            buffer = ChannelBuffers.dynamicBuffer();
        }

        public MultiReply addString(String s){
            
            String r = "$"+s.getBytes().length+"\r\n";    
            buffer.writeBytes(r.getBytes());            
            buffer.writeBytes(s.getBytes());
            buffer.writeBytes("\r\n".getBytes());

            elemcnt++;
            return this;
        }

        public MultiReply addInt(int i){
        	buffer.writeBytes(":".getBytes());        	
        	buffer.writeBytes(Integer.toBinaryString(i).getBytes());        	
        	buffer.writeBytes("\r\n".getBytes());
            elemcnt++;
            return this;
        }

        public MultiReply addNull(){
            String r = "$-1\r\n";    
            buffer.writeBytes(r.getBytes());            
            elemcnt++;
            return this;
        }

        public MultiReply finish(){    
            // prepend header line             
            ChannelBuffer newbuffer = ChannelBuffers.dynamicBuffer();
            newbuffer.writeBytes( ("*" + elemcnt + "\r\n").getBytes() );
            newbuffer.writeBytes(buffer);
            this.buffer = newbuffer;
            return this;
        }

        public ChannelBuffer getBuffer(){
            return buffer;
        }

    }

    public MultiReply startMultiReply() {
        return new MultiReply();
    }


}