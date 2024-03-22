package cn.jnu;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author PengYijun
 * @version 1.0
 * @date 2023.12.09
 * @apiNote Java 一个基于selenium的爬虫，用于爬取Web of Science和EI检索的论文
 */
public class Main {
    public static void main(String[] args) {
        //初始化
        MysqlStart mysql;
        try {
            mysql = new MysqlStart();
            CrawlerSci.setMysql(mysql);
            CrawlerEI.setMysql(mysql);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        int chooseOuter = 1;
        Scanner in = new Scanner(System.in);
        //主程序
        System.out.println("欢迎进入论文爬虫助手1.0！");
        System.out.println("需先进行建库建表操作才可保证功能正常！");
        while (chooseOuter != 173) {
            PrintInfo();
            System.out.print("请输入数字选取功能：");
            chooseOuter = in.nextInt();
            in.nextLine();//清空键盘缓冲区
            switch (chooseOuter) {
                case 2: {
                    try {//此处可改为用户输入数据库名和表名，为方便此处内置
                        mysql.New("jdbc", "paper");
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                break;
                case 3: {
                    System.out.print("请输入Sci关键词：");
                    CrawlerSci.setKeyWordsToSearch(in.nextLine());
                }
                break;
                case 4: {
                    System.out.print("请输入EI关键词：");
                    CrawlerEI.setKeyWordsToSearch(in.nextLine());
                }
                break;
                case 5: {
                    System.out.println("Sci爬虫程序启动，请稍候...");
                    /*
                    通过双线程同时执行两个网站的论文采集工作(失败)，改用逐个采集
                     */
                    CrawlerSci.run();
                }
                break;
                case 6:{
                    System.out.println("EI爬虫程序启动，请稍候...");
                    CrawlerEI.run();
                }
                break;
                case 7: {
                    System.out.println("请输入sql查询语句(select):");
                    mysql.search(in.nextLine());
                }
                break;
                case 8: {
                    int chooseInner = 1;
                    while (chooseInner != 678) {
                        PrintInfoDB();
                        System.out.print("请输入数字选取功能：");
                        chooseInner = in.nextInt();
                        in.nextLine();//清空键盘缓冲区
                        switch (chooseInner) {
                            case 1: {//插入
                                System.out.println("请输入info的所有数据以便完成插入，插入到表中的最新一行。");
                                Info infoTemp = new Info();
                                setInfo(infoTemp, in);
                                mysql.insert(infoTemp);
                            }
                            break;
                            case 2: {//删除
                                System.out.print("请指定id来删除整行数据或不输入来删除整张表(后者不建议)：");
                                String str = in.nextLine();
                                if (str.isEmpty()) mysql.delete();
                                else try {
                                    int a = Integer.parseInt(str);
                                    mysql.delete(a);
                                } catch (NumberFormatException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            break;
                            case 3: {//修改
                                System.out.println("请输入指定id和所有内容来修改id行数据。");
                                System.out.print("id: ");
                                int index = in.nextInt();
                                in.nextLine();//清空键盘缓冲区
                                Info infoTemp = new Info();
                                setInfo(infoTemp, in);
                                mysql.modify(index, infoTemp);
                            }
                            break;
                            case 4: {//查找
                                System.out.println("请输入sql语句进行查找。");
                                mysql.search(in.nextLine());
                            }
                            break;
                            case 5: {//导入
                                System.out.print("输入1确认当前目录下的source/src.csv文件存在：");
                                if (in.nextInt() == 1) {
                                    mysql.importData();
                                    in.nextLine();
                                }
                            }
                            break;
                            case 6: {//导出
                                System.out.print("输入1确认当前目录下的target/tag.csv文件不存在/可以删除：");
                                if (in.nextInt() == 1) {
                                    mysql.exportData();
                                    in.nextLine();
                                }
                            }
                            break;
                            default:
                                break;
                        }
                    }
                }
                break;
                default:
                    break;
            }
        }
        System.out.println("欢迎下次使用！");
    }

    //设置info变量
    private static void setInfo(Info info, Scanner in) {
        System.out.print("title: ");
        info.setTitle(in.nextLine());
        System.out.print("sourceOfWeb: ");
        info.setSourceOfWeb(in.nextLine());
        System.out.print("author: ");
        info.setAuthor(in.nextLine());
        System.out.print("email: ");
        info.setEmail(in.nextLine());
    }

    /**
     * @apiNote 用于输出提示信息
     */
    public static void PrintInfo() {
        System.out.println("------------功能--------------");
        System.out.println("1.显示功能面板------------------");
        System.out.println("2.新建数据库jdbc和数据表paper----");
        System.out.println("3.修改Sci爬虫关键字-------------");
        System.out.println("4.修改EI爬虫关键字--------------");
        System.out.println("5.启动Sci爬虫并写入信息到数据库----");
        System.out.println("6.启动EI 爬虫并写入信息到数据库----");
        System.out.println("7.显示数据库信息-----------------");
        System.out.println("8.对数据库信息进行操作------------");
        System.out.println("173.退出本助手------------------");
    }

    public static void PrintInfoDB() {
        System.out.println("------数据库操作------");
        System.out.println("1.插入---------------");
        System.out.println("2.删除---------------");
        System.out.println("3.修改---------------");
        System.out.println("4.查找---------------");
        System.out.println("5.导入---------------");
        System.out.println("6.导出---------------");
        System.out.println("678.退出-------------");
    }
}
