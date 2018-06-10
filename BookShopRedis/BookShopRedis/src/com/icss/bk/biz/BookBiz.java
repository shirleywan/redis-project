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
	 * 给指定的评论点赞(返回点赞后的数量)
	 * 评论点赞是局部刷新，使用传统数据库开销较大
	 * 先发书的id，找到评论list，需要每条评论都有自己的id，对这条评论中zanNum做修改
	 * 评论页面一般是独立页面，不放在原始页面中，速度较慢
	 * 可以用ltrim命令截取最新或者最热评论
	 * @throws Exception
	 */
	public int zanBookComment(String isbn ,long aid) throws Exception{ //BookComment类中包含点赞次数zanNum属性，评论点赞就是更改zanNum属性
		int zanNum = 0;
		
		Jedis jedis = RedisUtil.getJedis();
		List<String>  bcList =  jedis.lrange("pl-"+isbn, 0, -1);//找到评论list
		for(int i=0;i<bcList.size();i++){
			String bc = bcList.get(i); //取出评论list中的每一条评论
			String bcString = bc.substring(1, bc.length()-1);
			JSONObject jsonObject = JSONObject.fromObject(bcString); //转为json类型
			BookComment bcJson  = (BookComment) JSONObject.toBean(jsonObject,BookComment.class); //转为BookComment类型
			if(aid == bcJson.getAid()){        //比较aid和参数的aid是否一致，如果一致则加1，回写
				zanNum = bcJson.getZanNum() + 1;//一致，加一
				bcJson.setZanNum(zanNum); //更新zanNum值
				JSONArray jsonArray = JSONArray.fromObject(bcJson); 			
				String bcNewString = jsonArray.toString();			
				jedis.lset("pl-"+isbn,i,bcNewString);     //用新的字符串替换原有内容，回写操作，对队列中指定位置的值进行替换
				break;
			}			
		}
			RedisUtil.returnResource(jedis);
		
		return zanNum; //返回点赞数量----实现局部刷新
	}
	
	/**
	 * 读取指定图书的评论信息
	 * @throws Exception
	 */
	public List<BookComment> getBookComments(String isbn ) throws Exception{
		List<BookComment> comms = new ArrayList<BookComment>();
		
        Jedis jedis = RedisUtil.getJedis();
		List<String>  bcList =  jedis.lrange("pl-"+isbn, 0, -1); //获取某本书的评论列表，先显示是否已有评论
        for(String bc : bcList){
        	String bcString = bc.substring(1, bc.length()-1); //逐一取出，substring()截取字符串的一个方法
			JSONObject jsonObject = JSONObject.fromObject(bcString);
			BookComment bcJson  = (BookComment) JSONObject.toBean(jsonObject,BookComment.class); //转成BookComment对象
			comms.add(bcJson); //添加到新创建的list中，队列中的所有评论
        }        
		RedisUtil.returnResource(jedis);//释放资源
		
		return comms; //返回list
	}
	
	/**
	 * 给指定的图书添加评论
	 * 添加评论使用链表，使用list；从尾部一个一个添加评论，
	 * 在redis中，使用"pl-"+isbn生成每本书的评论key值
	 * @param isbn    
	 * @throws Exception
	 */
	public void addBookComment(String isbn,String uname,String info) throws Exception{
		Jedis jedis = RedisUtil.getJedis();
		BookComment bc = new BookComment(); //评论信息类BookComment，每条评论都有自己的主键和相关信息
		bc.setInfo(info);
		SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		bc.setPdate(sd.format(new Date()));
		bc.setUname(uname);
		bc.setAid(new Date().getTime());//主键按照当前时间创建
		bc.setZanNum(0);//以上都是添加的评论信息，点赞次数默认为0
		JSONArray jsonArray = JSONArray.fromObject(bc); 			
		String bcString = jsonArray.toString();      //每条评论，生成一个json串     
		jedis.rpush("pl-"+isbn, bcString);     //每本书的评论，写入各自的集合中；用书的id+前缀生成主键，是list对象，这本书的相关评论
		RedisUtil.returnResource(jedis); //形成这本书的评论队列，评论的对象信息转成json，放入list中。
	}
	
	/**
	 * 给指定的图书，增加一次page view
	 * @param isbn
	 * @throws Exception
	 */
	public void addBookPageView(String isbn) throws Exception{
		Jedis jedis = RedisUtil.getJedis();	
		jedis.zincrby("bkpv", 1, isbn);          		//给isbn的score加1,集合中没有就自动添加一条		
		RedisUtil.returnResource(jedis);		
	}
	
	/**
	 * 提取页面访问次数(按访问次数由大到小排序)
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
			String bkString = allBooks.get(tuple.getElement());                //根据ISBN从redis中提取Book对象
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
	 * 从redis中提取所有图书信息
	 * @return
	 * @throws Exception
	 */
	public List<BookJson> getTimeSortBooks() throws Exception{
		
		List<BookJson> bkList = new ArrayList<BookJson>();
		
		Jedis jedis = RedisUtil.getJedis();	
		
		Set<String> isbns = jedis.zrevrange("bkTimeZset", 0, -1);    //按照日期逆向排序
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
	 * 主页显示，所有图书信息
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
	 * 根据指定的isbn，提取对应的图片
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
	 * 根据书号提取书的详细信息(不含图片信息)
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
	 * 根据书的主键信息，提取所有图书信息
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
