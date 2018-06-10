package com.icss.bk.biz;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;

import com.icss.bk.dao.BookDao;
import com.icss.bk.dto.BookComment;
import com.icss.bk.dto.BookJson;
import com.icss.bk.entity.TBook;
import com.icss.bk.util.Log;
import com.icss.bk.util.RedisUtil;

public class BookBiz {
	
	/**
	 * ��ָ�������۵���(���ص��޺������)
	 * ���۵����Ǿֲ�ˢ�£�ʹ�ô�ͳ���ݿ⿪���ϴ�
	 * �ȷ����id���ҵ�����list����Ҫÿ�����۶����Լ���id��������������zanNum���޸�
	 * ����ҳ��һ���Ƕ���ҳ�棬������ԭʼҳ���У��ٶȽ���
	 * ������ltrim�����ȡ���»�����������
	 * @throws Exception
	 */
	public int zanBookComment(String isbn ,long aid) throws Exception{ //BookComment���а������޴���zanNum���ԣ����۵��޾��Ǹ���zanNum����
		int zanNum = 0;
		
		Jedis jedis = RedisUtil.getJedis();
		List<String>  bcList =  jedis.lrange("pl-"+isbn, 0, -1);//�ҵ�����list
		for(int i=0;i<bcList.size();i++){
			String bc = bcList.get(i); //ȡ������list�е�ÿһ������
			String bcString = bc.substring(1, bc.length()-1);
			JSONObject jsonObject = JSONObject.fromObject(bcString); //תΪjson����
			BookComment bcJson  = (BookComment) JSONObject.toBean(jsonObject,BookComment.class); //תΪBookComment����
			if(aid == bcJson.getAid()){        //�Ƚ�aid�Ͳ�����aid�Ƿ�һ�£����һ�����1����д
				zanNum = bcJson.getZanNum() + 1;//һ�£���һ
				bcJson.setZanNum(zanNum); //����zanNumֵ
				JSONArray jsonArray = JSONArray.fromObject(bcJson); 			
				String bcNewString = jsonArray.toString();			
				jedis.lset("pl-"+isbn,i,bcNewString);     //���µ��ַ����滻ԭ�����ݣ���д�������Զ�����ָ��λ�õ�ֵ�����滻
				break;
			}			
		}
			RedisUtil.returnResource(jedis);
		
		return zanNum; //���ص�������----ʵ�־ֲ�ˢ��
	}
	
	/**
	 * ��ȡָ��ͼ���������Ϣ
	 * @throws Exception
	 */
	public List<BookComment> getBookComments(String isbn ) throws Exception{
		List<BookComment> comms = new ArrayList<BookComment>();
		
        Jedis jedis = RedisUtil.getJedis();
		List<String>  bcList =  jedis.lrange("pl-"+isbn, 0, -1); //��ȡĳ����������б�����ʾ�Ƿ���������
        for(String bc : bcList){
        	String bcString = bc.substring(1, bc.length()-1); //��һȡ����substring()��ȡ�ַ�����һ������
			JSONObject jsonObject = JSONObject.fromObject(bcString);
			BookComment bcJson  = (BookComment) JSONObject.toBean(jsonObject,BookComment.class); //ת��BookComment����
			comms.add(bcJson); //��ӵ��´�����list�У������е���������
        }        
		RedisUtil.returnResource(jedis);//�ͷ���Դ
		
		return comms; //����list
	}
	
	/**
	 * ��ָ����ͼ���������
	 * �������ʹ������ʹ��list����β��һ��һ��������ۣ�
	 * ��redis�У�ʹ��"pl-"+isbn����ÿ���������keyֵ
	 * @param isbn    
	 * @throws Exception
	 */
	public void addBookComment(String isbn,String uname,String info) throws Exception{
		Jedis jedis = RedisUtil.getJedis();
		BookComment bc = new BookComment(); //������Ϣ��BookComment��ÿ�����۶����Լ��������������Ϣ
		bc.setInfo(info);
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		bc.setPdate(sd.format(new Date()));
		bc.setUname(uname);
		bc.setAid(new Date().getTime());//�������յ�ǰʱ�䴴��
		bc.setZanNum(0);//���϶�����ӵ�������Ϣ�����޴���Ĭ��Ϊ0
		JSONArray jsonArray = JSONArray.fromObject(bc); 			
		String bcString = jsonArray.toString();      //ÿ�����ۣ�����һ��json��     
		jedis.rpush("pl-"+isbn, bcString);     //ÿ��������ۣ�д����Եļ����У������id+ǰ׺������������list�����Ȿ����������
		RedisUtil.returnResource(jedis); //�γ��Ȿ������۶��У����۵Ķ�����Ϣת��json������list�С�
	}
	
