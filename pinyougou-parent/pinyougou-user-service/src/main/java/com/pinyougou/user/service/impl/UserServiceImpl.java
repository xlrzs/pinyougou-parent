package com.pinyougou.user.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.Session;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.MessageCreator;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.pinyougou.mapper.TbUserMapper;
import com.pinyougou.pojo.TbUser;
import com.pinyougou.user.service.UserService;

import entity.PageResult;

/**
 * 服务实现层
 * @author Administrator
 *
 */
@Service
public class UserServiceImpl implements UserService{

	@Autowired
	private TbUserMapper userMapper;
	
	/**
	 * 查询全部
	 */
	@Override
	public List<TbUser> findAll() {
		return userMapper.selectByExample(null);
	}

	/**
	 * 按分页查询
	 */
	@Override
	public PageResult findPage(int pageNum, int pageSize) {
		PageHelper.startPage(pageNum, pageSize);		
		Page<TbUser> page=   (Page<TbUser>) userMapper.selectByExample(null);
		return new PageResult(page.getTotal(), page.getResult());
	}

	/**
	 * 增加
	 */
	@Override
	public void add(TbUser user) {
		
		user.setCreated(new Date());//用户注册时间
		user.setUpdated(new Date());//修改时间
		user.setSourceType("1");//注册来源		
		user.setPassword( DigestUtils.md5Hex(user.getPassword()));//密码加密
		
		userMapper.insert(user);		
	}

	
	/**
	 * 修改
	 */
	@Override
	public void update(TbUser user){
		userMapper.updateByPrimaryKey(user);
	}	
	
	/**
	 * 根据ID获取实体
	 * @param id
	 * @return
	 */
	@Override
	public TbUser findOne(Long id){
		return userMapper.selectByPrimaryKey(id);
	}

	/**
	 * 批量删除
	 */
	@Override
	public void delete(Long[] ids) {
		for(Long id:ids){
			userMapper.deleteByPrimaryKey(id);
		}		
	}
	

	@Override
	public PageResult findPage(TbUser user, int pageNum, int pageSize) {
		// TODO Auto-generated method stub
		return null;
	}

	@Autowired
	private RedisTemplate<String , Object> redisTemplate;	
	
	
	/**
	 * 生成短信验证码
	 */
	public void createSmsCode(final String phone){		
		//生成6位随机数
		final String code =  (long) (Math.random()*1000000)+"";
		System.out.println("验证码："+code);
		//存入缓存
		redisTemplate.boundHashOps("smscode").put(phone, code);
		//发送到activeMQ		
		jmsTemplate.send(smsDestination, new MessageCreator() {			
			@Override
			public Message createMessage(Session session) throws JMSException {	
				MapMessage mapMessage = session.createMapMessage();			
				mapMessage.setString("mobile", phone);//手机号
				mapMessage.setString("template_code", "SMS_149101366");//模板编号
				mapMessage.setString("sign_name", "雷鸣SD");//签名				
				Map m=new HashMap<>();
				m.put("code", code);				
				mapMessage.setString("param", JSON.toJSONString(m));//参数
				return mapMessage;
			}
		});				
	}

	/**
	 * 判断验证码是否正确
	 */
	public boolean  checkSmsCode(String phone,String code){
		//得到缓存中存储的验证码
		String sysCode = (String) redisTemplate.boundHashOps("smscode").get(phone);
		if(sysCode==null){
			return false;
		}
		if(!sysCode.equals(code)){
			return false;
		}
		return true;		
	}
	@Autowired
	private JmsTemplate jmsTemplate;	
	@Autowired
	private Destination smsDestination;	

	@Value("${template_code}")
	private String template_code;
	
	@Value("${sign_name}")
	private String sign_name;

}
