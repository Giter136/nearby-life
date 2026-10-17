package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TTL;
import static com.hmdp.utils.RedisConstants.SHOP_TYPE_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Autowired
    StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        //1.从 redis 中取数据
        Map<Object, Object> typeMap=stringRedisTemplate.opsForHash().entries(SHOP_TYPE_KEY);
        //2.
        List<ShopType> typeList=new ArrayList<>();
        //3. 如果存在就返回
        if(!typeMap.isEmpty()){
            for(Object typeStr: typeMap.values()){
                ShopType shopType = JSONUtil.toBean(typeStr.toString(), ShopType.class);
                typeList.add(shopType);
            }
        }
        else{
            //4.不存在就查表
            typeList=list();
            //5.存入 redis 中
            for(ShopType shopType:typeList){
                String jsonType= JSONUtil.toJsonStr(shopType);
                stringRedisTemplate.opsForHash().put(SHOP_TYPE_KEY,shopType.getId().toString(),jsonType);
            }
            //6.如果查表还查不到就返回错误
            if(typeList.isEmpty()){
                return Result.fail("数据库错误");
            }
        }
        //7.返回结果
        typeList.sort(Comparator.comparingInt(ShopType::getSort));
        stringRedisTemplate.expire(SHOP_TYPE_KEY, CACHE_SHOP_TTL, TimeUnit.MINUTES);
        return Result.ok(typeList);
    }
}
