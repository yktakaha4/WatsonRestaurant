package general;

import java.sql.Connection;

@FunctionalInterface
public interface ExecuteQuery {
  Object call(Connection connection) throws Exception;
}
