package cn.jnu;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.Properties;
import java.util.Scanner;

/**
 * @author PengYijun
 * @version 1.0
 * @apiNote 有关MySQL的一系列操作
 */

public class MysqlStart {
    private String DBName;
    private String TBName;
    private Connection connection;

    /**
     * @throws IOException 文件读取出错
     * @apiNote 启动mysql和jdbc服务
     */
    public MysqlStart() throws IOException {
        // 通过 Properties 对象获取配置文件的信息
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("src\\main\\resources\\db.properties")));
        // 获取相关的值
        String user = properties.getProperty("user");
        String password = properties.getProperty("password");
        String url = properties.getProperty("url");
        String driver = properties.getProperty("driver");
        try {
            Class.forName(driver);     //加载MYSQL JDBC驱动程序
            System.out.println("成功加载 Mysql Driver!");
        } catch (Exception e) {
            System.out.print("加载失败 Mysql Driver!");
            e.printStackTrace();
        }
        try {
            this.connection = DriverManager.getConnection(url, user, password);
            System.out.println("连接MySQL根目录成功！");
            //建立连接，URL为jdbc:mysql//服务器地址/数据库 ，后面的2个参数分别是登陆用户名和密码
        } catch (Exception e) {
            System.out.println("连接MySQL失败！");
            e.printStackTrace();
        }
    }

    /**
     * @param dbname 新建数据库的名称
     * @param tbname 新建数据表的名称
     * @throws IOException 访问数据库及其内容失败
     * @apiNote 新建数据库和表，连接到新数据库
     */
    public void New(String dbname, String tbname) throws IOException {
        this.DBName = dbname;
        this.TBName = tbname;
        // 通过 Properties 对象获取配置文件的信息
        Properties properties = new Properties();
        properties.load(Files.newInputStream(Paths.get("src\\main\\resources\\db.properties")));
        // 获取相关的值
        String user = properties.getProperty("user");
        String password = properties.getProperty("password");
        String url = properties.getProperty("url");
        try {
            //创建运行对象
            /*
             id为11空格的int值(最多显示11位10进制数)，title/author/email都是100空格可变字符，sourceOfWeb是20空格可变字符
             邮箱email字段允许为空，其余非空
             主键id，自增量为一
             */
            String DBSql = "CREATE DATABASE IF NOT EXISTS " + this.DBName + ";";
            String TBSql = "CREATE TABLE IF NOT EXISTS " + this.TBName
                    + " (id int(11) NOT NULL AUTO_INCREMENT," +
                    "title varchar(100) COLLATE utf8_bin NOT NULL," +
                    "sourceOfWeb varchar(20) COLLATE utf8_bin NOT NULL," +
                    "author varchar(100) COLLATE utf8_bin NOT NULL," +
                    "email varchar(100) COLLATE utf8_bin," +
                    "PRIMARY KEY (id)" +
                    ") ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin AUTO_INCREMENT=1;";

            Statement stmt = this.connection.createStatement();
            if (this.connection != null) {
                System.out.println("成功连接 Mysql 根目录!");
                // 执行建库语句
                stmt.executeUpdate(DBSql);
                stmt.close();
                // 链接新建的数据库
                Connection newConn = DriverManager
                        .getConnection(url.substring(0, 28) + dbname + url.substring(28), user, password);
                if (newConn != null) {
                    this.connection.close();
                    this.connection = newConn;
                    System.out.println("已经连接到新创建的数据库：" + dbname);
                    Statement newSmt = newConn.createStatement();
                    // 执行建表语句
                    int i = newSmt.executeUpdate(TBSql);
                    // DDL语句返回值为0
                    if (i == 0) {
                        System.out.println(tbname + "表已经创建成功！");
                    }
                }
            }
        } catch (Exception e) {
            System.out.print("get data error!");
            e.printStackTrace();
        }
    }

    /**
     * @param info Info类的数据作为输入
     * @apiNote 增加新字段
     */
    public void insert(Info info) {
        try {
            //插入Info实例中的值到paper表中
            String sql = "INSERT INTO " + this.TBName + "(title, sourceOfWeb, author, email) VALUES (?, ?, ?, ?)";
            PreparedStatement preStat = this.connection.prepareStatement(sql);
            preStat.setString(1, info.getTitle());
            preStat.setString(2, info.getSourceOfWeb());
            preStat.setString(3, info.getAuthor());
            preStat.setString(4, info.getEmail());
            int rs = preStat.executeUpdate();
            if (rs != 0) {
                System.out.println("插入成功！");
            } else {
                System.out.println("插入失败！");
            }
            preStat.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @apiNote 删除数据表
     */
    public void delete() {
        //清空数据表
        System.out.println("你真的要清除整个数据表吗？输入98765确认。");
        Scanner scanner = new Scanner(System.in);
        int choose = scanner.nextInt();
        scanner.nextLine();
        if (choose == 98765) {
            try {
                Statement stat = this.connection.createStatement();
                stat.executeUpdate("TRUNCATE TABLE " + this.TBName + ";");
                System.out.println("清除成功！");
            } catch (Exception e) {
                System.out.println("清除失败！");
                throw new RuntimeException();
            } finally {
                scanner.close();//回收资源
            }
        }
    }

    /**
     * @param index 指定删除id
     * @apiNote 删除指定id内容
     */
    public void delete(int index) {
        //删除指定id的所有内容
        try {
            //插入Info实例中的值到paper表中
            String sql = "DELETE FROM " + this.TBName + " WHERE id=?";
            PreparedStatement preStat = this.connection.prepareStatement(sql);
            preStat.setInt(1, index);
            int rs = preStat.executeUpdate();
            if (rs != 0) {
                System.out.println("删除成功！");
            } else {
                System.out.println("删除失败！");
            }
            preStat.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param index 指定修改字段id
     * @apiNote 修改字段
     */
    public void modify(int index, Info info) {
        //修改指定id的所有内容
        try {
            //插入Info实例中的值到paper表中
            String sql = "UPDATE " + this.TBName + " SET title=?,sourceOfWeb=?,author=?,email=? WHERE id=?";
            PreparedStatement preStat = this.connection.prepareStatement(sql);
            preStat.setString(1, info.getTitle());
            preStat.setString(2, info.getSourceOfWeb());
            preStat.setString(3, info.getAuthor());
            preStat.setString(4, info.getEmail());
            preStat.setInt(5, index);
            int rs = preStat.executeUpdate();
            if (rs != 0) {
                System.out.println("修改成功！");
            } else {
                System.out.println("修改失败！");
            }
            preStat.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @apiNote 查找字段
     */
    public void search(String sql) {
        //输入sql语句进行查找
        try {
            //查询paper表中内容
            Statement Stat = this.connection.createStatement();
            ResultSet rs = Stat.executeQuery(sql);
            if (rs==null) {//若查询结果为空
                System.out.println("查询失败！");
            } else {//否则输出查询结果
                System.out.println("      id       title sourceOfWeb      author       email");
                while (rs.next()) {
                    System.out.println("\t" + rs.getString("id") +
                            "\t" + rs.getString("title") +
                            "\t" + rs.getString("sourceOfWeb") +
                            "\t" + rs.getString("author") +
                            "\t" + rs.getString("email"));
                }
            }
            Stat.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @apiNote 导入数据为csv格式，默认路径为从本项目文件下的source\src.csv
     */
    public void importData() {
        try {
            Reader in = new FileReader("source\\src.csv");
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(in);
            try {
                String sql = "INSERT INTO " + this.TBName + " VALUES(?, ?, ?, ?, ?)";
                PreparedStatement prestat = connection.prepareStatement(sql);
                for (CSVRecord record : records) {
                    prestat.setInt(1, Integer.parseInt(record.get("id")));
                    prestat.setString(2, record.get("title"));
                    prestat.setString(3, record.get("sourceOfWeb"));
                    prestat.setString(4, record.get("author"));
                    prestat.setString(5, record.get("email"));
                    prestat.executeUpdate();
                }
                System.out.println("数据成功导入数据库！");
            } catch (SQLException | NumberFormatException e) {
                throw new RuntimeException(e);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @apiNote 导出数据为csv格式，默认路径为到本项目文件下的target\tag.csv
     */
    public void exportData() {
        try {
            FileWriter file = new FileWriter("target\\tag.csv",
                    StandardCharsets.UTF_8, false);
            try (Statement st = this.connection.createStatement()) {
                ResultSet resultSet = st.executeQuery("select * from " + this.TBName + ";");
                CSVPrinter csvPrinter = new CSVPrinter(file, CSVFormat.DEFAULT.withHeader(resultSet));
                csvPrinter.printRecords(resultSet);
                csvPrinter.close();
                System.out.println("数据导出成功！路径为target\\tag.csv");
            } catch (SQLException e) {
                e.printStackTrace();
                System.exit(1);
            } catch (IOException ie) {
                System.err.println(ie.getMessage());
                System.exit(1);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}