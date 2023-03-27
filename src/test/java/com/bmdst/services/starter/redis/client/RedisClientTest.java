package com.bmdst.services.starter.redis.client;

import com.bmdst.services.starter.redis.TestApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {TestApplication.class})
public class RedisClientTest {
    @Resource
    private RedisClient redisClient;

    @Test
    public void set_Get_Exists() {
        //Arrange
        String key = "name";
        String testValue = "zhangsan";

        //Act
        redisClient.set(key, testValue);
        String value = (String) redisClient.get(key);

        //Assert
        assertEquals(testValue, value);

        //Act
        boolean isExists = redisClient.exists(key);

        //Assert
        assertTrue(isExists);
    }

    @Test
    public void multiSet() {
        //Arrange
        Map<String, Object> map = new HashMap<>();
        map.put("age", 18);
        map.put("age1", 18);
        map.put("age2", 18);
        map.put("age3", 18);

        //Act
        redisClient.multiSet(map);
        boolean isExists = redisClient.exists("age3");

        //Assert
        assertTrue(isExists);
    }

    @Test
    public void multiGet() {
        //Arrange
        String key1 = "key1";
        String key2 = "key2";
        String value1 = "zhangsan";
        String value2 = "lisi";
        redisClient.set(key1, value1);
        redisClient.set(key2, value2);

        //Act
        List<Object> list = redisClient.multiGet(key1, key2);

        //Assert
        assertEquals(2, list.size());

        //Act
        String value = (String) redisClient.get(key1);

        //Assert
        assertEquals(value1, value);
    }

    @Test
    public void expire() throws InterruptedException {
        //Arrange
        String id = "id";
        redisClient.set(id, "zhangsan");

        //Act
        redisClient.expire(id, 1, TimeUnit.SECONDS);

        Thread.sleep(2000);

        //Assert
        assertFalse(redisClient.exists(id));

    }

    @Test
    public void delete_FlushCacheDb() {
        //Arrange
        String num1 = "num1";
        String num2 = "num2";
        redisClient.set(num1, 1);
        redisClient.set(num2, 2);

        //Act
        boolean isExists = redisClient.exists(num1);

        //Assert
        assertTrue(isExists);

        //Act
        redisClient.delete(num1);

        //Assert
        assertFalse(redisClient.exists(num1));
    }

    @Test
    public void lPush_size_rPop() {
        String key = "redis-test";
        String value = "test";

        //Act
        long result = redisClient.lPush(key, Collections.singleton(value));

        //Assert
        assertEquals(1, result);

        //Act
        Long result2 = redisClient.size(key);

        //Assert
        assertEquals(1, result2.intValue());

        //Act
        String result3 = String.valueOf(redisClient.rPop(key));

        //Assert
        assertEquals(value, result3);
    }

    @Test
    public void hSet_hHasKey_hGet_hIncrement_hDel() {
        //Arrange
        String key = "hashTest";
        String field1 = "hashField1";
        Integer value1 = 1;

        //Act
        redisClient.hSet(key, field1, value1);
        Integer result1 = (Integer) redisClient.hGet(key, field1);

        //Assert
        assertTrue(redisClient.hHasKey(key, field1));
        assertEquals(value1, result1);

        //Act
        Long result2 = redisClient.hIncrement(key, field1, 1);

        //Assert
        assertEquals(2, result2.intValue());
        assertEquals(2, (int) redisClient.hGet(key, field1));

        //Act
        Long result3 = redisClient.hDel(key, field1);

        //Assert
        assertEquals(1, result3.intValue());
        assertFalse(redisClient.hHasKey(key, field1));
        redisClient.delete(key);
        assertFalse(redisClient.exists(key));
    }

    //@Test
    public void scan() {
        //Arrange
        String key1 = "test10008-customer200003-date20210119";
        String key2 = "test10008-customer200003-date20210120";
        String key3 = "test10007-customer200003-date20210120";
        String key4 = "test10009-customer200003-date20210120";
        redisClient.set(key1, 1);
        redisClient.set(key2, 1);
        redisClient.set(key3, 1);
        redisClient.set(key4, 1);

        //Act
        Set<String> result = redisClient.scan("test10008-customer*-date*");

        //Assert
        assertEquals(2, result.size());
        assertTrue(result.contains(key1));
        assertTrue(result.contains(key2));

        redisClient.delete(key1, key2, key3, key4);

        Set<String> result2 = redisClient.scan("test*-customer*-date*");
        assertTrue(result2.isEmpty());
    }
}