	/**
	 * ��ָ����ͼ�飬����һ��page view
	 * @param isbn
	 * @throws Exception
	 */
	public void addBookPageView(String isbn) throws Exception{
		Jedis jedis = RedisUtil.getJedis();	
		jedis.zincrby("bkpv", 1, isbn);          		//��isbn��score��1,������û�о��Զ����һ��		
		RedisUtil.returnResource(jedis);		
	}
	
	/**
	 * ��ȡҳ����ʴ���(�����ʴ����ɴ�С����)
	 * @param top
	 * @throws Exception
	 */
	public List<BookJson> getBookPageView() throws Exception{
		List<BookJson> books = new ArrayList<BookJson>();
		
		Jedis jedis = RedisUtil.getJedis();
		Map<String,String> allBooks =  jedis.hgetAll("allBookList");
		
		Set<String> isbns = jedis.zrevrange("bkpv", 0, -1);	
		Set<Tuple> bkpv = jedis.zrevrangeWithScores("bkpv", 0, -1);
		for(Tuple tuple : bkpv){			
			TBook bk = new TBook();
			String bkString = allBooks.get(tuple.getElement());                //����ISBN��redis����ȡBook����
			String bkNew = bkString.substring(1, bkString.length()-1);
			JSONObject jsonObject = JSONObject.fromObject(bkNew);
			BookJson bkJson  = (BookJson) JSONObject.toBean(jsonObject,BookJson.class); 
			bkJson.setPageview((int)tuple.getScore());
			books.add(bkJson);
		}		
		RedisUtil.returnResource(jedis);
		
		return  books;
	}
	
	
	/**
	 * ��redis����ȡ����ͼ����Ϣ
	 * @return
	 * @throws Exception
	 */
	public List<BookJson> getTimeSortBooks() throws Exception{
		
		List<BookJson> bkList = new ArrayList<BookJson>();
		
		Jedis jedis = RedisUtil.getJedis();	
		
		Set<String> isbns = jedis.zrevrange("bkTimeZset", 0, -1);    //����������������
		for(String isbn : isbns){
			String bkstring = jedis.hget("allBookList", isbn);
			String bkNew = bkstring.substring(1, bkstring.length()-1);
			JSONObject jsonObject = JSONObject.fromObject(bkNew);
			BookJson bkJson  = (BookJson) JSONObject.toBean(jsonObject,BookJson.class); 
			bkList.add(bkJson);			
		}		
		RedisUtil.returnResource(jedis);		
		
		return bkList;		
	}	
	
	/**
	 * ��ҳ��ʾ������ͼ����Ϣ
	 * @return
	 * @throws Exception
	 */
	public List<TBook> getAllBooks() throws Exception{
		List<TBook> books = null;
		
		BookDao dao = new BookDao();
		try {
			books = dao.getAllBooks();	
		} catch (Exception e) {
			Log.logger.error(e.getMessage());
		}finally{
			dao.closeConnection();
		}
		
		return books;		
	}
	
	/**
	 * 
	 * @param book
	 * @throws Exception
	 */
	public void addBook(TBook book) throws Exception{
		BookDao dao = new BookDao();
		try {
			dao.addBook(book);
		} catch (Exception e) {
			Log.logger.error(e.getMessage());
			throw e;
		}finally{
			dao.closeConnection();
		}		
	}
	
	/**
	 * ����ָ����isbn����ȡ��Ӧ��ͼƬ
	 * @param isbn
	 * @return
	 * @throws Exception
	 */
	public byte[] getBookPic(String isbn) throws Exception{
		byte[] pic = null;
		
		BookDao dao = new BookDao();
		try {
			pic = dao.getBookPic(isbn);			
		} catch (Exception e) {
			Log.logger.error(e.getMessage());
			throw e;
		}finally{
			dao.closeConnection();
		}
		
		return pic;
	}
	
	/**
	 * ���������ȡ�����ϸ��Ϣ(����ͼƬ��Ϣ)
	 * @return
	 * @throws Exception
	 */
	public TBook getBookDetail(String isbn) throws Exception{
		TBook book = null;
		
		BookDao dao = new BookDao();
		try {
			book = dao.getBookDetail(isbn);
		} catch (Exception e) {
			Log.logger.error(e.getMessage());
			throw e;
		}finally{
			dao.closeConnection();
		}
		
		return book;
	}
	
	/**
	 * �������������Ϣ����ȡ����ͼ����Ϣ
	 * @param isbns
	 * @return
	 * @throws Exception
	 */
	public List<TBook> getBooks(Set<String> isbns) throws Exception{
		List<TBook> books = null;
		
		BookDao dao = new BookDao();
		try {
			if(isbns.size() > 0)
				books = dao.getBooks(isbns);
		} catch (Exception e) {
			e.printStackTrace();
			Log.logger.error(e.getMessage());
			throw e;
		}finally{
			dao.closeConnection();
		}		
		
		return books;
	}

}
