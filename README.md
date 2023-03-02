# subaru-copy
Small utility program that allows copying media files in custom order to Subaru infotainment compatible USB stick

## Introduction
As it happens with many car infotainment systems, they are 'far from ideal'.
One of the specific problems I had with my Subaru was the way it read media files form USB drive.
I don't remember exactly, but I read somewhere that due to some licence limitations, the audio device can only read files in the order they were written to file system.
Unfortunately, when using MS Windows to copy media files, the order is totally random.

Crude, but effective solution to that problem is to write the files in predefined order.

This program is the solution. So, how does it work?
1. Use __**order**__ command to create a file consisting of entries to copy.
2. Review the file and change the order at will.
3. Use __**copy**__ command to copy the files in the order defined in the file.

General usage:
```shell
$ java -jar target/subaru-copy-jar-with-dependencies.jar help
Usage: subaru-copy [-hV] [COMMAND]
Allows copying media files in configurable order to Subaru Infotainment
compatible USB stick.
  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.
Commands:
  order  Creates a file with order definition
  copy   Copies files defined in order file to a target destination.
  help   Display help information about the specified command.
```

## Building

To build the program you need Java and Maven.
Build command:
```shell
mvn clean package
```
from within project directory, will create subaru-copy-jar-with-dependencies.jar in target subdirectory.

## Creating the required order

Command __order__ usage:
```shell
$ java -jar target\subaru-copy-jar-with-dependencies.jar help order
Usage: subaru-copy order [-hmsV] [-o=FILE] [-t=TYPE] DIR...
Creates a file with order definition
      DIR...          One or more directories to process.
  -h, --help          Show this help message and exit.
  -m, --mp3           Include only files with mp3 extension.
  -o, --output=FILE   Output order file, defaults to ./subaru-order.csv.
  -s, --subdirs       Process subdirectories instead.
  -t, --type=TYPE     Order type, one of NATURAL, TRACK.
  -V, --version       Print version information and exit.
```

The order command creates ./subaru-order.csv (can be changed if necessary) that contains the files from the DIR directories already pre-sorted.
Sorting is performed using natural order for directories and disk/track numbers for mp3 files.
If you prefer or need to use natural order for mp3 files (i.e. ID3 tags are not present), use option --type=NATURAL.

By default all files found will be included in the list, but if you prefer to copy just mp3 files use the -m/--mp3 option.

The -s/--subdirs option allows treating the DIR directories as containers. For those, sub-directories of the container directory will be copied instead.

For example, if you want to copy all subdirectories of directory /home/mirek/Music/ use the following command:

```shell
$ java -jar target/subaru-copy-jar-with-dependencies.jar order --subdirs /home/mirek/Music/
```

Another example is copying the selected directories themselves:
```shell
$ java -jar target/subaru-copy-jar-with-dependencies.jar order /home/mirek/Music/ABBA/ /home/mirek/Music/Elbow/
```
In this case directories ABBA and Elbow will be copied to destination.

If the order of files is still not what you expect, you can always edit the ./subaru-order.csv. Simply change the order of entries just moving the lines up or down. Do not change the lines themselves.

## Copying to the destination (presumably an USB stick)

Before copying consider performing quick format on the stick. That way file system will be cleaned.

Copying is rather straightforward. Just use the file generated before as input.

Command __copy__ usage:
```shell
$ java -jar target/subaru-copy-jar-with-dependencies.jar help copy
Usage: subaru-copy copy [-chV] [-i=FILE] TARGET
Copies files defined in order file to a target destination.
      TARGET          Target destination.
  -c, --cleanspaces   Replaces spaces in target path with underscores.
  -h, --help          Show this help message and exit.
  -i, --input=FILE    Input order file, defaults to ./subaru-order.csv.
  -V, --version       Print version information and exit.

```

The only option to copy command is -c/--cleanspaces. If not used, the path created on the target will the same as in source directories.
