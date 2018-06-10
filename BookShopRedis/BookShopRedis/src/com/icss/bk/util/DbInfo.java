package com.icss.bk.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class DbInfo {
	
	private static DbInfo dbinfo;                  //��������	
	
	 private String dbUrl;
	 private String dbdriver;
	 private String uname;
	 private String pwd; 
	

	
	public String getDbUrl() {
		return dbUrl;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public String getDbdriver() {
		return dbdriver;
	}

	public void setDbdriver(String dbdriver) {
		this.dbdriver = dbdriver;
	}

	public String getUname() {
		return uname;
	}

	public void setUname(String uname) {
		this.uname = uname;
	}

	public String getPwd() {
		return pwd;
	}


	public void setPwd(String pwd) {
		this.pwd = pwd;
	}
	
	private DbInfo(){
		//���캯��˽�л�����ֹ�ⲿ����new DbInfo()		
	}
	
	//ʹ�þ�̬��������������ʵ��
	public static DbInfo instance(){
		if(dbinfo == null){
			dbinfo = new DbInfo();
			getProperties();
		}
		
		return dbinfo;
	}
	

	/**
	 * ��ȡ�����ļ�
	 */
	private static void getProperties() {
		FileInputStream fis = null;
		
		try {
			String uString = DbInfo.class.getResource("/").toURI().getPath() +"db.properties";
			File file = new File(uString);
			Properties properties = new Properties();
			
			fis = new FileInputStream(file);;
			properties.load(fis);
			dbinfo.setDbdriver(properties.getProperty("dbdriver").trim());
			dbinfo.setDbUrl(properties.getProperty("dbURL").trim());
			dbinfo.setUname(properties.getProperty("uname").trim());
			dbinfo.setPwd(properties.getProperty("pwd").trim());
		} catch (FileNotFoundException e) {
			Log.logger.error(e.getMessage());
		} catch (IOException e) {
			Log.logger.error(e.getMessage());
		} catch (Exception e) {
			Log.logger.error(e.getMessage());
		}finally{
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException e) {
					Log.logger.error(e.getMessage());
				}
			}			
		}
	}	
}


