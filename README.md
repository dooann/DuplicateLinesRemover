# DuplicateLinesRemover
Effectively removes duplicate lines from a large file that cannot be read into memory.

Usage:

```bash
java DuplicateLinesRemover -i <inputFile> [-o <outputFile>] [-l <maxBufferLines>] [-n <groupNumber>]
    -i: <inputFile>: The path of input file.
    -o: <outputFile>: The path of output file.
    -b: <maxBufferLines>: The maximum number of lines can be read at once.
    -g: <groupNumber>: The number of files that <inputFile> will be split into.
(Make sure that remaining storage size is greater than the size of <inputFile>)
```

Example:

```bash
java DuplicateLinesRemover -i D:/temp/data.txt
```

![example](https://github.com/dooann/DuplicateLinesRemover/blob/main/assets/example.png)