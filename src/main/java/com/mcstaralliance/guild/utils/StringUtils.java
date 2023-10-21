package com.mcstaralliance.guild.utils;

import java.util.regex.Pattern;

/* loaded from: Guild-1.2.3-b200614.jar:me/asnxthaony/guild/utils/StringUtils.class */
public class StringUtils {
    public static boolean isAllChinese(String str) {
        Pattern pattern = Pattern.compile("^[一-龥]*$");
        return !pattern.matcher(str).matches();
    }

    public static String join(Object[] array) {
        return org.apache.commons.lang.StringUtils.join(array, " ");
    }
}
