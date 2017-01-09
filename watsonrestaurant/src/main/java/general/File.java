package general;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class File {
  private static final String CHAR_NAME = "UTF-8";

  public static String read(String path) throws IOException {
    return Files.lines(Paths.get(path), Charset.forName(CHAR_NAME))
        .collect(Collectors.joining(System.getProperty("line.separator")));
  }

  public static void write(String path, List<String> lines) throws IOException {
    Files.write(Paths.get(path), lines, Charset.forName(CHAR_NAME), StandardOpenOption.CREATE,
        StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  public static void write(String path, String line) throws IOException {
    write(path, Arrays.asList(line.split(System.getProperty("line.separator"))));
  }
}
