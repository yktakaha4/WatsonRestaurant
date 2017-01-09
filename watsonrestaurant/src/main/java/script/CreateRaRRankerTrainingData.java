package script;

import java.math.BigDecimal;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import general.Common;
import general.DBClient;
import general.ExecuteQuery;
import general.File;
import general.GroundTruth;

public class CreateRaRRankerTrainingData {
  public static void main(String[] args) throws Exception {
    // TODO 自動生成されたメソッド・スタブ
    final GroundTruth groundTruth = new GroundTruth();
    final BigDecimal CONFIDENCE_CATEGORY = new BigDecimal("+1.5");
    final BigDecimal CONFIDENCE_SHOWDETAIL = new BigDecimal("+0.05");
    final BigDecimal CONFIDENCE_POSITIVE = new BigDecimal("+0.5");
    final BigDecimal CONFIDENCE_NEGATIVE = new BigDecimal("-1.0");
    final BigDecimal CONFIDENCE_POSITIVE_ALLQUERY = new BigDecimal("+0.05");
    final BigDecimal CONFIDENCE_NEGATIVE_ALLQUERY = new BigDecimal("-0.1");

    // カテゴリーから質問文を生成し、カテゴリーと部分一致するドキュメントに加点する
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        final List<String> queries = new ArrayList<String>();
        {
          PreparedStatement statement = connection
              .prepareStatement("select distinct category from restaurant");
          ResultSet resultSet = statement.executeQuery();

          while (resultSet.next()) {
            for (String query : resultSet.getString(1).split("[・\\s\\(\\)]")) {
              if (query.length() == 0) {
                continue;
              }
              if (query.contains("その他")) {
                continue;
              }
              if (queries.contains(query)) {
                continue;
              }
              queries.add(query);
            }
          }
        }
        {
          PreparedStatement statement = connection
              .prepareStatement("select document_id from document as doc join restaurant as res"
                  + " on doc.shop_id = res.shop_id and res.category like ?");

          for (String query : queries) {
            statement.setString(1, "%" + query + "%");

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
              groundTruth.add(Common.escapeSolrQueryText(query), resultSet.getString(1), CONFIDENCE_CATEGORY);
            }
          }
        }
        return null;
      }
    });

    // メニュー名から質問文を生成し、ドキュメントに加点する
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        {
          PreparedStatement statement = connection
              .prepareStatement("select kuc.menu_name, doc.document_id from kuchikomi as kuc"
                  + " join document as doc on kuc.shop_id = doc.shop_id"
                  + " and kuc.vote_id = doc.vote_id");
          ResultSet resultSet = statement.executeQuery();

          while (resultSet.next()) {
            groundTruth.add(Common.escapeSolrQueryText(resultSet.getString(1)), resultSet.getString(2),
                CONFIDENCE_CATEGORY);
          }
        }
        return null;
      }
    });

    // 詳細が閲覧されたドキュメントに加点する
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        {
          PreparedStatement statement = connection.prepareStatement(
              "select log.query_text, doc.document_id from logging as log join document as doc"
                  + " on log.shop_id = doc.shop_id and log.vote_id = doc.vote_id"
                  + " and log.logging_type like ?");
          statement.setString(1, Common.prop("logging_show_detail") + "%");

          ResultSet resultSet = statement.executeQuery();
          while (resultSet.next()) {
            groundTruth.add(Common.escapeSolrQueryText(resultSet.getString(1)), resultSet.getString(2),
                CONFIDENCE_SHOWDETAIL);
          }
        }
        return null;
      }
    });

    // コメントが行われたドキュメントに加減点する
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        {
          PreparedStatement statement = connection.prepareStatement(
              "select log.query_text, doc.document_id, log.comment_class, log.comment_text"
                  + " from logging as log"
                  + " join document as doc where log.shop_id = doc.shop_id"
                  + " and log.vote_id = doc.vote_id and log.logging_type like ?");
          statement.setString(1, Common.prop("logging_comment"));

          ResultSet resultSet = statement.executeQuery();
          while (resultSet.next()) {
            BigDecimal confidence = resultSet.getString(3) == Common.prop("nlc_class_good")
                ? CONFIDENCE_POSITIVE : CONFIDENCE_NEGATIVE;
            groundTruth.add(Common.escapeSolrQueryText(resultSet.getString(1)), resultSet.getString(2),
                confidence);
            groundTruth.add(Common.escapeSolrQueryText(resultSet.getString(4)), resultSet.getString(2),
                confidence);
          }
        }
        return null;
      }
    });

    // 検索された全ての質問文と全ての学習データに対して、口コミ、ユーザーコメントで高評価/低評価の店舗を加減点する
    {
      final List<String> queries = new ArrayList<String>();
      final Map<String, BigDecimal> umasoConfidences = new HashMap<String, BigDecimal>();
      final Map<String, BigDecimal> loggingConfidences = new HashMap<String, BigDecimal>();
      DBClient.execute(new ExecuteQuery() {
        public Object call(Connection connection) throws Exception {
          // TODO 自動生成されたメソッド・スタブ
          // 検索された全てのクエリを取得
          {
            PreparedStatement statement = connection
                .prepareStatement("select distinct query_text from logging");
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
              queries.add(resultSet.getString(1));
            }
          }
          // 口コミでうまそうと評価されている
          {
            PreparedStatement statement = connection
                .prepareStatement("select doc.document_id, kuc.umaso_count_sum from document as doc"
                    + " join (select shop_id, sum(umaso_count) as umaso_count_sum from kuchikomi"
                    + " where umaso_count > ? group by shop_id) as kuc on doc.shop_id = kuc.shop_id");
            statement.setInt(1, 0);

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
              BigDecimal confidence = CONFIDENCE_POSITIVE_ALLQUERY.multiply(new BigDecimal(resultSet.getInt(2)));
              umasoConfidences.put(resultSet.getString(1), confidence);
            }
          }
          // ユーザーコメントで評価がされている
          {
            PreparedStatement statement = connection
                .prepareStatement("select shop_id,"
                    + " sum(case(comment_class) when ? then 1 else 0 end),"
                    + " sum(case(comment_class) when ? then 1 else 0 end)"
                    + " from logging group by shop_id");
            statement.setString(1, Common.prop("nlc_class_good"));
            statement.setString(2, Common.prop("nlc_class_bad"));

            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
              BigDecimal positiveConfidence = CONFIDENCE_POSITIVE_ALLQUERY
                  .multiply(new BigDecimal(resultSet.getInt(2)));
              BigDecimal negativeConfidence = CONFIDENCE_NEGATIVE_ALLQUERY
                  .multiply(new BigDecimal(resultSet.getInt(3)));
              loggingConfidences.put(resultSet.getString(1), positiveConfidence.add(negativeConfidence));
            }
          }
          return null;
        }
      });
      for (String query : groundTruth.getQueries()) {
        if (!queries.contains(query)) {
          queries.add(query);
        }
      }
      for (String query : queries) {
        for (String id : umasoConfidences.keySet()) {
          groundTruth.add(query, id, umasoConfidences.get(id));
        }
        for (String id : loggingConfidences.keySet()) {
          groundTruth.add(query, id, loggingConfidences.get(id));
        }
      }
    }

    groundTruth.validate();

    File.write(
        Paths.get(Common.prop("work_path"), "watson_data", "rar_training." + Common.getDateStr() + ".csv").toString(),
        groundTruth.toLines());
  }
}
