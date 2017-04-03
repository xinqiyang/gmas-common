package com.gmasdata.common;


import org.hashids.Hashids;

import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Created by xinqiyang on 2017/01/19.
 *
 *
 <dependency>
 <groupId>org.hashids</groupId>
 <artifactId>hashids</artifactId>
 <version>1.0.1</version>
 </dependency>
 mac地址+时间戳+原子自增
 * http://www.cnblogs.com/ELMND/p/4863577.html
 */
public class LongID {
    private static final Charset utf8Charset = Charset.forName("utf-8");
    private static Pattern ptn = Pattern.compile("([0-9]{1})_([0-9]{1,2})");
    private static final AtomicInteger atomicId = new AtomicInteger(1);
    private static final int APP_ID_INC = 1000000;
    private static Long appId = Long.valueOf(101 * APP_ID_INC);
    private static String LONG_SALT = "LONGIDSalt";

    static {
        //初始化appId,默认没有配置，为mac地址crc32计算值
        initAppId(null);
    }

    /*
     *根据配置的ID，做解析，配置示例：
     *appId=IdcId_HostId，
     *例如：appId=1_01,appId=1_02;appId=2_01,appId=2_02;
     * */
    public static void initAppId(String cfgAppId) {
        appId = parseAppId(cfgAppId);
        if (0 == appId) {
            appId = generateRandId();
        }
    }

    private static Long parseAppId(String cfgAppId) {
        try {
            if (null == cfgAppId) {
                return 0L;
            }

            Matcher matcher = ptn.matcher(cfgAppId);
            if (matcher.find()) {
                String idcId = matcher.group(1);
                int nIdcId = Integer.parseInt(idcId);
                String hostId = matcher.group(2);
                int nHostId = Integer.parseInt(hostId);
                int appId = nIdcId * 100 + nHostId;
                return Long.valueOf(appId * APP_ID_INC);
            }
        } catch (Exception e) {
            //ignore
        }
        return 0L;
    }

    private static Long generateRandId() {
        String mac = UUID.randomUUID().toString();
        try {
            String tmpMac = getMacAddress();
            if (null != tmpMac) {
                mac = tmpMac;
            }
        } catch (Exception e) {
            //ignore
        }
        Long tmpRst = getChecksum(mac);
        if (tmpRst < 999 && tmpRst > 0) {
            return tmpRst * APP_ID_INC;
        }
        //大于999，取余数
        Long mod = tmpRst % 999;
        if (mod == 0) {
            //不允许取0
            mod = 1L;
        }
        return mod * APP_ID_INC;
    }

    private static String getMacAddress() throws Exception {
        Enumeration<NetworkInterface> ni = NetworkInterface.getNetworkInterfaces();
        while (ni.hasMoreElements()) {
            NetworkInterface netI = ni.nextElement();
            if (null == netI) {
                continue;
            }
            byte[] macBytes = netI.getHardwareAddress();
            if (netI.isUp() && !netI.isLoopback() && null != macBytes && macBytes.length == 6) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0, nLen = macBytes.length; i < nLen; i++) {
                    byte b = macBytes[i];
                    //与11110000作按位与运算以便读取当前字节高4位
                    sb.append(Integer.toHexString((b & 240) >> 4));
                    //与00001111作按位与运算以便读取当前字节低4位
                    sb.append(Integer.toHexString(b & 15));
                    if (i < nLen - 1) {
                        sb.append("-");
                    }
                }
                return sb.toString().toUpperCase();
            }
        }
        return null;
    }

    /**
     * 获取对应的CRC32校验码
     *
     * @param input 待校验的字符串
     * @return 返回对应的校验和
     */
    private static Long getChecksum(String input) {
        if (null == input) {
            return 0L;
        }
        byte[] data = input.getBytes(utf8Charset);
        CRC32 crc32 = new CRC32();
        for (byte b : data) {
            crc32.update(b);
        }
        return crc32.getValue();
    }

    /**
     * 获取随机数,加大随机数位数，是为了防止高并发，且单个并发中存在循环获取ID的场景
     * 如果您的应用并发有200以上，且每个并发中都存在循环调用获取ID的场景，可能会发生ID冲突
     * 对应的解决方法是：在循环逻辑中加入休眠1-5ms的逻辑
     *
     * @return
     */
    private static int getRandNum() {
        int num = atomicId.getAndIncrement();
        if (num >= 999999) {
            atomicId.set(0);
            return atomicId.getAndIncrement();
        }
        return num;
    }

    public static Long getId() {
        return Long.valueOf(getBasicId());
    }

    private static long getBasicId() {
        return (System.currentTimeMillis() / 1000) * 1000000 + appId + getRandNum();
    }

    public static String encodeId() {
        Hashids hashids = new Hashids(LONG_SALT);
        String hash = hashids.encode(getId());
        return hash;
    }

    //get encode id
    public static String encodeId(Long id) {
        Hashids hashids = new Hashids(LONG_SALT);
        return hashids.encode(id);
    }

    public static Long decodeId(String hash) {
        Hashids hashids = new Hashids(LONG_SALT);
        long[] numbers = hashids.decode(hash);
        return numbers[0];
    }
}
