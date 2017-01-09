package script;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import general.APIClient;
import general.Common;
import general.DBClient;
import general.ExecuteQuery;

public class InsertRestaurantRecords {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    // TODO 自動生成されたメソッド・スタブ
    Common.logging("start", "InsertRestaurantRecords");
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection.prepareStatement("delete from restaurant");
        statement.executeUpdate();
        return null;
      }
    });

    int offsetPage = 0;
    while (true) {
      Thread.sleep(1000);
      offsetPage++;

      HashMap<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("keyid", Common.prop("gurunavi_apikey"));
      parameters.put("format", "json");

      parameters.put("coordinates_mode", Common.prop("gurunavi_req_coordinatesmode"));
      parameters.put("latitude", Common.prop("gurunavi_req_latitude"));
      parameters.put("longitude", Common.prop("gurunavi_req_longitude"));
      parameters.put("range", Common.prop("gurunavi_req_range"));

      parameters.put("hit_per_page", 500);
      parameters.put("offset_page", offsetPage);

      final Map<String, Object> response = APIClient.getGuruNaviResponse("RestSearchAPI/20150630", parameters);

      if (response.get("error") != null) {
        break;
      }

      DBClient.execute(new ExecuteQuery() {
        public Object call(Connection connection) throws Exception {
          // TODO 自動生成されたメソッド・スタブ
          PreparedStatement statement = connection.prepareStatement(
              "insert into restaurant values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

          List<String> idList = new ArrayList<String>();
          for (Map<String, Object> rest : (List<Map<String, Object>>) response.get("rest")) {
            String id = rest.get("id").toString();
            if (idList.contains(id)) {
              continue;
            }
            idList.add(id);

            statement.setString(1, Common.formatAPIResponse(id));
            statement.setString(2, Common.formatAPIResponse(rest.get("name")));
            statement.setString(3, Common.formatAPIResponse(rest.get("name_kana")));
            statement.setString(4, Common.formatAPIResponse(rest.get("url").toString().split("\\?")[0]));

            statement.setString(5, Common.formatAPIResponse(rest.get("latitude")));
            statement.setString(6, Common.formatAPIResponse(rest.get("longitude")));
            statement.setString(7, Common.formatAPIResponse(rest.get("opentime")));
            statement.setString(8, Common.formatAPIResponse(rest.get("holiday")));

            Map<String, Object> imageUrl = (Map<String, Object>) rest.get("image_url");
            statement.setString(9, Common.formatAPIResponse(imageUrl.get("shop_image1")));

            Map<String, Object> pr = (Map<String, Object>) rest.get("pr");
            statement.setString(10, Common.formatAPIResponse(pr.get("pr_long")));
            String prShort = Common.formatAPIResponse(pr.get("pr_short"));
            statement.setString(11, prShort);

            List<Object> categories = (List<Object>) ((Map<String, Object>) rest.get("code"))
                .get("category_name_s");
            List<String> categoryList = new ArrayList<String>();
            for (Object category : categories) {
              if (category instanceof String) {
                categoryList.add(category.toString());
              }
            }
            statement.setString(12, Common.formatAPIResponse(String.join(" ", categoryList)));
            if ("".equals(prShort)) {
              statement.setString(11, String.join("。", categoryList));
            }

            statement.setInt(13, Common.parseInt(rest.get("budget"), -1));
            statement.setInt(14, Common.parseInt(rest.get("party"), -1));
            statement.setInt(15, Common.parseInt(rest.get("lunch"), -1));

            statement.setString(16, Common.formatAPIResponse(rest.get("update_date")));

            statement.executeUpdate();
          }
          return null;
        }
      });
    }
    Common.logging("end", "InsertRestaurantRecords");
  }
}
