package general;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBClient {
  public static Object execute(ExecuteQuery executeQuery) throws Exception {
    Connection connection = null;
    Object returnValue = null;
    try {
      connection = DriverManager.getConnection(Common.prop("db_wn_url"), Common.prop("db_wn_user"),
          Common.prop("db_wn_password"));
      connection.setAutoCommit(false);
      connection.setSchema(Common.prop("db_wn_schema"));

      returnValue = executeQuery.call(connection);

      connection.commit();
    } catch (Exception exception) {
      if (connection != null) {
        try {
          connection.rollback();
        } catch (Exception rollbackException) {
        }
      }
      throw exception;
    } finally {
      if (connection != null) {
        try {
          connection.close();
        } catch (SQLException exception) {
        }
      }
    }
    return returnValue;
  }
}
