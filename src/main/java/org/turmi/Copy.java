package org.turmi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "copy", description = "Copies files defined in order file to a target destination.",
    mixinStandardHelpOptions = true)
public class Copy implements Runnable {

  @Option(names = {"-i", "--input"}, paramLabel = "FILE",
      description = "Input order file, defaults to ./subaru-order.csv.")
  String orderFile = "./subaru-order.csv";

  @Option(names = {"-c", "--cleanspaces"}, paramLabel = "FILE",
      description = "Replaces spaces in target path with underscores.")
  boolean cleanSpaces = false;

  @Parameters(paramLabel = "TARGET", description = "Target destination.")
  Path target;

  @Override
  public void run() {

    if (!Files.exists(target) || !Files.isDirectory(target)) {
      throw new RuntimeException("Target must be an existing directory");
    }
    List<String> lines;
    try {
      lines = Files.readAllLines(Paths.get(orderFile), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    for (var line : lines) {
      var tags = line.split(";");
      if (tags.length != 2) {
        throw new RuntimeException("Invalid file format");
      }
      var source = Paths.get(tags[0]);
      var baseDir = Paths.get(tags[1]);
      var relativePath = baseDir.relativize(source);
      if (cleanSpaces) {
        relativePath = Paths.get(relativePath.toString().replaceAll(" ", "_"));
      }
      var filename = relativePath.getFileName();

      try {
        var destinationDir = Files.createDirectories(target.resolve(relativePath.getParent()));
        var destinationFile = destinationDir.resolve(filename);
        Files.copy(source, destinationFile);
        System.out.printf("Copied: %s%n", destinationFile);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }
}
