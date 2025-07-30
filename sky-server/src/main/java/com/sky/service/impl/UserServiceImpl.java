package com.sky.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.WeChatProperties;
import com.sky.utils.HttpClientUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements com.sky.service.UserService {
    @Autowired
    private WeChatProperties weChatProperties;
    @Autowired
    private UserMapper userMapper;
    public static final String wxRequestUrl="https://api.weixin.qq.com/sns/jscode2session?";
    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @Override

    public User login(UserLoginDTO userLoginDTO) {
        String openid = getopenid(userLoginDTO.getCode());
        //判断openID是否为空  为空则登陆失败

        if(openid==null){
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }
        //判断当前用户是否为新用户  即openID是否存在
        User user = userMapper.getByopenid(openid);

        //注册
        if(user==null){
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
        }

        //返回用户对象
        return user;
    }
    private String getopenid(String code){
        //调用微信接口服务获得openId
        Map<String, String> map=new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret",weChatProperties.getSecret());
        String authorization_code="authorization_code";
        map.put("grant_type",authorization_code);
        map.put("js_code",code);
        String json = HttpClientUtil.doGet(wxRequestUrl, map);
        JSONObject jsonObject = JSON.parseObject(json);
        String openid = jsonObject.getString("openid");
        return openid;

    }
}
