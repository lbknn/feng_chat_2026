package org.example.fengbushi.util;

import org.springframework.stereotype.Component;

/**
 * 雪花算法 - 生成全局唯一ID
 * 用于消息ID生成，保证：
 * 1. 消息不乱序
 * 2. 多服务器不重复
 * 3. 可排序、可分页
 */
@Component
public class SnowflakeIdGenerator {
    
    // 起始时间戳（2024-01-01）
    private static final long EPOCH = 1704067200000L;
    
    // 机器ID所占位数
    private static final long WORKER_ID_BITS = 5L;
    
    // 数据中心ID所占位数
    private static final long DATA_CENTER_ID_BITS = 5L;
    
    // 序列号所占位数
    private static final long SEQUENCE_BITS = 12L;
    
    // 最大值计算
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    
    // 移位计算
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    
    private long workerId;
    private long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    
    public SnowflakeIdGenerator() {
        // 默认使用机器IP后几位作为workerId和dataCenterId
        this.workerId = getDefaultWorkerId();
        this.dataCenterId = getDefaultDataCenterId();
    }
    
    public SnowflakeIdGenerator(long workerId, long dataCenterId) {
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException("Worker ID不能大于" + MAX_WORKER_ID + "或小于0");
        }
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("Data Center ID不能大于" + MAX_DATA_CENTER_ID + "或小于0");
        }
        this.workerId = workerId;
        this.dataCenterId = dataCenterId;
    }
    
    /**
     * 获取下一个ID
     */
    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();
        
        if (timestamp < lastTimestamp) {
            throw new RuntimeException("时钟回拨，拒绝生成ID");
        }
        
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = waitNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
        }
        
        lastTimestamp = timestamp;
        
        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }
    
    /**
     * 生成字符串格式的ID
     */
    public String nextStringId() {
        return String.valueOf(nextId());
    }
    
    private long waitNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
    
    private long getDefaultWorkerId() {
        try {
            String hostAddress = java.net.InetAddress.getLocalHost().getHostAddress();
            int[] ips = java.util.Arrays.stream(hostAddress.split("\\."))
                    .mapToInt(Integer::parseInt).toArray();
            return (ips[2] & 0x1F); // 取第三段IP的后5位
        } catch (Exception e) {
            return 1L;
        }
    }
    
    private long getDefaultDataCenterId() {
        try {
            String hostAddress = java.net.InetAddress.getLocalHost().getHostAddress();
            int[] ips = java.util.Arrays.stream(hostAddress.split("\\."))
                    .mapToInt(Integer::parseInt).toArray();
            return (ips[3] & 0x1F); // 取第四段IP的后5位
        } catch (Exception e) {
            return 1L;
        }
    }
}
