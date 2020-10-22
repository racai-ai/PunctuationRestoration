package ro.racai.base;

import java.util.HashMap;
import java.util.Map;

public class Token implements Cloneable {

	private HashMap<String,String> hashMap=new HashMap<String,String>(20);
	
	public Token() {
		
	}
	
	public Token clone() {
		Token t= new Token();
		for(Map.Entry<String, String> entry:hashMap.entrySet() ) {
			t.setByKey(entry.getKey(), entry.getValue());
		}
		return t;
	}
	
	public String getByKey(String key) {
		if(hashMap.containsKey(key))return hashMap.get(key);
		return null;
	}
	
	public void setByKey(String key, String value) {
		hashMap.put(key, value);
	}
	
	public String toString() {
		StringBuilder sb=new StringBuilder(1000);
		for(String key:hashMap.keySet()) {
			if(sb.length()>0)sb.append(",");
			sb.append(key+"="+hashMap.get(key));
		}
		return sb.toString();
	}
	
}
