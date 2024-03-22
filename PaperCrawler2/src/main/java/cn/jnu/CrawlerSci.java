package cn.jnu;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.*;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author PengYijun
 * @apiNote 本爬虫基于selenium引擎和java.sql包，根据关键词搜索，可将WebofScience网站的论文信息爬取并存入jdbc数据库，每次爬取1000条信息
 */
public class CrawlerSci {
    private static MysqlStart mysql;
    //定义要搜索的关键词
    private static String keyWordsToSearch;

    //保存当前页面序号
    private static String pageNow;

    public static void setMysql(MysqlStart mysql) {
        CrawlerSci.mysql = mysql;
    }

    public static void setKeyWordsToSearch(String k) {
        CrawlerSci.keyWordsToSearch = k;
    }

    public static void run() {
        //加载 chromedriver 驱动
        System.setProperty("webdriver.chrome.driver", "C:\\Program Files\\Google\\Chrome\\Application\\chromedriver.exe");
        //打开一个浏览器窗口
//        ChromeOptions chromeOptions = new ChromeOptions();
//        chromeOptions.addArguments("--start-maximized");
//        chromeOptions.addArguments("--headless"); //无头浏览器
//        chromeOptions.addArguments("--disable-gpu"); // 谷歌文档提到需要加上这个属性来规避bug
//        WebDriver chromeDriver = new ChromeDriver(chromeOptions);

        WebDriver chromeDriver = new ChromeDriver();
        chromeDriver.manage().window().maximize();
        // 创建一个map集合，传入要爬取的页面的url，并定义一个Bool型值，用于判断当前传入的url是否被遍历过；
        Map<String, Boolean> oldMap = new LinkedHashMap<String, Boolean>();

        //设置读取上一次爬虫页面位置
        File logFile = new File("src\\main\\java\\logSci.log");
        //保存上次搜索进行到的页面序号
        String pageNum;
        try {
            BufferedReader f = new BufferedReader(new FileReader(logFile));
            String str = f.readLine();
            if (str == null) pageNum = "1";
            else {
                pageNum = str;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //打开Web of Science
        chromeDriver.navigate().to("https://www.webofscience.com/wos/alldb/basic-search");
        //最多等待40秒，超过则报错
        chromeDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(40));
        //在搜索文本框输入keyWordsToSearch搜索
        chromeDriver.findElement(By.id("mat-input-0")).sendKeys(keyWordsToSearch, Keys.ENTER);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //继续从上次位置开始爬虫
        WebElement pageNumInput = chromeDriver.findElement(By.id("snNextPageTop"));
        pageNow = pageNumInput.getAttribute("aria-label").substring(16);
        if (pageNum.compareTo(pageNow) > 0) {
            pageNumInput.clear();
            pageNumInput.sendKeys(pageNum, Keys.ENTER);
            pageNow = pageNum;
        }
        //开始爬虫(默认查找20页*50条数据 = 1000)
        for (int i = 0; i < 20; i++)
            CrawlerHelper(oldMap, chromeDriver);
        System.out.println("Sci爬虫完成！");
    }

    /**
     * @param oldMap LinkedHashMap类型，用于存放所有url
     * @param driver WebDriver类型，selenium
     * @apiNote 进入搜索页面并提取50条论文搜索记录，提取信息(getSciInfo)，写入数据库，最后跳转下一页
     */
    private static void CrawlerHelper(Map<String, Boolean> oldMap, WebDriver driver) {
        try {
            try {//线程睡眠5秒以增强系统稳定性
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //等到WebOfScience搜索结果页面元素加载完（这里最多等30秒）超时则异常
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(30000));
            //滚动加载链接
            while (!driver.findElements(By.cssSelector("app-records-list>app-record.loading-cover")).isEmpty()) {
                String script = "return arguments[0].scrollIntoView();";
                WebElement element = driver.findElement(By.cssSelector("app-records-list>app-record.loading-cover"));
                ((JavascriptExecutor) driver).executeScript(script, element);
                try{
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
            //提取链接
            List<WebElement> elementsLink = driver
                    .findElement(By.tagName("app-records-list"))
                    .findElements(By.cssSelector("a[data-ta='summary-record-title-link']"));
            for (WebElement Link : elementsLink
            ) {
                String newLink = Link.getAttribute("href").trim();
                //判断newLink是否为合法的,如果newLink不以https开头，此时的newLink就是一个相对路径
                if (!newLink.startsWith("https")) {
                    if (newLink.startsWith("/")) {
                        newLink = "https://www.webofscience.com" + newLink;
                    } else {
                        newLink = "https://www.webofscience.com" + "/" + newLink;
                    }
                }
                //去掉url结尾的'/'，规范化方便判断重复
                if (newLink.endsWith("/")) {
                    newLink = newLink.substring(0, newLink.length() - 1);
                }
                //判断url有没有重复
                if (!oldMap.containsKey(newLink)) {
                    oldMap.put(newLink, false);
                }
            }

            //每次依次打开10条链接，设置时间间隔，防止封IP
            //初始化数据
            String oldLink = "";
            int i = 1;
            String HandleCur = driver.getWindowHandle();//保存原始页面句柄
            for (Map.Entry<String, Boolean> mapping : oldMap.entrySet()) {
                //打开10条链接
                if (!mapping.getValue()) {
                    oldLink = mapping.getKey();
                    // 在新的标签页打开链接
                    JavascriptExecutor js = (JavascriptExecutor) driver;
                    js.executeScript("var link = arguments[0];" +
                            "window.open(link,'_blank')", oldLink);
                    //将访问过的链接标记为true
                    oldMap.replace(oldLink, false, true);

                    // 切换到新的标签页
                    if (i % 10 == 0) {
                        Set<String> HandlesTemp = driver.getWindowHandles();
                        HandlesTemp.remove(HandleCur);
                        for (String str : HandlesTemp
                        ) {//若不是我们想要的论文信息页，则跳过
                            driver.switchTo().window(str);
                            String ccUrl = driver.getCurrentUrl();
                            String pattern = "https://webofscience\\.clarivate\\.cn/wos/alldb/full-record/.*";
                            boolean isMatch = Pattern.matches(pattern, ccUrl);
                            pattern = "https://www\\.webofscience\\.com/wos/alldb/full-record/.*";
                            isMatch = isMatch ? isMatch : Pattern.matches(pattern, ccUrl);
                            if (isMatch) {
                                //等待新窗口或新页签的内容加载
                                wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("snMainArticle")));
                                getSciInfo(driver);//获取信息
                            }
                            driver.close();
                        }
                        driver.switchTo().window(HandleCur);
                    }
                    //睡眠10秒以防止封IP
                    try {
                        if (i % 10 == 0) Thread.sleep(10000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                i++;
            }
            //进入下一页
            driver.findElement(By.cssSelector("button[aria-label='Bottom Next Page']")).click();
            int temp = Integer.parseInt(pageNow);
            temp++;
            pageNow = String.valueOf(temp);
        } catch (Exception e) {//若爬虫发生异常则保存此次爬取所进行到的页面，方便下次从此处开始
            System.out.println("Sci发生异常！正在保存爬虫进度。");
            try {
                FileWriter fileWriter = new FileWriter("src\\main\\java\\logSci.log", false);
                fileWriter.write(pageNow);
                fileWriter.close();
            } catch (IOException ioException) {
                System.out.println("写入日志文件失败！");
                throw new RuntimeException(e);
            }
            java.awt.Toolkit.getDefaultToolkit().beep();
            System.exit(1);
        }
    }

    /**
     * @param driver:当前页面句柄
     * @apiNote 从Web of Science获取所需信息并存储到数据库
     */
    private static void getSciInfo(WebDriver driver) {
        //初始化info
        Info info = new Info();
        //获取论文标题title
        info.setTitle(driver.findElement(By.id("FullRTa-fullRecordtitle-0")).getText());
        //获取论文网站来源sourceOfWeb
        info.setSourceOfWeb("Web of Science");
        //获取论文作者author
        List<WebElement> elementsAuthor = driver.findElements(By.cssSelector("span[id ^= 'author']"));
        StringBuffer str1 = new StringBuffer();
        for (WebElement e : elementsAuthor
        ) {
            str1.append(e.getText());
        }
        info.setAuthor(str1.toString().trim());
        //获取邮箱email(可能为空，所以要先判断)
        if (!(driver.findElements(By.id("FRAiinTa-AuthRepEmailAddr-0"))).isEmpty())
            info.setEmail(driver.findElement(By.id("FRAiinTa-AuthRepEmailAddr-0")).getText());
        //获取关键字keyword(暂未实现)
        //将其写入到jdbc数据库
        mysql.insert(info);
        //待续...
    }
}
