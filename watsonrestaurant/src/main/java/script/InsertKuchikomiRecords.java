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

public class InsertKuchikomiRecords {

  @SuppressWarnings("unchecked")
  public static void main(String[] args) throws Exception {
    // TODO 自動生成されたメソッド・スタブ
    Common.logging("start", "InsertKuchikomiRecords");
    DBClient.execute(new ExecuteQuery() {
      public Object call(Connection connection) throws Exception {
        // TODO 自動生成されたメソッド・スタブ
        PreparedStatement statement = connection.prepareStatement("delete from kuchikomi");
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

      parameters.put("latitude", Common.prop("gurunavi_req_latitude"));
      parameters.put("longitude", Common.prop("gurunavi_req_longitude"));
      parameters.put("range", Common.prop("gurunavi_req_range"));

      parameters.put("hit_per_page", 50);
      parameters.put("offset_page", offsetPage);

      final Map<String, Object> response = APIClient.getGuruNaviResponse("PhotoSearchAPI/20150630", parameters);

      if (response.get("gnavi") != null) {
        break;
      }

      DBClient.execute(new ExecuteQuery() {
        public Object call(Connection connection) throws Exception {
          // TODO 自動生成されたメソッド・スタブ
          PreparedStatement statement = connection
              .prepareStatement("insert into kuchikomi values (?, ?, ?, ?, ?, ?, ?, ?, ?)");

          List<String> idList = new ArrayList<String>();
          int photoCount = 1;
          while (((Map<String, Object>) response.get("response"))
              .get(((Integer) photoCount).toString()) != null) {
            Map<String, Object> photo = (Map<String, Object>) ((Map<String, Object>) ((Map<String, Object>) response
                .get("response")).get(((Integer) photoCount).toString())).get("photo");

            String voteId = photo.get("vote_id").toString();
            if (idList.contains(voteId)) {
              photoCount++;
              continue;
            }
            idList.add(voteId);

            statement.setString(1, Common.formatAPIResponse(voteId));
            statement.setString(2, Common.formatAPIResponse(photo.get("shop_id")));
            statement.setString(3, Common.formatAPIResponse(photo.get("shop_name")));
            statement.setString(4, Common.formatAPIResponse(photo.get("menu_name")));
            statement.setString(5, Common.formatAPIResponse(photo.get("comment")));

            statement.setString(6, "");
            Map<String, Object> imageUrl = (Map<String, Object>) photo.get("image_url");
            Object[] urls = { imageUrl.get("url_1024"), imageUrl.get("url_320"), imageUrl.get("url_250"),
                imageUrl.get("url_200") };
            for (Object url : urls) {
              if (url != null) {
                statement.setString(6, Common.formatAPIResponse(url));
                break;
              }
            }

            statement.setDouble(7, Common.parseDouble(photo.get("total_score"), -1));
            statement.setInt(8, Common.parseInt(photo.get("umaso_count"), -1));
            statement.setString(9, Common.formatAPIResponse(photo.get("update_date")));

            statement.executeUpdate();

            photoCount++;
          }
          return null;
        }
      });
    }
    Common.logging("end", "InsertKuchikomiRecords");
  }
}
