package general;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RestaurantDocument {
  @JsonProperty("id")
  public String documentId;

  @JsonProperty("shop_id")
  public String shopId;

  @JsonProperty("vote_id")
  public String voteId;

  @JsonProperty("shop_name")
  public String shopName;

  @JsonProperty("shop_name_kana")
  public String shopNameKana;

  @JsonProperty("menu_name")
  public String menuName;

  @JsonProperty("menu_name_kana")
  public String menuNameKana;

  @JsonProperty("latitude")
  public String latitude;

  @JsonProperty("longitude")
  public String longitude;

  @JsonProperty("shop_url")
  public String shopUrl;

  @JsonProperty("image_url")
  public String imageUrl;

  @JsonProperty("pr_text")
  public String prText;

  @JsonProperty("shop_text")
  public String text;

  @JsonProperty("budget")
  public int budget;
}
