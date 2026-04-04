package com.nexusfuture.currency.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class XmlExchangeRateUtil {

    // 私有构造，禁止实例化
    private XmlExchangeRateUtil() {}

    /**
     * 从欧洲央行 XML 中解析对应货币的汇率
     * @param xml 完整 XML 字符串
     * @param currency 货币代码，如 CNY、THB
     * @return 汇率（1欧元 = ? XX货币）
     */
    public static double getRate(String xml, String currency) {
        // ECB：双引号 <Cube currency="CNY" rate="7.9495"/>；部分样例为单引号
        String regex = "currency=['\"]" + Pattern.quote(currency) + "['\"]\\s+rate=['\"]([0-9.]+)['\"]";
        Matcher matcher = Pattern.compile(regex).matcher(xml);
        if (matcher.find()) {
            return Double.parseDouble(matcher.group(1));
        }
        throw new IllegalArgumentException("未找到货币汇率：" + currency);
    }
}