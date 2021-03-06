package com.crispy;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.TimeZone;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.maxmind.geoip.LookupService;

public class Net {
	private static LookupService IP_SERVICE;
	
	public static void init() throws FileNotFoundException, IOException {
		File tmpFile = File.createTempFile("GeoIP", "dat");
		IOUtils.copy(Net.class.getResourceAsStream("/GeoIP.dat"),
				new FileOutputStream(tmpFile));
		
		IP_SERVICE = new LookupService(tmpFile);
	}
	
	public static String countryCodeFromIP(String ip) {
		ip = ip.trim();
		if (ip.split("\\.").length == 4)
			return IP_SERVICE.getCountry(ip).getCode();
		else 
			return IP_SERVICE.getCountryV6(ip).getCode();
	}
	
	
	public static String getCountryCodeFromName(String name) {
		Integer index = (Integer) IP_SERVICE.getHashmapcountrynametoindex().get(name);
		if (index == null)
			return null;
		return IP_SERVICE.getCountrycode()[index];
	}

	
	public static String getCountryNameFromCode(String code) {
		Integer index = (Integer) IP_SERVICE.getHashmapcountrycodetoindex().get(code);
		return IP_SERVICE.getCountryname()[index];
	}
	
	public static String[] countries() {
		return IP_SERVICE.getCountrycode();
	}
	
	/**
	 * Return the millisecond offset for the country as computed by averaging.
	 * 
	 * @param countryCode
	 * @return
	 */
	public static int timezoneOffset(String countryCode) {
		if (countryCode.equals("--"))
			return 0;
		String[] zoneIds = com.ibm.icu.util.TimeZone.getAvailableIDs(countryCode);
		int total = 0;
		int count = 0;
		for (String zone : zoneIds) {
			TimeZone tz = TimeZone.getTimeZone(zone);
			total += tz.getRawOffset();
			count++;
		}
		if (count == 0)
			return 0;
		return total / count;
	}
	
	public static String get(String url) throws Exception {
		String data = null;
		DefaultHttpClient httpClient = new DefaultHttpClient();
		HttpGet get = new HttpGet(url);
		HttpResponse response = httpClient.execute(get);
		if (response.getStatusLine().getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				data = EntityUtils.toString(entity);
				EntityUtils.consume(entity);
			}
		} else {
			EntityUtils.consume(response.getEntity());
		}

		return data;
	}
}
