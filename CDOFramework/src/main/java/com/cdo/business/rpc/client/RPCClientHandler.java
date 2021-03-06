package com.cdo.business.rpc.client;


import java.io.File;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.cdo.business.rpc.RPCFile;
import com.cdo.google.handle.CDOMessage;
import com.cdo.google.handle.Header;
import com.cdo.google.handle.ProtoProtocol;
import com.cdo.google.protocol.GoogleCDO;
import com.cdo.util.common.UUidGenerator;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;

public class RPCClientHandler extends  ChannelInboundHandlerAdapter {
	private static Logger logger=Logger.getLogger(RPCClientHandler.class);

    private volatile Channel channel;
    private ByteString clientId; 
    final CallsLinkedHashMap calls = new CallsLinkedHashMap();
    /** A counter for generating call IDs. */
    static final AtomicInteger callIdCounter = new AtomicInteger();
    
    /**
     * 通过 tcp来关闭server
     */
    public void stopLocalServer(){
		CDOMessage stopServer=new CDOMessage();
		Header header=new Header();
		header.setType(ProtoProtocol.TYPE_STOPLOCALServer);	
		stopServer.setHeader(header);
		channel.writeAndFlush(stopServer);
    }
    
    public RPCResponse handleTrans(CDO cdoRequest) {   
    	//是否有文件传输
    	List<File> files=null;
    	try{
    		files=RPCFile.readFileFromCDO(cdoRequest);    		
    	}catch(Exception ex){
		    //文件不存在直接返回 ,跟RPCServerHandler 返回给客户端保持一致风格
    		logger.error(ex.getMessage(), ex);
    		CDO cdoOutput=new CDO();
			CDO cdoResponse=new CDO();
			CDO cdoReturn=new CDO();
			cdoReturn.setIntegerValue("nCode",-1);
			cdoReturn.setStringValue("strText","RPCClient "+ex.getMessage());
			cdoReturn.setStringValue("strInfo","RPCClient "+ex.getMessage());
			cdoOutput.setCDOValue("cdoReturn",cdoReturn);
			cdoOutput.setCDOValue("cdoResponse", cdoResponse);	    					 
			return new RPCResponse(cdoOutput.toProtoBuilder().build());
    	}
    	
    	//CDO请求数据
    	int callId=callIdCounter.getAndIncrement() & Integer.MAX_VALUE;
        final Call call =new Call(callId);    
    	GoogleCDO.CDOProto.Builder proto=cdoRequest.toProtoBuilder();
    	proto.setCallId(callId);
		proto.setClientId(clientId);	
		//构造发送message 类型数据
		Header reqHeader=new Header();
		reqHeader.setType(ProtoProtocol.TYPE_CDO);
		CDOMessage reqMessage=new CDOMessage();
		reqMessage.setHeader(reqHeader);
		reqMessage.setBody(proto.build());
		reqMessage.setFiles(files);
		
		
        channel.writeAndFlush(reqMessage);
        calls.put(callId, call);
        boolean interrupted = false;
        synchronized (call) {
          while (!call.done()) {
            try {
              call.wait();                 
            } catch (InterruptedException ie) {
              // save the fact that we were interrupted
              interrupted = true;
            }
          }
          if (interrupted) {
            // set the interrupt flag now that we are done waiting
            Thread.currentThread().interrupt();
          } 
         ReferenceCountUtil.release(reqMessage);
         reqMessage=null;
         return call.getRPCResponse();
       }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) {
        channel = ctx.channel();
        clientId=ByteString.copyFrom(UUidGenerator.ClientId.getClientId());
    }
    
    /**
     * 客端设定 15秒是空闲写时,发起心跳检查
     * 服务端在60秒内 read_idea处于空闲状态,则会认为该连接无效，进行关闭
     */
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {// ping message when there is no outbound traffic for 15 seconds  see RPCClient,RPCServerInitializer         	
                case WRITER_IDLE:
        			CDOMessage heartBeat=new CDOMessage();
        			Header header=new Header();
        			header.setType(ProtoProtocol.TYPE_HEARTBEAT_REQ);
        			heartBeat.setHeader(header);
        			ctx.writeAndFlush(heartBeat);
                    break;
                default:
                    break;
            }
        }
    }
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {    
    	if(msg instanceof CDOMessage){   
    		try{
    			CDOMessage cdoMessage=(CDOMessage)msg;
    		
	           if(cdoMessage.getHeader().getType()==ProtoProtocol.TYPE_CDO){
	        		GoogleCDO.CDOProto proto=(GoogleCDO.CDOProto)(cdoMessage.getBody());
	    			int callId=proto.getCallId();
	    			Call call = calls.get(callId);
	    	        calls.remove(callId);    	        
	    	        call.setRPCResponse(new RPCResponse(proto, cdoMessage.getFiles()));		    			
	    		}else if(cdoMessage.getHeader().getType()==ProtoProtocol.TYPE_HEARTBEAT_REQ){
	    			//服务端发起心跳检查,客服端回复心跳
					Header resHeader=new Header();
					resHeader.setType(ProtoProtocol.TYPE_HEARTBEAT_RES);
					CDOMessage resMessage=new CDOMessage();
					resMessage.setHeader(resHeader);
					ctx.writeAndFlush(resMessage);
					if(logger.isDebugEnabled())
			    			logger.debug("client response server heartbeat ["+(InetSocketAddress)ctx.channel().remoteAddress()+"] heart msg:"+msg);
	    		}else if(cdoMessage.getHeader().getType()==ProtoProtocol.TYPE_HEARTBEAT_RES){
		    		//客户端发起心跳，服务端回复心跳
		    	  if(logger.isDebugEnabled())
		    			logger.debug("client receive server["+(InetSocketAddress)ctx.channel().remoteAddress()+"] heart msg:"+msg);
		       }
    		}finally{
    			ReferenceCountUtil.release(msg);
    		}
    	}else{
    		ctx.fireChannelRead(msg);
    	}        
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    	logger.error(cause.getMessage(),cause);
    	ctx.fireExceptionCaught(cause);
    }
      
	
}
