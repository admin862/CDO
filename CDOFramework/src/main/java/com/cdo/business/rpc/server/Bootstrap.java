package com.cdo.business.rpc.server;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;

import org.apache.log4j.Logger;

import com.cdo.business.rpc.client.RPCClient;
import com.cdo.util.resource.GlobalResource;



public final class Bootstrap {

    /**
     * Daemon object used by main.
     */
	private static Logger log=Logger.getLogger(Bootstrap.class);
	
	private static Bootstrap daemon = null;

	private Object catalinaDaemon = null;
    
	 ClassLoader catalinaLoader = null;
    
    public void init(String homePath) throws IOException, Exception{
    	
    	String dir=homePath+"/lib";
    	File directory=new File(dir);
		directory = directory.getCanonicalFile();
	    String fileUrlString = directory.toURI().toString();
	    fileUrlString = fileUrlString.replaceAll("!/", "%21/");
		URL url = new URL(fileUrlString);
		final URL[] urls={url};
		catalinaLoader=AccessController.doPrivileged(
                new PrivilegedAction<URLClassLoader>() {
                    @Override
                    public URLClassLoader run() {                        
                            return new URLClassLoader(urls);
                    }
                });
        Thread.currentThread().setContextClassLoader(catalinaLoader);
        Class<?> startupClass =catalinaLoader.loadClass("com.cdo.business.rpc.server.RPCServer");
        catalinaDaemon= startupClass.newInstance();
	  
    }
    
    public void start(String homePath)
            throws Exception {
            if(catalinaDaemon==null ) init(homePath);
            Method method = catalinaDaemon.getClass().getMethod("start", (Class [] )null);
            method.invoke(catalinaDaemon, (Object [])null);
        }
    
   /**
    * 通过socket来关闭服务
    */
	public  void stop(){
		int port=GlobalResource.cdoConfig.getInt("netty.server.port");
		RPCClient rClient=new RPCClient(); 
	 	rClient.stopLocalNettyServer(port);
	}
	
    public void stopLocal()
            throws Exception {
            Method method = catalinaDaemon.getClass().getMethod("stop", (Class [] ) null);
            method.invoke(catalinaDaemon, (Object [] ) null);
        }
    
    public static void main(String[] args){ 
    	String homePath=args[0];    	
        if (daemon == null) {
            // Don't set daemon until init() has completed
            Bootstrap bootstrap = new Bootstrap();
            try {
				bootstrap.init(homePath);
			} catch (Exception e) {
				log.error("bootstrap init error :"+e.getMessage(), e);				
				return;
			}
            daemon = bootstrap;
        } else {
            // When running as a service the call to stop will be on a new
            // thread so make sure the correct class loader is used to prevent
            // a range of class not found exceptions.
            Thread.currentThread().setContextClassLoader(daemon.catalinaLoader);
        }
        try {
            String command = "stop";
            GlobalResource.bundleInitCDOEnv();          
            if (args.length > 1) {
                command = args[args.length - 1];
            }         
            if (command.equals("start")) {
                daemon.start(homePath);               
            } else if (command.equals("stop")) {
                daemon.stop();                             
            } else {
                log.warn("Bootstrap: command \"" + command + "\" does not exist.");
            }
           
        } catch (Throwable t) {
        	log.fatal("bootstrap throwable :"+t.getMessage(), t);	
            System.exit(1);
        }    	
        
    }
}
