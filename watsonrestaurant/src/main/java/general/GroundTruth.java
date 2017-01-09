package general;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GroundTruth {
  private Map<String, Map<String, BigDecimal>> groundTruth = new HashMap<String, Map<String, BigDecimal>>();

  public void add(String query, String id, BigDecimal confidence)
      throws NumberFormatException {
    if (!groundTruth.containsKey(query)) {
      groundTruth.put(query, new HashMap<String, BigDecimal>());
    }
    Map<String, BigDecimal> groundTruthRow = groundTruth.get(query);
    if (groundTruthRow.containsKey(id)) {
      groundTruthRow.put(id, groundTruthRow.get(id).add(confidence));
    } else {
      groundTruthRow.put(id, confidence);
    }
  }

  @SuppressWarnings("unchecked")
  public void validate() throws Exception {
    Common.logging("start", "GroundTruth.validate");

    int queryCount = 1;
    String[] queries = groundTruth.keySet().toArray(new String[0]);
    for (String query : queries) {
      Map<String, Object> parameters = new HashMap<String, Object>();
      parameters.put("q", query);
      parameters.put("fl", "*");
      parameters.put("rows", Common.parseInt(Common.prop("watson_rar_select_rows"), 0));

      Map<String, BigDecimal> groundTruthRow = groundTruth.get(query);

      List<String> responseIdList = new ArrayList<String>();
      for (Map<String, Object> document : (List<Map<String, Object>>) ((Map<String, Object>) APIClient
          .getRaRSelectResponse(parameters).get("response")).get("docs")) {
        responseIdList.add(document.get("id").toString());
      }
      String[] groundTruthIdList = groundTruthRow.keySet().toArray(new String[0]);
      for (String id : groundTruthIdList) {
        if (!responseIdList.contains(id)) {
          groundTruthRow.remove(id);
        }
      }
      if (groundTruthRow.isEmpty()) {
        groundTruth.remove(query);
      }
      Common.logging(
          "progress:", queryCount, "/", queries.length,
          "found:", groundTruthRow.size(), "/", groundTruthIdList.length,
          "responseSize:", responseIdList.size(),
          "query:", query);
      queryCount++;
    }

    Common.logging("end", "GroundTruth.validate");
  }

  public List<String> toLines() {
    List<String> lines = new ArrayList<String>();

    for (String query : groundTruth.keySet()) {
      List<String> line = new ArrayList<String>();
      line.add(Common.escapeToCSV(Common.escapeToURL(query)));

      for (String id : groundTruth.get(query).keySet()) {
        line.add(Common.escapeToCSV(id));
        int confidence = groundTruth.get(query).get(id)
            .setScale(0, RoundingMode.UP).intValue();
        confidence = Math.min(Math.max(confidence, 0), 4);
        line.add(Common.escapeToCSV(new Integer(confidence)));
      }
      lines.add(String.join(",", line));
    }
    return lines;
  }

  public Set<String> getQueries() {
    return groundTruth.keySet();
  }

}
