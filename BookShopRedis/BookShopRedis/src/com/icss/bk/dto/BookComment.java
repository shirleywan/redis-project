package com.icss.bk.dto;

public class BookComment {
	private long aid;        //��ţ��õ�ǰʱ��ĺ������Զ�����
	private String uname;    //������
	private String info;     //������Ϣ
	private String pdate;    //����ʱ��
	private int zanNum;      //���޴���	
	
	
	public long getAid() {
		return aid;
	}
	public void setAid(long aid) {
		this.aid = aid;
	}
	public int getZanNum() {
		return zanNum;
	}
	public void setZanNum(int zanNum) {
		this.zanNum = zanNum;
	}
	public String getUname() {
		return uname;
	}
	public void setUname(String uname) {
		this.uname = uname;
	}
	public String getInfo() {
		return info;
	}
	public void setInfo(String info) {
		this.info = info;
	}
	public String getPdate() {
		return pdate;
	}
	public void setPdate(String pdate) {
		this.pdate = pdate;
	}

}
