package script;

import general.APIClient;
import general.Common;
import general.DBClient;
import general.ExecuteQuery;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InsertAreaMasterRecords {

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws Exception {
		// TODO 自動生成されたメソッド・スタブ
		Common.logging("start", "InsertAreaMasterRecords");
		DBClient.execute(new ExecuteQuery() {
			public Object call(Connection connection) throws Exception {
				// TODO 自動生成されたメソッド・スタブ
				PreparedStatement statement = connection
						.prepareStatement("delete from area_master");
				statement.executeUpdate();
				return null;
			}
		});

		HashMap<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("keyid", Common.prop("gurunavi_apikey"));
		parameters.put("format", "json");

		final Map<String, Object> response = APIClient.getGuruNaviResponse(
				"master/AreaSearchAPI/20150630", parameters);

		if (response.get("error") != null) {
			return;
		}

		DBClient.execute(new ExecuteQuery() {
			public Object call(Connection connection) throws Exception {
				// TODO 自動生成されたメソッド・スタブ
				PreparedStatement statement = connection
						.prepareStatement("insert into area_master values (?, ?)");

				List<String> idList = new ArrayList<String>();
				for (Map<String, Object> area : (List<Map<String, Object>>) response
						.get("area")) {
					String id = area.get("area_code").toString();
					if (idList.contains(id)) {
						continue;
					}
					idList.add(id);

					statement.setString(1, Common.formatAPIResponse(id));
					statement.setString(2,
							Common.formatAPIResponse(area.get("area_name")));

					statement.executeUpdate();
				}
				return null;
			}
		});

		Common.logging("end", "InsertAreaMasterRecords");
	}
}
