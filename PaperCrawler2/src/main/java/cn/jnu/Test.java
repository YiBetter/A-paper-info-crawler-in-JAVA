package cn.jnu;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.openqa.selenium.support.ui.ExpectedConditions.titleIs;

/**
 * @apiNote Test测试类，可以删除
 */
public class Test {
    private String s1;

    public void test(String str){
        this.s1=str;
    }
    public void pp(){
        System.out.println(this.s1);
        System.out.println(s1);
    }
    public static void main(String[] args) throws IOException {
//        Info info = new Info();
//        info.setTitle("test1");
//        info.setAuthor("tauthor1");
//        info.setSourceOfWeb("ttttt");
//        info.setEmail("temail");
//        MysqlStart mysql = new MysqlStart();
//        mysql.New("TTT","tes");
//        mysql.insert(info);
//        mysql.modify(1,info);
//        mysql.delete();
//        mysql.insert(info);
//        mysql.delete(1);
//        mysql.insert(info);
//        mysql.insert(info);
//        mysql.exportData();
//        mysql.importData();
        //初始化
        MysqlStart mysql;
        try {
            mysql = new MysqlStart();
            CrawlerSci.setMysql(mysql);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        mysql.New("TTT","tes");
//        CrawlerSci.setMysql(mysql);
//        CrawlerSci.setKeyWordsToSearch("big data");
//        CrawlerSci.run();
        CrawlerEI.setMysql(mysql);
        CrawlerEI.setKeyWordsToSearch("social media");
        CrawlerEI.run();
    }
}
