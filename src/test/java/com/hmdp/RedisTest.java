package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.util.RandomUtil;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_USER_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_USER_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * Program : hm-dianping
 * Author : llj
 * Create : 2024-06-03 16:49
 **/

@SpringBootTest
public class RedisTest {

    @Resource
    private UserServiceImpl userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void creatUsers(){
        int maxUsers = 1000;
        List<String> phones = creatPhones(maxUsers);
        phones.forEach(phone -> {
            User user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            userService.save(user);
        });

    }
    List<String> creatPhones(int maxUsers){
        List<String> phones = new ArrayList<>();

        Random random = new Random();
        StringBuilder phoneNumber = new StringBuilder("1");
        phoneNumber.append(random.nextInt(9) + 3);
        for (int i = 0; i < 9; i++) {
            phoneNumber.append(random.nextInt(10));
        }

        phones.add(phoneNumber.toString());

        for (int i = 1; i < maxUsers; i ++ ){
            phones.add(String.valueOf(Long.parseLong(phoneNumber.toString()) + i));
        }

        return phones;
    }

    @Test
    void usersLogin(){

        int maxUsers = 1000;
        List<String> phones = creatPhones(maxUsers);
        //System.out.println("phones = " + phones);

        phones.forEach(phone -> {
            //用户写入数据库
            User user = new User();
            user.setPhone(phone);
            user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
            userService.save(user);

            //创建token DTO对象
            String token = UUID.randomUUID().toString();
            UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

            //存为hash redis 数据
            Map<String, Object> toMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                    CopyOptions.create().setIgnoreNullValue(true)
                            .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
            stringRedisTemplate.opsForHash().putAll(LOGIN_USER_KEY + token, toMap);
            stringRedisTemplate.expire(LOGIN_USER_KEY + token, -1L, TimeUnit.SECONDS);
        });

    }

    @Test
    void downloadsTokens(){

        String namespace = "login:token:*";
        Set<String> tokens = stringRedisTemplate.keys(namespace);
        String outputFileName = "tokens.txt";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFileName))) {
            assert tokens != null;
            for (String token : tokens) {
                if (token != null) {
                    writer.write(token.substring(namespace.length() - 1));
                    writer.newLine();
                }
            }
            System.out.println("Tokens have been written to " + outputFileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
