package com.cdo.business.rpc.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class RouteManager {

	static Map<String, Integer> routeMap=new HashMap<String, Integer>();
	private static RouteManager instance=null;
	private Logger log=Logger.getLogger(RouteManager.class);
	public static  synchronized RouteManager getInstance()
	{//使用单列
		if(instance==null)
			instance=new RouteManager();
		return instance;
	}
	
	private RouteManager(){
		
	}
	/**
	 * 均匀使用连接
	 * @param serviceName
	 * @param hostList
	 * @return
	 */
	public  RPCClient route(String serviceName,List<String> hostList){
		int total=hostList.size();
		int index=routeMap.get(serviceName)==null?0:routeMap.get(serviceName);
		if(index>=(total-1))
			index=0;
		else
			index++;
		
		String address=hostList.get(index);
		routeMap.put(serviceName, index);
		
		RPCClient rpcClient=RPCClient.getClients().get(address);
		if(rpcClient!=null)
			return rpcClient;
		
		//连接未建立  建立连接
		RPCClient rpClient=new RPCClient(address.split(":")[0],Integer.parseInt(address.split(":")[1]));
		rpClient.init();
		int retry=1;
		while (retry<5) {
			if(rpClient.getHandle()!=null)
				break;
			try {
				Thread.sleep(1000+500*retry);
			} catch (InterruptedException e) {
			}
			retry++;
		}
		return rpClient;
	}
}
