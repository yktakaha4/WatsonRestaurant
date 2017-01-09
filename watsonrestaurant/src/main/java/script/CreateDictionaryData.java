package script;

import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import general.APIClient;
import general.Common;
import general.DBClient;
import general.ExecuteQuery;
import general.File;

public class CreateDictionaryData {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    // TODO 自動生成されたメソッド・スタブ
    Common.logging("start", "CreateDictionaryData");

    final String DATE_STR = Common.getDateStr();

    // 店名、メニュー名をユーザー辞書に追加
    final Map<String, List<String>> userDict = new HashMap<String, List<String>>();
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        final String PATTERN = "[\\s,]";
        final int WORD_MAXLENGTH = 6;

        PreparedStatement statement = connection.prepareStatement(
            "select shop_name as name, shop_name_kana as kana, 'shop' as type from document"
                + " union all select menu_name, '', 'menu' as type from kuchikomi");
        ResultSet resultSet = statement.executeQuery();

        while (resultSet.next()) {
          List<String> userDictRow = new ArrayList<String>();
          String shopName = resultSet.getString("name").replaceAll(PATTERN, "");
          String shopNameKana = resultSet.getString("kana").replaceAll(PATTERN, "");
          if ("menu".equals(resultSet.getString("type"))) {
            shopNameKana = Common.getKatakana(shopName);
          }

          if (shopName.length() > WORD_MAXLENGTH || shopNameKana.length() > WORD_MAXLENGTH * 2) {
            continue;
          }
          if (shopName.length() == 0) {
            continue;
          }

          userDictRow.add(shopName);
          userDictRow.add(shopName);
          userDictRow.add(shopNameKana);
          userDictRow.add("カスタム名詞");

          if (!userDict.containsKey(shopName)) {
            userDict.put(shopName, userDictRow);
          }
        }
        return null;
      }
    });

    // カテゴリ情報をユーザー辞書、同義語辞書に追加
    final String SYNONYMS_PATTERN = "[・\\(\\)\\（\\）]";

    Map<String, List<String>> synonyms = new HashMap<String, List<String>>();

    Map<String, List<String>> categorySList = new HashMap<String, List<String>>();
    {
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("keyid", Common.prop("gurunavi_apikey"));
      parameters.put("format", "json");
      Map<String, Object> response = APIClient.getGuruNaviResponse("master/CategorySmallSearchAPI/20150630",
          parameters);
      for (Map<String, Object> category : (List<Map<String, Object>>) response.get("category_s")) {
        String categoryLCode = category.get("category_l_code").toString();
        if (!categorySList.containsKey(categoryLCode)) {
          categorySList.put(categoryLCode, new ArrayList<String>());
        }
        for (String categorySName : category.get("category_s_name").toString().split(SYNONYMS_PATTERN)) {
          categorySName = Common.formatForWatson(categorySName);

          if (categorySName.length() == 0) {
            continue;
          }
          if (categorySName.contains("その他")) {
            continue;
          }
          categorySList.get(categoryLCode).add(categorySName);
          if (!userDict.containsKey(categorySName)) {
            List<String> userDictRow = new ArrayList<String>();
            userDictRow.add(categorySName);
            userDictRow.add(categorySName);
            userDictRow.add(Common.getKatakana(categorySName));
            userDictRow.add("カスタム名詞");

            userDict.put(categorySName, userDictRow);
          }
        }
      }
    }
    {
      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("keyid", Common.prop("gurunavi_apikey"));
      parameters.put("format", "json");
      Map<String, Object> response = APIClient.getGuruNaviResponse("master/CategoryLargeSearchAPI/20150630",
          parameters);
      for (Map<String, Object> category : (List<Map<String, Object>>) response.get("category_l")) {
        String categoryLCode = category.get("category_l_code").toString();
        String categoryLName = category.get("category_l_name").toString().replaceAll(SYNONYMS_PATTERN, " ");

        if (categoryLName.length() == 0) {
          continue;
        }
        if (categoryLName.contains("その他")) {
          continue;
        }
        List<String> sList = categorySList.get(categoryLCode);
        for (String categoryName : categoryLName.split("\\s+")) {
          if (!sList.contains(categoryName)) {
            sList.add(categoryName);
          }
          if (!userDict.containsKey(categoryName)) {
            List<String> userDictRow = new ArrayList<String>();
            userDictRow.add(categoryName);
            userDictRow.add(categoryName);
            userDictRow.add(Common.getKatakana(categoryName));
            userDictRow.add("カスタム名詞");

            userDict.put(categoryLName, userDictRow);
          }
        }
        synonyms.put(categoryLName, sList);
      }
    }

    {
      List<String> lines = new ArrayList<String>();
      lines.add("#内容を確認の上、修正をおこなって下さい");
      for (List<String> values : userDict.values()) {
        lines.add(String.join(",", values));
      }
      String path = Paths.get(Common.prop("work_path"), "watson_data", "userdict_ja." + DATE_STR + ".txt").toString();
      File.write(path, lines);
    }
    {
      List<String> lines = new ArrayList<String>();
      lines.add("#内容を確認の上、修正をおこなって下さい");
      for (String key : synonyms.keySet()) {
        String line = key + " => " + String.join(" ", synonyms.get(key));
        lines.add(line);
      }
      String path = Paths.get(Common.prop("work_path"), "watson_data", "synonyms." + DATE_STR + ".txt").toString();
      File.write(path, lines);
    }
    Common.logging("end", "CreateDictionaryData");
  }
}
