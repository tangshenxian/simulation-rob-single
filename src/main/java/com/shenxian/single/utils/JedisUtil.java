package com.shenxian.single.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.params.SetParams;

/**
 * @author: shenxian
 * @date: 2021/12/28 9:51
 */
@Component
public class JedisUtil {

    @Autowired
    private JedisPool jedisPool;

    public boolean setNx(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            if (jedis == null) {
                return false;
            }
            SetParams setParams = new SetParams();
            setParams.nx().px(1000 * 60);
            return "ok".equalsIgnoreCase(jedis.set(key, value, setParams));
        } catch (Exception ex) {

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return false;
    }

    public int delNx(String key, String value) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            if (jedis == null) {
                return 0;
            }
            StringBuilder sbScript = new StringBuilder();
            // if redis.call('get','orderkey')=='1111' then return redis.call('del','orderkey') else return 0 end
            sbScript.append("if redis.call('get','").append(key).append("')").append("=='").append(value).append("'")
                    .append(" then ")
                    .append("   return redis.call('del','").append(key).append("')")
                    .append(" else ")
                    .append("   return 0")
                    .append(" end");
            return Integer.parseInt(jedis.eval(sbScript.toString()).toString());
        } catch (Exception ex) {

        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
        return 0;
    }

    @Bean
    public JedisPool getJedisPool() {
        return new JedisPool("127.0.0.1", 6379);
    }

}
