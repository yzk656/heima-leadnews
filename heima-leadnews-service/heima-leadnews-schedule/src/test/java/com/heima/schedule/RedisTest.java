package com.heima.schedule;

import com.heima.common.redis.CacheService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Set;

@SpringBootTest(classes = ScheduleApplication.class)
@RunWith(SpringRunner.class)
public class RedisTest {

    @Autowired
    private CacheService cacheService;

    @Test
    public void testList() {
        //在list左边添加元素
        cacheService.lLeftPush("list_001","hello redis");

        //在list右边获取元素并且删除
        String s = cacheService.lRightPop("list_001");
        System.out.println(s);
    }

    @Test
    public void zSet() {
        //添加数据到zSet中
/*        cacheService.zAdd("zset_key_001","hello zSet1",1000);
        cacheService.zAdd("zset_key_001","hello zSet2",7777);
        cacheService.zAdd("zset_key_001","hello zSet3",8888);
        cacheService.zAdd("zset_key_001","hello zSet4",9999);*/

        //按照分值获取数据
        Set<String> zsetKey001 = cacheService.zRangeByScore("zset_key_001", 0, 8888);
        System.out.println(zsetKey001);
    }
}
