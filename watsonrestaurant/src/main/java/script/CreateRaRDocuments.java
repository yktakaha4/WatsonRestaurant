package script;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import general.Common;
import general.DBClient;
import general.ExecuteQuery;
import general.File;
import general.RestaurantDocument;

public class CreateRaRDocuments {

  public static void main(String[] args) throws Exception {
    // TODO 自動生成されたメソッド・スタブ
    Common.logging("start", "CreateRaRDocuments");

    final int PRTEXT_MAXLENGTH = 50;
    final List<RestaurantDocument> documents = new ArrayList<RestaurantDocument>();

    // ぐるなびのPR文からドキュメント生成
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection.prepareStatement(
            "select res.*, coalesce(kuc.image_url, '') as menu_image_url from restaurant as res"
                + " left join (select l.shop_id, l.image_url from kuchikomi as l"
                + " inner join (select shop_id, max(update_date) as newest_date"
                + " from kuchikomi group by shop_id) as r"
                + " on l.shop_id = r.shop_id and l.update_date = r.newest_date) as kuc"
                + " on res.shop_id = kuc.shop_id");
        ResultSet resultSet = statement.executeQuery();

        int count = 0;
        while (resultSet.next()) {
          count++;

          RestaurantDocument document = new RestaurantDocument();

          document.shopId = resultSet.getString("shop_id");
          document.voteId = "";
          document.shopName = resultSet.getString("name");
          document.shopNameKana = resultSet.getString("name_kana");
          document.menuName = "";
          document.menuNameKana = "";
          document.latitude = resultSet.getString("latitude");
          document.longitude = resultSet.getString("longitude");
          document.shopUrl = resultSet.getString("shop_url");

          document.imageUrl = "";
          String[] imageUrls = { resultSet.getString("image_url"), resultSet.getString("menu_image_url") };
          for (String imageUrl : imageUrls) {
            if (imageUrl != "") {
              document.imageUrl = imageUrl;
              break;
            }
          }

          document.prText = Common.formatForHTMLText(resultSet.getString("pr_short"));

          String[] texts = { document.shopName.replaceAll("\\s", ""),
              document.shopNameKana.replaceAll("\\s", ""), resultSet.getString("category"),
              resultSet.getString("pr_long") };
          document.text = Common.formatForWatson(String.join("。", texts));

          document.budget = -1;
          int[] budgets = { resultSet.getInt("budget_party"), resultSet.getInt("budget"),
              resultSet.getInt("budget_lunch") };
          for (int budget : budgets) {
            if (budget != -1) {
              document.budget = budget;
              break;
            }
          }

          document.documentId = Common.makeIdentifier(document.shopId);
          documents.add(document);
        }
        Common.logging("ぐるなびPR文から", count);
        return null;
      }
    });

    // ぐるなびの応援コメントからドキュメント生成
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection
            .prepareStatement("select l.*, r.vote_id, r.menu_name, r.comment, r.image_url as menu_image_url"
                + " from restaurant as l"
                + " inner join kuchikomi as r on l.shop_id = r.shop_id and r.total_score >= ?");
        statement.setInt(1, Integer.parseInt(Common.prop("kuchikomi_score_threshold")));
        ResultSet resultSet = statement.executeQuery();

        int count = 0;
        while (resultSet.next()) {
          count++;
          RestaurantDocument document = new RestaurantDocument();

          document.shopId = resultSet.getString("shop_id");
          document.voteId = resultSet.getString("vote_id");
          document.shopName = resultSet.getString("name");
          document.shopNameKana = resultSet.getString("name_kana");
          document.menuName = resultSet.getString("menu_name");
          document.menuNameKana = Common.getKatakana(document.menuName);
          document.latitude = resultSet.getString("latitude");
          document.longitude = resultSet.getString("longitude");
          document.shopUrl = resultSet.getString("shop_url");
          document.imageUrl = resultSet.getString("menu_image_url");

          document.prText = Common
              .formatForHTMLText("『" + document.menuName + "』" + resultSet.getString("comment"));
          if (document.prText.length() > PRTEXT_MAXLENGTH) {
            document.prText = document.prText.substring(0, PRTEXT_MAXLENGTH);
            document.prText = Common
                .formatForHTMLText(Common.getFirstMatch("^.*[』!♪。\\s]", document.prText));
          }

          String[] texts = { document.menuName, resultSet.getString("comment") };
          document.text = Common.formatForWatson(String.join("。", texts));

          document.budget = -1;

          document.documentId = Common.makeIdentifier(document.shopId, document.voteId);
          documents.add(document);
        }
        Common.logging("システム利用者のコメントから", count);
        return null;
      }
    });

    // システム利用者のコメントからドキュメント生成
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection
            .prepareStatement("select l.*, r.comment_text, r.logging_id from restaurant as l"
                + " inner join logging as r"
                + " on l.shop_id = r.shop_id and r.logging_type = ? and r.comment_class = ?");
        statement.setString(1, Common.prop("logging_comment"));
        statement.setString(2, Common.prop("nlc_class_good"));

        ResultSet resultSet = statement.executeQuery();

        int count = 0;
        while (resultSet.next()) {
          count++;
          RestaurantDocument document = new RestaurantDocument();

          document.shopId = resultSet.getString("shop_id");
          document.voteId = "";
          document.shopName = resultSet.getString("name");
          document.shopNameKana = resultSet.getString("name_kana");
          document.menuName = "";
          document.menuNameKana = "";
          document.latitude = resultSet.getString("latitude");
          document.longitude = resultSet.getString("longitude");
          document.shopUrl = resultSet.getString("shop_url");
          document.imageUrl = resultSet.getString("image_url");

          document.prText = Common.formatForHTMLText(resultSet.getString("comment_text"));
          if (document.prText.length() > PRTEXT_MAXLENGTH) {
            document.prText = document.prText.substring(0, PRTEXT_MAXLENGTH);
            document.prText = Common
                .formatForHTMLText(Common.getFirstMatch("^.*[!♪。\\s]", document.prText));
          }

          document.text = Common.formatForWatson(resultSet.getString("comment_text"));

          document.budget = -1;
          int[] budgets = { resultSet.getInt("budget_party"), resultSet.getInt("budget"),
              resultSet.getInt("budget_lunch") };
          for (int budget : budgets) {
            if (budget != -1) {
              document.budget = budget;
              break;
            }
          }

          document.documentId = Common.makeIdentifier(document.shopId, document.voteId,
              resultSet.getString("logging_id"));
          documents.add(document);
        }
        Common.logging("システム利用者のコメントから", count);
        return null;
      }
    });

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    objectMapper.writeValueAsString(documents);

    File.write(Paths.get(Common.prop("work_path"), "watson_data", "rar_documents."+ Common.getDateStr() + ".json").toString(),
        objectMapper.writeValueAsString(documents));

    insertDocumentRecords(documents);
  }

  private static void insertDocumentRecords(final List<RestaurantDocument> documents) throws Exception {
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection.prepareStatement("delete from document");
        statement.executeUpdate();
        return null;
      }
    });

    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection
            .prepareStatement("insert into document values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        for (RestaurantDocument document : documents) {
          statement.setString(1, document.documentId);
          statement.setString(2, document.shopId);
          statement.setString(3, document.voteId);
          statement.setString(4, document.shopName);
          statement.setString(5, document.shopNameKana);
          statement.setString(6, document.menuName);
          statement.setString(7, document.menuNameKana);
          statement.setString(8, document.latitude);
          statement.setString(9, document.longitude);
          statement.setString(10, document.shopUrl);
          statement.setString(11, document.imageUrl);
          statement.setString(12, document.prText);
          statement.setString(13, document.text);
          statement.setInt(14, document.budget);

          statement.executeUpdate();
        }

        return null;
      }
    });
    Common.logging("end", "CreateRaRDocuments");
  }

}
