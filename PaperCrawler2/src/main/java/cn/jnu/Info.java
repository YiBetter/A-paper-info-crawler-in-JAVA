package cn.jnu;

/**
 * @apiNote Info用于存取与mysql指定表对应数据。除sourceOfWeb预先设置无需判断外，其余字符串若大于字段容量则截取
 */
public class Info {
    /**
     * 论文标题
     */
    private String title;
    /**
     * 来源
     */
    private String sourceOfWeb;
    /**
     * 作者
     */
    private String author;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 关键字
     */
//    private String keyword;
    public String getTitle() {
        return title;
    }

    //若字符串长度大于字段容量则截取
    public void setTitle(String title) {
        if (title.length() > 100)
            this.title = title.substring(0, 100);
        else
            this.title = title;
    }

    public String getSourceOfWeb() {
        return sourceOfWeb;
    }

    public void setSourceOfWeb(String sourceOfWeb) {
        this.sourceOfWeb = sourceOfWeb;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        if (author.length() > 100)
            this.author = author.substring(0, 100);
        else
            this.author = author;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        if (email.length() > 100)
            this.email = email.substring(0, 100);
        else
            this.email = email;
    }

//    public String getKeyword() {
//        return keyword;
//    }
//
//    public void setKeyword(String keyword) {
//        this.keyword = keyword;
//    }
}
