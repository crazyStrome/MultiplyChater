package crazyStrome;

import java.sql.*;

public class MySQLExcutor {
    public static final int NOT_EXIST = 1;
    public static final int FAILS = 2;
    public static final int SUCCESSED = 3;

    private static final String DRIVER = "com.mysql.jdbc.Driver";
    private static final String URL = "jdbc:mysql://127.0.0.1:3306/ywy?useUnicode=true&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASSWORD = "000000";
    static {
        try {
            Class.forName(DRIVER);
            System.out.println("MySql驱动加载成功");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            System.out.println("MySql驱动加载失败");
        }
    }

    /**
     * 查看当前账户密码是否存在
     * @param userName
     * @param password
     * @return NOT_EXIST : 不存在该用户
     * @return SUCCESSED : 验证成功
     * @return FAILS : 验证失败，密码错误
     */
    public static int CheckAccount(String userName, String password) {
        try (
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement stmt = conn.createStatement();
                ) {
            String sql = "select user_password from multiply_chater where user_name='" + userName + "';";
            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                if (password.equals(resultSet.getString(1))) {
                    return SUCCESSED;
                } else {
                    return FAILS;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return FAILS;
        }
        return NOT_EXIST;
    }

    /**
     * 新建一个账户
     * 如果之前有该用户名
     * 则创建失败
     * @param name
     * @param password
     * @return SUCCESSED : 创建成功
     * @return FAILS : 创建失败，可能已有账户
     */
    public static int CreateAccount(String name, String password) {
        try (
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement stmt = conn.createStatement()
                ) {
            String sql = "insert into multiply_chater values ('" + name + "', '" + password + "');";
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.out.println("插入失败!: " + e);
            return FAILS;
        }
        return SUCCESSED;
    }

    /**
     * 批量删除数据库中的信息
     * 只能root用户使用
     * @param names
     * @return SUCCESSED : 删除成功
     * @return FAILS : 删除失败
     */
    public static int DeleteByName(String... names) {
        try (
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement stmt = conn.createStatement()
                ) {
            for (String s: names) {
                String sql = "delete from multiply_chater where user_name = '" + s + "';";
                stmt.executeUpdate(sql);
            }
            return SUCCESSED;
        } catch (SQLException e) {
            System.out.println("删除失败: " + e);
            return FAILS;
        }
    }

    public static String ShowAllDatas() {
        try (
                Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
                Statement stmt = conn.createStatement()
                ) {
            String sql = "select * from multiply_chater;";
            ResultSet rs = stmt.executeQuery(sql);
            String content = "用户名\t密码\n";
            while (rs.next()) {
                content = content + rs.getString(1) + "\t" + rs.getString(2) + "\n";
            }
            return content;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 调试用的Main方法
     * @param args
     */
    public static void main(String[] args) {
        System.out.println(ShowAllDatas());
        DeleteByName("wyy");
        System.out.println(ShowAllDatas());
    }
}
