package script;

import general.Common;

public class ScriptCaller {
  public static void main(String[] args) {
    // TODO 自動生成されたメソッド・スタブ
    try {
      Common.logging("start", "ScriptCaller");

      // ぐるなびAPIからレストラン情報・応援口コミ情報をDBに登録
      // InsertRestaurantRecords.main(args);
      // InsertKuchikomiRecords.main(args);

      // DBからRetrieve and Rankのドキュメントデータを生成
      // CreateRaRDocuments.main(args);

      // ぐるなびAPI・DBからSolr辞書の元データを生成
      // CreateDictionaryData.main(args);

      // Natural Language Classifierの学習データを生成
      // CreateNLCTrainingData.main(args);

      // Retrieve and Rankの学習データを生成
      // CreateRaRRankerTrainingData.main(args);

      Common.logging("end", "ScriptCaller");
    } catch (Exception e) {
      // TODO 自動生成された catch ブロック
      Common.logging("error", "ScriptCaller");
      e.printStackTrace();
    }
  }
}
