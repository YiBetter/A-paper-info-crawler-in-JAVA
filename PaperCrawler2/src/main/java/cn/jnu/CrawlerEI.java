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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author PengYijun
 * @apiNote 本爬虫基于selenium引擎和java.sql包，根据关键词搜索，可将EI网站的论文信息爬取并存入jdbc数据库，每次爬取1000条信息
 */
public class CrawlerEI {
    private static MysqlStart mysql;
    //定义要搜索的关键词
    private static String keyWordsToSearch;

    private static String pageNow;

    public static void setMysql(MysqlStart mysql) {
        CrawlerEI.mysql = mysql;
    }

    public static void setKeyWordsToSearch(String k) {
        CrawlerEI.keyWordsToSearch = k;
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
        //打开engineering village主页
        chromeDriver.navigate().to("https://www.engineeringvillage.com/");
        //全局元素查找超过40秒则报错
        chromeDriver.manage().timeouts().implicitlyWait(Duration.ofSeconds(40));
        //js加载查找超过50秒则报错
        chromeDriver.manage().timeouts().scriptTimeout(Duration.ofSeconds(50));
        //页面加载查找超过60秒则报错
        chromeDriver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(60));
        //在搜索文本框输入keyWordsToSearch搜索
        chromeDriver.findElement(By.id("search-word-1")).sendKeys(keyWordsToSearch, Keys.ENTER);
        //设置读取上一次爬虫页面位置
        File logFile = new File("src\\main\\java\\logEI.log");
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
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        //继续从上次位置开始爬虫
        String url = chromeDriver.getCurrentUrl();
        String regexCount = "COUNT=\\d+";
        Matcher matcher = Pattern.compile(regexCount).matcher(url);
        pageNow = "";//现在打开的网页序号
        if (matcher.find())
            pageNow = matcher.group().substring(6);
        if (pageNum.compareTo(pageNow) > 0) {//若保存的网页序号大于当前打开的则跳转
            String urlNew = chromeDriver.getCurrentUrl().replaceFirst("COUNT=\\d+", "COUNT=" + pageNum);
            chromeDriver.get(urlNew);
            pageNow = pageNum;
        }
        //开始爬虫(默认查找40页*25条数据 = 1000)
        for (int i = 0; i < 1; i++)
            CrawlerHelper(oldMap, chromeDriver);
        System.out.println("EI爬虫完成！");
    }

    /**
     * @param oldMap LinkedHashMap类型，用于存放所有url
     * @param driver WebDriver类型，selenium
     * @apiNote 进入搜索页面并提取25条论文搜索记录，提取信息(getSciInfo)，写入数据库，最后跳转下一页
     */
    private static void CrawlerHelper(Map<String, Boolean> oldMap, WebDriver driver) {
        try {
            try {//线程睡眠10秒以增强系统稳定性
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            //等到EI搜索结果页面元素加载完（这里最多等39秒）超时则异常
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofMillis(39000));
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("results-region")));
            //提取链接
            List<WebElement> elementsLink = driver
                    .findElement(By.id("results-region"))
                    .findElements(By.cssSelector(".combinedlink"));
            for (WebElement Link : elementsLink
            ) {
                String newLink = Link.getAttribute("href").trim();
                //判断newLink是否为合法的,如果newLink不以https开头，此时的newLink就是一个相对路径
                if (!newLink.startsWith("https")) {
                    if (newLink.startsWith("/")) {
                        newLink = "https://www.engineeringvillage.com" + newLink;
                    } else {
                        newLink = "https://www.engineeringvillage.com" + "/" + newLink;
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

            //每次依次打开10/10/5条链接，设置时间间隔，防止封IP
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
                    if (i % 10 == 0 || i == 25) {
                        Set<String> HandlesTemp = driver.getWindowHandles();
                        HandlesTemp.remove(HandleCur);
                        try {//等待3秒防止新页面未加载完成
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        for (String str : HandlesTemp
                        ) {//若不是我们想要的论文信息页，则跳过
                            driver.switchTo().window(str);
                            String ccUrl = driver.getCurrentUrl();
                            String pattern = "https://www\\.engineeringvillage\\.com/app/doc/\\?docid=.*";
                            boolean isMatch = Pattern.matches(pattern, ccUrl);
                            if (isMatch) {
                                //等待新窗口或新页签的内容加载
                                wait.until(ExpectedConditions
                                        .visibilityOfAllElementsLocatedBy(By
                                                .cssSelector(".MainArticle_main-article__Q84sc")));
                                getEIInfo(driver);//获取信息
                            }
                            driver.close();
                        }
                        driver.switchTo().window(HandleCur);
                    }
                    //睡眠10秒以防止封IP
                    try {
                        if (i % 10 == 0) Thread.sleep(8000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                i++;
            }
            //进入下一页
            driver.findElement(By.id("next-page-top")).click();
            int temp = Integer.parseInt(pageNow);
            temp += 25;
            pageNow = String.valueOf(temp);
        } catch (Exception e) {//若爬虫发生异常则保存此次爬取所进行到的页面，方便下次从此处开始
            System.out.println("EI发生异常！正在保存爬虫进度。");
            try {
                FileWriter fileWriter = new FileWriter("src\\main\\java\\logEI.log", false);
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
    private static void getEIInfo(WebDriver driver) {
        //初始化info
        Info info = new Info();
        //获取论文标题title
        info.setTitle(driver.findElement(By.tagName("h2")).getText());
        //获取论文网站来源EI
        info.setSourceOfWeb("EI");
        //获取论文作者author
        List<WebElement> elementsAuthor = driver.findElements(By.cssSelector(".ev__author-name"));
        StringBuffer str1 = new StringBuffer();
        for (WebElement e : elementsAuthor
        ) {
            str1.append(e.getText());
            str1.append("; ");
        }
        info.setAuthor(str1.toString().trim());
        //获取邮箱email(可能为空，所以要先判断)
        if (!(driver.findElements(By.cssSelector(".ev__author-email"))).isEmpty()) {
            List<WebElement> elementsEmail = driver.findElements(By.cssSelector(".ev__author-email"));
            StringBuffer str2 = new StringBuffer();
            for (WebElement e : elementsEmail
            ) {
                str2.append(e.getAttribute("href").substring(7));
                str2.append("; ");
            }
            info.setEmail(str2.toString().trim());
        }
        //获取关键字keyword(暂未实现)
        //将其写入到jdbc数据库
        mysql.insert(info);
        //待续...
    }
}
