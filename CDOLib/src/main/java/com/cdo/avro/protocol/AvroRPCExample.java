/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cdo.avro.protocol;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.avro.AvroRemoteException;
import org.apache.avro.ipc.NettyServer;
import org.apache.avro.ipc.NettyTransceiver;
import org.apache.avro.ipc.Server;
import org.apache.avro.ipc.specific.SpecificRequestor;
import org.apache.avro.ipc.specific.SpecificResponder;
import org.apache.avro.util.Utf8;

/**
 * Start a server, attach a client, and send a message.
 */
public class AvroRPCExample {
	
    public static class CDORpcProtocol implements CDOProtocol {
		@Override
		public AvroCDO send(AvroCDO CDORequest) throws AvroRemoteException {
			// TODO Auto-generated method stub
//			CDORequest.getFields()
			System.out.println("receive level="+CDORequest.getLevel());
			Map<CharSequence,ByteBuffer> map=CDORequest.getFields();
			for(Iterator<Map.Entry<CharSequence, ByteBuffer>> iterator=map.entrySet().iterator();iterator.hasNext();){
				Entry<CharSequence, ByteBuffer> entry=iterator.next();
				System.out.println("receive map key="+entry.getKey());
				System.out.println("receive map value ="+entry.getValue().getInt());
			} 
			//TODO 处理 
			
			AvroCDO cdoDataRes=new AvroCDO();
			
	        map=new LinkedHashMap<CharSequence,ByteBuffer>();
	        ByteBuffer buffer=ByteBuffer.allocate(4);
	        buffer.putInt(8);
	        buffer.flip();
	        map.put("resKey", buffer);
	        cdoDataRes.setLevel(2);
	        cdoDataRes.setFields(map);
	        
	
			return cdoDataRes;
		}

    }
    
    private static Server server;

    private static void startServer() throws IOException {
        server = new NettyServer(new SpecificResponder(CDOProtocol.class, new CDORpcProtocol()), new InetSocketAddress(65111));
        // the server implements the Mail protocol (MailImpl)
    }

    public static void main(String[] args) throws IOException {
        System.out.println("Starting server");
        // usually this would be another app, but for simplicity
        startServer();
        System.out.println("Server started");

        NettyTransceiver client = new NettyTransceiver(new InetSocketAddress(65111));
        // client code - attach to the server and send a message
        CDOProtocol proxy = (CDOProtocol) SpecificRequestor.getClient(CDOProtocol.class, client);
        System.out.println("Client built, got proxy");

        // fill in the Message record and send it
        Map<CharSequence,ByteBuffer> map=new LinkedHashMap<CharSequence,ByteBuffer>();
        ByteBuffer buffer=ByteBuffer.allocate(4);
        buffer.putInt(5);
        buffer.flip();
        map.put("key", buffer);
        long startTime=System.nanoTime();
        AvroCDO cdoDataReq=new AvroCDO();
        cdoDataReq.setLevel(1);
        cdoDataReq.setFields(map);
        System.out.println("create avro object ns="+(System.nanoTime()-startTime));
        System.out.println("Calling proxy.send with message:  " + cdoDataReq.getFields());      
        AvroCDO cdoDataRes=proxy.send(cdoDataReq);
    	for(Iterator<Map.Entry<CharSequence, ByteBuffer>> iterator=cdoDataRes.getFields().entrySet().iterator();iterator.hasNext();){
    			Entry<CharSequence, ByteBuffer> entry=iterator.next();
    			System.out.println("res map key="+entry.getKey());
    			System.out.println("res map value ="+entry.getValue().getInt());
    	}         		
        // cleanup
        client.close();
        server.close();
    }
	
}