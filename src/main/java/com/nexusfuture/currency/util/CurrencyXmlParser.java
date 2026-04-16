package com.nexusfuture.currency.util;

import com.alibaba.fastjson2.JSON;
import com.nexusfuture.currency.common.CurrencyInfoEnum;
import com.nexusfuture.currency.dto.CurrencyRateDto;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class CurrencyXmlParser {

    /**
     * 解析欧洲央行 XML → 返回 List<CurrencyRate>
     * 可以直接存 Redis
     */
    public static String parseToCurrencyRateList(String xml) {
        List<CurrencyRateDto> list = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList nodes = doc.getElementsByTagName("Cube");

            for (int i = 0; i < nodes.getLength(); i++) {
                Node node = nodes.item(i);
                var currencyAttr = node.getAttributes().getNamedItem("currency");
                var rateAttr = node.getAttributes().getNamedItem("rate");

                if (currencyAttr == null || rateAttr == null) continue;

                String code = currencyAttr.getNodeValue();
                BigDecimal rate = new BigDecimal(rateAttr.getNodeValue());

                // 绑定枚举 → 拿中英文名称
                CurrencyInfoEnum info = CurrencyInfoEnum.fromCode(code);
                if (info == null) continue;

                list.add(new CurrencyRateDto(
                        code,
                        info.getEnglishName(),
                        info.getChineseName(),
                        rate
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return JSON.toJSONString(list);
    }
}