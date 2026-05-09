package com.nexusfuture.currency.client;

import com.nexusfuture.currency.config.BocCrawlerProperties;
import com.nexusfuture.currency.entity.BocExchangeRate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 中国银行外汇牌价爬虫客户端
 * <p>
 * 使用 Jsoup 解析 HTML，提取外汇牌价表格数据
 * 体现了对 HTTP 协议、HTML 解析、反爬策略的理解
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BocCrawlerClient {

    private final BocCrawlerProperties properties;
    private final RestTemplate restTemplate;

    /**
     * 抓取并解析中国银行外汇牌价
     *
     * @return 解析后的汇率列表
     */
    public List<BocExchangeRate> crawlAndParse() {
        log.info("🕷️ [BOC Crawler] 开始抓取中国银行外汇牌价（支持分页）...");
        
        List<BocExchangeRate> allRates = new ArrayList<>();
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= properties.getMaxAttempts(); attempt++) {
            try {
                log.info("🕷️ [BOC Crawler] 第 {} 次尝试抓取...", attempt);
                
                int totalPages = fetchTotalPages();
                log.info("📄 [BOC Crawler] 检测到总页数: {}", totalPages);
                
                for (int page = 1; page <= totalPages; page++) {
                    log.info("📄 [BOC Crawler] 正在抓取第 {}/{} 页...", page, totalPages);
                    
                    String html = fetchHtmlWithAntiBot(page);
                    
                    if (html == null || html.isBlank()) {
                        throw new IllegalStateException("第 " + page + " 页 HTML 内容为空");
                    }
                    
                    List<BocExchangeRate> pageRates = parseHtmlToRates(html);
                    allRates.addAll(pageRates);
                    
                    log.info("✅ [BOC Crawler] 第 {} 页解析完成，获取 {} 条记录", page, pageRates.size());
                    
                    if (page < totalPages) {
                        Thread.sleep(500);
                    }
                }
                
                log.info("✨ [BOC Crawler] 所有页面抓取完成，总计 {} 条汇率记录", allRates.size());
                
                if (!allRates.isEmpty()) {
                    log.info(" [BOC Crawler] ===== 汇率数据详情 =====");
                    for (BocExchangeRate rate : allRates) {
                        log.info("  - 货币: {} ({}) | 现汇买入: {} | 现钞买入: {} | 现汇卖出: {} | 现钞卖出: {} | 折算价: {} | 日期: {} {}",
                                rate.getCurrencyName(),
                                rate.getCurrencyCode(),
                                rate.getSpotBuy(),
                                rate.getCashBuy(),
                                rate.getSpotSell(),
                                rate.getCashSell(),
                                rate.getConversionRate(),
                                rate.getRateDate(),
                                rate.getPublishTime());
                    }
                    log.info("📊 [BOC Crawler] ==========================");
                }
                
                return allRates;
                
            } catch (Exception e) {
                lastException = e;
                log.warn("⚠️ [BOC Crawler] 第 {} 次尝试失败: {}", attempt, e.getMessage());
                
                if (attempt < properties.getMaxAttempts()) {
                    waitForRetry(attempt);
                }
            }
        }
        
        throw new RuntimeException("中国银行外汇牌价抓取失败，已重试 " + properties.getMaxAttempts() + " 次", lastException);
    }

    /**
     * 获取总页数
     */
    private int fetchTotalPages() throws IOException {
        Document doc = Jsoup.connect(properties.getUrl())
                .userAgent(properties.getUserAgent())
                .timeout((int) properties.getReadTimeout().toMillis())
                .get();
        
        // 策略 1：尝试查找包含“共 X 页”的文本
        Elements allElements = doc.select("*");
        for (Element el : allElements) {
            String text = el.text();
            if (text.contains("共") && text.contains("页")) {
                try {
                    String num = text.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        int pages = Integer.parseInt(num);
                        log.info(" [BOC Crawler] 通过文本匹配发现总页数: {}", pages);
                        return pages;
                    }
                } catch (NumberFormatException e) {
                    // 忽略解析错误
                }
            }
        }

        // 策略 2：查找分页组件中的最后一个页码数字
        Elements pageLinks = doc.select("div[class=paging] a");
        if (!pageLinks.isEmpty()) {
            // 通常最后一个链接是“下一页”或末页，倒数第二个可能是最大页码
            // 或者直接遍历所有页码取最大值
            int maxPage = 1;
            for (Element link : pageLinks) {
                try {
                    int pageNum = Integer.parseInt(link.text().trim());
                    if (pageNum > maxPage) {
                        maxPage = pageNum;
                    }
                } catch (NumberFormatException e) {
                    // 不是数字则跳过
                }
            }
            log.info("🔍 [BOC Crawler] 通过分页组件发现最大页码: {}", maxPage);
            return maxPage;
        }
        
        log.warn("⚠️ [BOC Crawler] 未能自动识别总页数，默认按 1 页处理");
        return 1;
    }

    private String fetchHtmlWithAntiBot(int page) throws IOException {
        String url = page == 1 ? properties.getUrl() : properties.getUrl() + "?page=" + page;
        
        Document doc = Jsoup.connect(url)
                .userAgent(properties.getUserAgent())
                .timeout((int) properties.getReadTimeout().toMillis())
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .referrer("https://www.boc.cn/")
                .followRedirects(true)
                .get();
        
        return doc.html();
    }

    /**
     * 解析 HTML 提取汇率数据
     * <p>
     * 中行页面结构示例：
     * <table>
     *   <tr>
     *     <td>货币名称</td>
     *     <td>现汇买入价</td>
     *     <td>现钞买入价</td>
     *     <td>现汇卖出价</td>
     *     <td>现钞卖出价</td>
     *     <td>中行折算价</td>
     *     <td>发布日期</td>
     *     <td>发布时间</td>
     *   </tr>
     * </table>
     */
    private List<BocExchangeRate> parseHtmlToRates(String html) {
        Document doc = Jsoup.parse(html);
        List<BocExchangeRate> rates = new ArrayList<>();
        
        Elements tables = doc.select("table");
        
        if (tables.isEmpty()) {
            log.error("❌ [BOC Crawler] 未找到任何表格元素");
            return rates;
        }
        
        for (Element table : tables) {
            Elements rows = table.select("tr");
            
            for (Element row : rows) {
                Elements cells = row.select("td");
                
                if (cells.size() < 8) {
                    continue;
                }
                
                try {
                    BocExchangeRate rate = parseRowToRate(cells);
                    if (rate != null) {
                        rates.add(rate);
                    }
                } catch (Exception e) {
                    log.warn("⚠️ [BOC Crawler] 解析行数据失败: {}, 行内容: {}", e.getMessage(), row.text());
                }
            }
        }
        
        return rates;
    }

    private BocExchangeRate parseRowToRate(Elements cells) {
        String currencyName = cells.get(0).text().trim();
        
        if (currencyName.isEmpty() || currencyName.equals("货币名称")) {
            return null;
        }
        
        BocExchangeRate rate = new BocExchangeRate();
        rate.setCurrencyName(currencyName);
        rate.setCurrencyCode(extractCurrencyCode(currencyName));
        
        rate.setSpotBuy(parseBigDecimal(cells.get(1).text()));
        rate.setCashBuy(parseBigDecimal(cells.get(2).text()));
        rate.setSpotSell(parseBigDecimal(cells.get(3).text()));
        rate.setCashSell(parseBigDecimal(cells.get(4).text()));
        rate.setConversionRate(parseBigDecimal(cells.get(5).text()));
        
        rate.setRateDate(cells.get(6).text().trim());
        rate.setPublishTime(cells.get(7).text().trim());
        
        String nowStr = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        rate.setCreateTime(nowStr);
        
        return rate;
    }

    /**
     * 从货币名称提取货币代码
     * 例如："美元" -> "USD", "欧元" -> "EUR"
     */
    private String extractCurrencyCode(String currencyName) {
        return switch (currencyName) {
            case "美元" -> "USD";
            case "欧元" -> "EUR";
            case "日元" -> "JPY";
            case "港币" -> "HKD";
            case "英镑" -> "GBP";
            case "澳元" -> "AUD";
            case "加元" -> "CAD";
            case "瑞士法郎" -> "CHF";
            case "新加坡元" -> "SGD";
            case "新西兰元" -> "NZD";
            case "韩元" -> "KRW";
            case "泰国铢" -> "THB";
            case "俄罗斯卢布" -> "RUB";
            case "马来西亚林吉特" -> "MYR";
            case "印尼卢比" -> "IDR";
            case "菲律宾比索" -> "PHP";
            case "巴西雷亚尔" -> "BRL";
            case "阿联酋迪拉姆" -> "AED";
            case "沙特里亚尔" -> "SAR";
            case "南非兰特" -> "ZAR";
            case "土耳其里拉" -> "TRY";
            default -> currencyName; // 如果无法映射，返回原名
        };
    }

    /**
     * 安全解析 BigDecimal
     */
    private BigDecimal parseBigDecimal(String text) {
        if (text == null || text.trim().isEmpty() || text.equals("-")) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replaceAll(",", ""));
        } catch (NumberFormatException e) {
            log.warn("⚠️ [BOC Crawler] 数字解析失败: {}", text);
            return null;
        }
    }

    /**
     * 重试等待（带退避策略）
     */
    private void waitForRetry(int attempt) {
        try {
            long waitTime = properties.getRetryBackoff().toMillis() * attempt;
            log.info("⏳ [BOC Crawler] 等待 {} ms 后重试...", waitTime);
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("重试等待被中断", e);
        }
    }
}
