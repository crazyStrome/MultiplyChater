package crazyStrome;

public interface CrazyStromeProtocol {
    int PROTOCOL_LEN = 2;
    /**
     * 发消息使用的信息头
     */
    String MAG_ROUND = "∏∞";
    /**
     * 注册使用的消息头
     */
    String REGISTER_ROUND = "≮≯";
    String REGISTER_SUCCESS = "2";
    String REGISTER_FAILED = "-4";
    /**
     * 登陆使用的消息头
     */
    String LOGIN_ROUND = "≮∝";
    String LOGIN_SUCCESS = "1";
    String LOGIN_FAILED = "-2";
    /**
     * 登录用户重复
     */
    String NAME_REP = "-1";
    /**
     * 私人信息头
     */
    String PRIVATE_ROUND = "∷∝";
    /**
     * 分隔符
     */
    String SPLIT_SIGN = "┣";
    /**
     * 用户不存在
     */
    String NAME_NOT_EXIST = "-3";
    /**
     * root用户使用的信息头
     */
    String ROOT_ROUND = "≯≮";
    /**
     * 数据库数据删除成功
     */
    String DEL_SUCCESS = "3";
    /**
     * 数据库删除失败
     */
    String DEL_FAIL = "-5";
    /**
     * 数据库查询失败
     */
    String QUERY_FAIL = "-6";
}
