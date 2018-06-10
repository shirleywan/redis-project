package com.icss.bk.biz;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.icss.bk.dao.UserDao;
import com.icss.bk.dto.BuyRecord;
import com.icss.bk.dto.TurnPage;
import com.icss.bk.entity.TUser;
import com.icss.bk.exception.InputNullExcepiton;
import com.icss.bk.util.Log;

public class UserBiz {
	
	/**
	 * ��ѯ�û��Ĺ����¼
	 * @return
	 * @throws Exception
	 */
	public List<BuyRecord> getUserBuyRecord(String uname ,
			                          Date beginDate ,Date endDate,TurnPage tp) throws Exception{
		List<BuyRecord> records = null;
		UserDao dao = new UserDao();
		try {
			records = dao.getUserBuyRecord(uname,beginDate,endDate,tp);	
		} catch (Exception e) {
			throw e;
		}finally{
			dao.closeConnection();
		}
		
	
		return records;
	}
	
	/**
	 * ʹ��������ƣ�����ͼ�鸶��
	 * @param uname
	 * @param allMoney
	 * @param books
	 * @throws Exception
	 */
	public void buyBooks(String uname,double allMoney,Map<String,Integer> shopCar) throws Exception{
		
		UserDao dao = new UserDao();	
		try {			
			dao.beginTransaction();                         //��ʼ����			
			dao.updateUserAccount(uname, -allMoney);		
			dao.addBuyRecord(uname, allMoney, shopCar);
			dao.commit();                                   //���쳣�����ύ����
		} catch (Exception e) {
			e.printStackTrace();
			dao.rollback();                                 //���쳣���ع�
			Log.logger.error(e.getMessage());
			throw e;
		}finally{
			dao.closeConnection();
		}
		
	}
	
	
	
	/**
	 * ��¼�ɹ�������TUser����
	 * @param uname  
	 * @param pwd
	 * @return
	 */
	public TUser login(String uname,String pwd) throws Exception{
		TUser user = null;
		
		if(uname != null && pwd != null && !uname.equals("") && !pwd.equals("") ){
		    UserDao dao = new UserDao();
		    try {
		    	user = dao.login(uname, pwd);	
			} catch (Exception e) {				
				Log.logger.error(e.getMessage());
				throw e;
			}finally{
				dao.closeConnection();                  //���ݿ�����ڴ˴��ر�
			}
		    	
		    
		}else{
			throw new InputNullExcepiton("�û��������벻��Ϊ��");			
		}
		return user;
	}

	
}
