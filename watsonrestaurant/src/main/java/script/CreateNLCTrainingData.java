package script;

import java.io.IOException;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import general.Common;
import general.DBClient;
import general.ExecuteQuery;
import general.File;

public class CreateNLCTrainingData {

  public static void main(String[] args) throws Exception {
    // TODO 自動生成されたメソッド・スタブ
    Common.logging("start", "CreateNLCTrainingData");

    final List<String> unique = new ArrayList<String>();
    final List<String> csv = new ArrayList<String>();

    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        final int NLC_VARIABLE_MAXSIZE = 1000;

        PreparedStatement statement = connection.prepareStatement("select distinct res.text, res.class from"
            + " (select l.comment as text, ? as class, l.update_date as date"
            + " from kuchikomi as l inner join"
            + " (select comment, max(update_date) as newest_date from kuchikomi group by comment) as r"
            + " on l.comment = r.comment and l.update_date = r.newest_date and l.total_score >= ?"
            + " union select l.query_text, l.comment_class, l.register_date from logging as l"
            + " inner join (select comment_text, max(register_date) as newest_date from logging"
            + " where logging_type = ? group by comment_text) as r"
            + " on l.comment_text = r.comment_text and l.register_date = r.newest_date"
            + " order by date desc) as res");
        statement.setString(1, Common.prop("nlc_class_good"));
        statement.setInt(2, Integer.parseInt(Common.prop("kuchikomi_score_threshold")));
        statement.setString(3, Common.prop("logging_comment"));

        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next() && unique.size() < NLC_VARIABLE_MAXSIZE) {
          String text_class = resultSet.getString("class");
          for (String text : Common.formatForWatson(resultSet.getString("text")).split("[!♪。\\s]+")) {
            if (!(unique.size() < NLC_VARIABLE_MAXSIZE)) {
              break;
            }
            if (text.length() < 5 || text.length() > 100) {
              continue;
            }
            if (unique.contains(text)) {
              continue;
            }
            unique.add(text);

            List<String> csvRow = new ArrayList<String>();
            csvRow.add(Common.escapeToCSV(text));
            csvRow.add(Common.escapeToCSV(text_class));

            csv.add(String.join(",", csvRow));
          }
        }
        return null;
      }
    });

    try {
      for (String text : File.read(Paths.get(Common.prop("work_path"), "resource", "nlc_yacky.txt").toString())
          .split("\n")) {
        if (unique.contains(text)) {
          continue;
        }
        unique.add(text);
        List<String> csvRow = new ArrayList<String>();
        csvRow.add(Common.escapeToCSV(Common.formatForWatson(text)));
        csvRow.add(Common.escapeToCSV(Common.prop("nlc_class_bad")));

        csv.add(String.join(",", csvRow));
      }
    } catch (IOException e) {
    }

    File.write(Paths.get(Common.prop("work_path"), "watson_data", "nlc_training."+ Common.getDateStr() + ".csv").toString(), csv);
    File.write(Paths.get(Common.prop("work_path"), "watson_data", "nlc_meta.json").toString(),
        "{\"language\":\"ja\", \"name\":\"kuchikomi\"}");

    Common.logging("end", "CreateNLCTrainingData");
  }
}
