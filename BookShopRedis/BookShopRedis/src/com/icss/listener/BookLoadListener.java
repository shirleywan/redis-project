package com.icss.listener;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import net.sf.json.JSONArray;
import redis.clients.jedis.Jedis;

import com.icss.bk.biz.BookBiz;
import com.icss.bk.dto.BookJson;
import com.icss.bk.entity.TBook;
import com.icss.bk.util.RedisUtil;


public class BookLoadListener implements  ServletContextListener{

	public void contextDestroyed(ServletContextEvent arg0) {
		
	}	
	
	
	public void contextInitialized(ServletContextEvent arg0) {
		Jedis jedis = RedisUtil.getJedis();
		loadAllBooks(jedis);
		setTimeSortBooks(jedis);
		RedisUtil.returnResource(jedis);
	}
	
	/**
	 * ϵͳ����ʱ�������е�ͼ����Ϣ��װ�ص�redis���ݿ���
	 */
	private void loadAllBooks(Jedis jedis){
		BookBiz biz = new BookBiz();
		try {
			List<TBook> books = biz.getAllBooks();	                         //�����ݿ��ж�ȡ����ͼ��   
			Map<String, String> map = new HashMap<String, String>();
			for(TBook bk : books){				
				BookJson bkj = new BookJson();
				bkj.setBname(bk.getBname());
				bkj.setIsbn(bk.getIsbn());
				SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd");				
				bkj.setPdate(sd.format(bk.getPdate()));
				bkj.setPicurl(bk.getPicurl());
				bkj.setPress(bk.getPress());
				bkj.setPrice(bk.getPrice());
				bkj.setPageview(0);
				JSONArray jsonArray = JSONArray.fromObject(bkj); 			
				String bkString = jsonArray.toString();                        //ÿ��������һ��json��		
				map.put(bk.getIsbn(), bkString);
			}			
			jedis.hmset("allBookList",map);                                    //������ͼ�飬����hash map��
		} catch (Exception e) {
			 e.printStackTrace();
		}		
	}
	/**
	 * ��ҳͼ�飬�����ϼ�ʱ�����������ʾ
	 */
	private void setTimeSortBooks(Jedis jedis){
		BookBiz biz = new BookBiz();
		try {
			List<TBook> books = biz.getAllBooks();	                         //�����ݿ��ж�ȡ����ͼ�� 
			for(TBook bk : books){		
				long time = bk.getPdate().getTime();
				jedis.zadd("bkTimeZset", time,bk.getIsbn());
			}			
		} catch (Exception e) {
			 e.printStackTrace();
		}				
	}
}
