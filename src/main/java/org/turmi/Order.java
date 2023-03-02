package org.turmi;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "order", description = "Creates a file with order definition", mixinStandardHelpOptions = true)
public class Order implements Runnable {

  public enum Type {NATURAL, TRACK};

  @Option(names = {"-t", "--type"}, paramLabel = "TYPE", description = "Order type, one of ${COMPLETION-CANDIDATES}.")
  Type type = Type.TRACK;

  @Option(names = {"-o", "--output"}, paramLabel = "FILE",
      description = "Output order file, defaults to ./subaru-order.csv.")
  String orderFile = "./subaru-order.csv";

  @Option(names = {"-m", "--mp3"},
      description = "Include only files with mp3 extension")
  boolean mp3Only = false;

  @Parameters(paramLabel = "DIR", arity = "1..*", description = "One or more directories to process.")
  List<Path> inDirs;

  @Override
  public void run() {
    // key: sorted path, value: base path
    TreeMap<String, String> entries = new TreeMap<>(new FileComparator(type));

    inDirs.forEach(inDir -> {
      try (Stream<Path> stream = Files.walk(inDir)) {
        stream.filter(this::includeFile)
            .forEach(path -> entries.put(path.toAbsolutePath().toString(), inDir.toAbsolutePath().toString()));
      } catch (IOException ioex) {
        ioex.printStackTrace();
      }
    });

    // Write to file
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(orderFile, StandardCharsets.UTF_8))) {
      for (var entry : entries.entrySet()) {
        writer.write(String.format("%s;%s\n", entry.getKey(), entry.getValue()));
        System.out.println(entry.getKey());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private boolean includeFile(Path path) {
    return Files.isRegularFile(path) && (!mp3Only || path.getFileName().toString().toLowerCase().endsWith(".mp3"));
  }

  private static class Mp3Metadata {
    Integer diskNumber;
    Integer trackNumber;
  }

  private static class FileComparator implements Comparator<String> {

    // Cache the compared values to avoid unnecessary file metadata read
    private final Map<String, Mp3Metadata> cache = new HashMap<>();
    private final Type type;

    private FileComparator(Type type) {
      this.type = type;
    }

    @Override
    public int compare(String o1, String o2) {
      var p1 = Paths.get(o1);
      var p2 = Paths.get(o2);
      var d1 = p1.getParent().toString();
      var d2 = p2.getParent().toString();
      var dirCompare = d1.compareTo(d2);
      if (dirCompare != 0) {
        return dirCompare;
      }
      var f1 = p1.getFileName().toString();
      var f2 = p2.getFileName().toString();
      if (Type.TRACK.equals(type)) {
        var m1 = getCachedMetadata(o1);
        var m2 = getCachedMetadata(o2);
        if (m1 != null && m2 != null) {
          var dn1 = m1.diskNumber;
          var dn2 = m2.diskNumber;
          if (dn1 != null && dn2 != null) {
            var diskNumberCompare = dn1.compareTo(dn2);
            if (diskNumberCompare != 0) {
              return diskNumberCompare;
            }
            return Integer.compare(m1.trackNumber, m2.trackNumber);
          } else if (dn1 != null) {
            return -1;
          } else if (dn2 != null) {
            return 1;
          }
        } else if (m1 != null) {
          return -1;
        } else if (m2 != null) {
          return 1;
        }
      }
      return f1.compareTo(f2);
    }

    private Mp3Metadata getCachedMetadata(String path) {
      if (cache.containsKey(path)) {
        return cache.get(path);
      }
      var number = getMp3Metadata(Paths.get(path));
      cache.put(path, number);
      return number;
    }

    private Mp3Metadata getMp3Metadata(Path path) {
      if (!isMp3File(path)) {
        return null;
      }
      Metadata metadata;
      try (var input = new FileInputStream(path.toFile())) {
        ContentHandler handler = new DefaultHandler();
        metadata = new Metadata();
        Parser parser = new Mp3Parser();
        ParseContext parseCtx = new ParseContext();
        parser.parse(input, handler, metadata, parseCtx);
      } catch (IOException e) {
        throw new RuntimeException(e);
      } catch (TikaException | SAXException e) {
        // Do nothing, if not possible to get metadata
        metadata = null;
        System.err.printf("Unable to read mp3 metadata of %s%n", path);
      }

      if (metadata == null) {
        return null;
      }

      var trackData = metadata.get("xmpDM:trackNumber");
      Integer trackNumber = null;
      if (trackData != null && !trackData.isBlank()) {
        try {
          trackNumber = Integer.parseInt(trackData.replaceFirst("/.*$", ""));
        } catch (NumberFormatException e) {
          System.err.printf("Unable to parse track number of %s%n", path);
        }
      }

      var diskData = metadata.get("xmpDM:discNumber");
      Integer diskNumber = null;
      if (diskData != null && !diskData.isBlank()) {
        try {
          diskNumber = Integer.parseInt(diskData.replaceFirst("/.*$", ""));
        } catch (NumberFormatException e) {
          System.err.printf("Unable to parse disk number of %s%n", path);
        }
      }

      var data = new Mp3Metadata();
      data.trackNumber = trackNumber;
      data.diskNumber = diskNumber;

      return data;
    }

    private Optional<String> getFileExtension(String filename) {
      return Optional.ofNullable(filename)
          .filter(f -> f.contains("."))
          .map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    private boolean isMp3File(Path path) {
      if (!Files.isRegularFile(path)) {
        return false;
      }
      var ext = getFileExtension(path.toString());
      return ext.isPresent() && "mp3".equalsIgnoreCase(ext.get());
    }

  }

}
