package com.cnsa.sparrow.xss.utils;

import org.apache.commons.lang3.StringUtils;
import org.owasp.validator.html.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * XSS工具类，用于过滤特殊字符
 */
public class XssUtils {

    private static Logger log = LoggerFactory.getLogger(XssUtils.class);

    private static final String ANTISAMY_SLASHDOT_XML = "antisamy-slashdot.xml";
    private static Policy policy;

    static{
        InputStream inputStream = XssUtils.class.getClassLoader().getResourceAsStream(ANTISAMY_SLASHDOT_XML);
        try {
            policy = Policy.getInstance(inputStream);
        } catch (PolicyException e) {
            log.error("Failed to read XSS config file [{}], reason is {}", ANTISAMY_SLASHDOT_XML, e);
        }
        finally {
            if(inputStream != null){
                try{
                    inputStream.close();
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 跨站攻击语句过滤
     * @param str 带过滤的参数
     * @param ignoredList  忽略过滤的参数列表
     * @return
     */
    public static String xssClean(String str, List<String> ignoredList){
        if(isIgnored(str, ignoredList)){
            return str;
        }
        AntiSamy antiSamy = new AntiSamy();
        try{
            final CleanResults cr = antiSamy.scan(str, policy);
            cr.getErrorMessages().forEach(log::debug);
            return cr.getCleanHTML();
        }catch (ScanException e) {
            log.error("scan failed armter is [" + str + "]", e);
        } catch (PolicyException e) {
            log.error("antisamy convert failed  armter is [" + str + "]", e);
        }
        return str;
    }


    public static boolean isIgnored(String str, List<String> ignoredList){
        if(StringUtils.isBlank(str)){
            return true;
        }
        if(CollectionUtils.isEmpty(ignoredList)){
            return false;
        }
        for(String item : ignoredList){
            if(str.contains(item)){
                return true;
            }
        }
        return false;
    }

}
