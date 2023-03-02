package org.turmi;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * 1. Generate order file like order.csv for given directory with a given ordering type
 * 2. Edit the order.csv file at will
 * 3. Copy to destination location using the edited order.csv file
 */
@Command(name = "subaru-copy", version = "subaru-copy 1.0", mixinStandardHelpOptions = true,
    description = "Allows copying media files in configurable order to Subaru Infotainment compatible USB stick.",
    subcommands = {Order.class, Copy.class, CommandLine.HelpCommand.class})
public class Main {
  @Spec
  CommandSpec spec;

  public static void main(String[] args) {
    System.exit(new CommandLine(new Main()).execute(args));
  }

}