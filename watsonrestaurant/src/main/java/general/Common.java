package general;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;
import java.text.Normalizer.Form;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.net.URLCodec;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.JapaneseTokenizer;
import org.codelibs.neologd.ipadic.lucene.analysis.ja.tokenattributes.ReadingAttribute;

public class Common {

  private static Properties properties;
  static {
    final String FILE_NAME = "watsonrestaurant.properties";
    properties = new Properties();
    try {
      Path path = Paths.get(System.getProperty("user.home"), "work", "watsonrestaurant", "resource", FILE_NAME);
      if (!Files.exists(path)) {
        path = Paths.get(FILE_NAME);
      }
      properties.load(Files.newInputStream(path));
    } catch (IOException e) {
      Common.logging("failed to load properties...");
    }
  }

  public static String prop(String key) {
    return properties.getProperty(key);
  }

  public static String formatAPIResponse(Object value) {
    String newValue = value == null ? "" : value.toString();

    newValue = newValue.replaceAll("^\\{\\}$", "");
    newValue = Normalizer.normalize(newValue, Form.NFKC);
    newValue = newValue.trim();

    return newValue;
  }

  public static String formatForHTMLText(Object value) {
    String newValue = value == null ? "" : value.toString();
    newValue = Normalizer.normalize(newValue, Form.NFKC);
    newValue = newValue.replaceAll("[\\x00-\\x1F\\x7F]", "");
    newValue = newValue.replaceAll("<[Bb][Rr]>", " ");
    newValue = newValue.trim();
    return newValue;
  }

  public static String escapeSolrQueryText(Object value) {
    String[] PATTERNS = {
        "\\\\", "\\+", "\\-", "\\&", "\\|", "\\!", "\\(", "\\)",
        "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~", "\\*",
        "\\?", "\\:", "\\/", "\r", "\n", "\t",
        "\u0020", "\u3000", "AND", "OR", "NOT"
    };
    String newValue = value == null ? "" : value.toString();
    for (String pattern : PATTERNS) {
      newValue = newValue.replaceAll(pattern, "\\\\" + pattern);
    }
    return newValue;
  }

  public static String formatForWatson(Object value) {
    String newValue = value == null ? "" : value.toString();

    // 正規化
    newValue = Normalizer.normalize(newValue, Form.NFKC);

    // 制御文字
    newValue = newValue.replaceAll("[\\x00-\\x1F\\x7F]", "");

    // 音読記号
    newValue = newValue.replaceAll("[、､]+", "、");
    newValue = newValue.replaceAll("[。｡]+", "。");
    newValue = newValue.replaceAll("[―ーｰ〜~]+", "ー");

    // 記号
    newValue = newValue.replaceAll("[「」｢｣『』]", "");

    newValue = newValue.replaceAll("<[Bb][Rr]>", " ");
    newValue = newValue.replaceAll("[【】（）〈〉≪≫《》〔〕｛｝\\(\\)\\{\\}\\[\\]]", " ");
    newValue = newValue.replaceAll("[◇◆・♪○◎☆★□■┗┛━┏┓┳┻┃―→⇒¥▼▽↓●※〇 ゚]", " ");

    // その他
    newValue = newValue.replaceAll("[!-/:-@≠\\[-`{-~]", "");
    newValue = newValue.replaceAll("[ 　]+", " ");
    newValue = newValue.trim();

    return newValue;
  }

  public static String escapeToCSV(Object value) {
    final String ENC = "\"";
    String newValue = (value != null ? value.toString() : "");
    return ENC + newValue.replaceAll(ENC, ENC + ENC) + ENC;
  }

  public static String escapeToURL(Object value) {
    String newValue = value == null ? "" : value.toString();
    try {
      newValue = new URLCodec().encode(newValue, "UTF-8");
    } catch (UnsupportedEncodingException e) {
    }
    return newValue;
  }

  public static int parseInt(Object value, int defaultValue) {
    try {
      return Integer.parseInt(value.toString());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static double parseDouble(Object value, double defaultValue) {
    try {
      return Double.parseDouble(value.toString());
    } catch (Exception e) {
      return defaultValue;
    }
  }

  public static String makeIdentifier(String... args) {
    List<String> id = new ArrayList<String>();
    for (String arg : args) {
      if (arg != null && !"".equals(arg)) {
        id.add(arg);
      }
    }
    return String.join(".", id);
  }

  public static void logging(Object... args) {
    List<String> messages = new ArrayList<String>();
    if (messages != null) {
      for (Object arg : args) {
        messages.add(arg.toString());
      }
    }
    System.out.println(new Date().toString() + " : " + String.join(" ", messages));
  }

  public static String getKatakana(String word) {
    String returnValue = null;
    JapaneseTokenizer tokenizer = new JapaneseTokenizer(null, true, JapaneseTokenizer.Mode.NORMAL);
    try {
      ReadingAttribute readingAttribute = tokenizer.getAttribute(ReadingAttribute.class);
      tokenizer.setReader(new StringReader(word));
      tokenizer.reset();

      StringBuilder stringBuilder = new StringBuilder();
      while (tokenizer.incrementToken()) {
        String reading = readingAttribute.getReading();
        if (reading == null || "".equals(reading)) {
          throw new Exception();
        }
        stringBuilder.append(reading);
      }
      returnValue = stringBuilder.toString();
    } catch (IOException e) {
      // TODO 自動生成された catch ブロック
      returnValue = word;
    } catch (Exception e) {
      Common.logging("Common.getKatakana", "読み方が取得できませんでした", word);
      returnValue = word;
    } finally {
      try {
        tokenizer.close();
      } catch (IOException e) {
      }
    }
    return returnValue;
  }

  public static String getFirstMatch(String pattern, String find) {
    Matcher matcher = Pattern.compile(pattern).matcher(find);
    matcher.find();
    try {
      return matcher.group();
    } catch (IllegalStateException e) {
      return "";
    }
  }

  public static String getDateStr() {
    return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
  }
}
