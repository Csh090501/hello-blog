package com.byteblogs.common.constant;

/**
 * @Author:byteblogs
 * @Date:2018/09/27 12:52
 */
public class Constants {

    // 系统全局是否标识
    public static final int YES = 1;
    public static final int NO = 0;

    public static final int ZERO = 0;
    public static final int ONE = 1;
    public static final int TWO = 2;
    public static final int THREE = 3;
    public static final int FOUR = 4;
    public static final int FIVE = 5;
    public static final int SIX = 6;
    public static final int SEVEN = 7;
    public static final int EIGHT = 8;
    public static final int NINE = 9;
    public static final int TEN = 10;

    // 默认文件路径分隔符
    public static final String PATH_SEPARATOR = "/";

    // token有效期
    public static final long EXPIRE_TIME = 7 * 24 * 3600 * 1000;

    // 分页默认起始页
    public static final int DEFAULT_PAGE_INDEX = 1;
    // 分页默认每页条数
    public static final int DEFAULT_PAGE_SIZE = 10;
    // 事务有效时间（持续300秒）
    public static final int TX_METHOD_TIMEOUT = 300;

    // 树形路径分隔符
    public static final String TREE_PATH = ".";

    public static final String QINIU_ACCESS_KEY = "qiniu_access_key";

    public static final String QINIU_SECRET_KEY = "qiniu_secret_key";

    public static final String QINIU_BUCKET = "qiniu_bucket";

    public static final String QINIU_IMAGE_DOMAIN = "qiniu_image_domain";

    // header认证字段
    public static final String AUTHENTICATION = "Authorization";

    public static final String BYTE_BLOGS_URL = "https://www.byteblogs.com";

    public static final String BYTE_BLOGS_ARTICLE_LIST = Constants.BYTE_BLOGS_URL + "/api/blog/posts/posts/list?pagingQuery.pageIndex={0}&pagingQuery.pageSize={1}";

    public static final String BYTE_BLOGS_CHAT_LIST = Constants.BYTE_BLOGS_URL + "/api/blog/chat/chat/list?pagingQuery.pageIndex={0}&pagingQuery.pageSize={1}";

    public static final String BYTE_BLOGS_OAUTH_LOGIN = Constants.BYTE_BLOGS_URL + "/api/blog/oauth/github-authorize/v1/get?state={0}";

    public static final String BYTE_BLOGS_ADD_ARTICLE = Constants.BYTE_BLOGS_URL + "/api/blog/posts/hello-blog/v1/add";

}
