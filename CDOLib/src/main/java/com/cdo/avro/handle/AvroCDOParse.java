package com.cdo.avro.handle;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.cdo.avro.protocol.AvroCDO;
import com.cdoframework.cdolib.data.cdo.CDO;
import com.cdoframework.cdolib.data.cdo.Field;
import com.cdoframework.cdolib.data.cdo.ParseCDOBuffer;

public class AvroCDOParse extends ParseCDOBuffer{
	
    public static AvroCDOParse AvroParse;
    static{
    	AvroParse=new AvroCDOParse();
   }
	protected AvroCDOParse(){		
	}
	
	public  CDO parse(AvroCDO avro){		
		CDO cdo=new CDO();
		avro2CDO(cdo,avro.getFields());					
		return cdo;
	} 
	
	private  void avro2CDO(CDO cdo,Map<CharSequence,ByteBuffer> fieldsMap){
		 for(Iterator<Map.Entry<CharSequence,ByteBuffer>> iterator=fieldsMap.entrySet().iterator();iterator.hasNext();){
			 Entry<CharSequence, ByteBuffer> entry=iterator.next();
			 String key=entry.getKey().toString();
			 ByteBuffer buffer=entry.getValue();
			 parseHierarchicCDO(cdo,buffer,key);
		  }	    	
		 
		for(Iterator<Map.Entry<String,Field>> iterator=cdo.iterator();iterator.hasNext();){
	    		Entry<String,Field> entry=iterator.next();
	    		list2array(cdo, entry.getKey(), entry.getValue());    		
	    } 
	}
			
}
