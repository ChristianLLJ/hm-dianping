package com.hmdp.controller;


import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 * 前端控制器
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@RestController
@RequestMapping("/shop-type")
public class ShopTypeController {
    @Resource
    private IShopTypeService typeService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ObjectMapper objectMapper;

    @GetMapping("list")
    public Result queryTypeList() throws JsonProcessingException {

        String typeStr = stringRedisTemplate.opsForValue().get("shopTypes");

        if (StrUtil.isNotBlank(typeStr)) {
            return Result.ok(objectMapper.readValue(typeStr, new TypeReference<List<ShopType>>() {}));
        }

        List<ShopType> typeList = typeService
                .query().orderByAsc("sort").list();

        if (typeList != null) {
            String json = objectMapper.writeValueAsString(typeList);
            stringRedisTemplate.opsForValue().set("shopTypes", json);
        }

        return Result.ok(typeList);
    }
}
