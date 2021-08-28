import java.io.*;
import java.util.*;
import java.util.List;

/**
 * @author Dooann
 * @version 0.1.0
 */
public class DuplicateLinesRemover {

    private static File srcFile; // input file
    private static File outputFile; // output file
    private static List<File> groupFiles = new ArrayList<>(); // group lines by hashcode and write
    private static BufferedReader bufferedReader; // input file reader
    private static List<BufferedWriter> writers = new ArrayList<>(); // writers for group files
    private static Deque<String> outputQueue = new LinkedList<>(); // output buffer
    private static Integer readLimit; // how many lines can be read at once
    private static Integer groupNum; // how many group files will be generated

    public static void main(String[] args) throws IOException {
        long startTime = System.currentTimeMillis();
        init(args);
        groupByHash();
        mergeFiles();
        long endTime = System.currentTimeMillis();
        System.out.println("All done! Total time: " + (endTime - startTime) + "ms");
    }

    private static void init(String[] args) {
        if (args.length < 1) {
            printHelpMessage();
            System.exit(1);
        }
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-i":
                    srcFile = new File(args[++i]);
                    break;
                case "-b":
                    readLimit = Integer.parseInt(args[++i]);
                    break;
                case "-g":
                    groupNum = Integer.parseInt(args[++i]);
                    break;
                case "-o":
                    outputFile = new File(args[++i]);
                    break;
                default:
                    printHelpMessage();
            }
        }
        long srcFileLength = srcFile.length();
        // using default setting
        if (outputFile == null) {
            outputFile = new File(srcFile.getParentFile(), "out.txt");
        }
        if (readLimit == null) {
            readLimit = 100000;
        }
        if (groupNum == null) {
            try {
                groupNum = Math.toIntExact(srcFileLength / (1024 * 1024 * 32)); // 32MiB per group file in average
            } catch (ArithmeticException e) {
                groupNum = Integer.MAX_VALUE;
            }
        }
        System.out.println("Input file size: " + srcFileLength + " Bytes");
    }

    private static void groupByHash() throws IOException {
        System.out.println("Start to read and group lines...");
        bufferedReader = new BufferedReader(new FileReader(srcFile));
        if (srcFile == null || !srcFile.exists()) {
            return;
        }
        long startTime = System.currentTimeMillis();
        initFiles(srcFile, groupNum);
        initWriters();
        boolean eof = false;
        while (!eof) {
            eof = read();
            write();
        }
        closeWriters();
        bufferedReader.close();
        long endTime = System.currentTimeMillis();
        System.out.println("Done! Time: " + (endTime - startTime) + "ms");
    }

    private static void mergeFiles() throws IOException {
        System.out.println("Start to remove duplicate lines and merge files...");
        long startTime = System.currentTimeMillis();
        int totalLines = 0;
        int totalDupLines = 0;
        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile));
        for (File file : groupFiles) {
            HashSet<String> lines = new HashSet<>();
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            int count = 0;
            while ((line = br.readLine()) != null) {
                lines.add(line);
                count++;
            }
            br.close();
            if (!file.delete()) {
                System.out.println(file + ": Delete failed.");
            }

            totalLines += count;
            totalDupLines += count - lines.size();
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                bw.write(iterator.next());
                bw.newLine();
            }
        }
        bw.flush();
        bw.close();
        long endTime = System.currentTimeMillis();
        System.out.println("Done! Time: " + (endTime - startTime) + "ms");
        System.out.println("Total lines: " + totalLines + ", Duplicate lines: " + totalDupLines);
    }

    private static void printHelpMessage() {
        System.out.println("Usage: java DuplicateLinesRemover -i <inputFile> [-o <outputFile>] [-l <maxBufferLines>] [-n <groupNumber>]");
        System.out.println("    -i: <inputFile>: The path of input file.");
        System.out.println("    -o: <outputFile>: The path of output file.");
        System.out.println("    -b: <maxBufferLines>: The maximum number of lines can be read at once.");
        System.out.println("    -g: <groupNumber>: The number of files that <inputFile> will be split into.");
        System.out.println("(Make sure that remaining storage size is greater than the size of <inputFile>)");
    }

    private static void write() throws IOException {
        while (!outputQueue.isEmpty()) {
            String string = outputQueue.poll();
            int hash = Math.abs(string.hashCode()) % groupNum;
            BufferedWriter writer = writers.get(hash);
            writer.write(string);
            writer.newLine();
        }
    }

    private static boolean read() throws IOException {
        while (outputQueue.size() < readLimit) {
            String string = bufferedReader.readLine();
            if (string != null) {
                outputQueue.offer(string);
            } else {
                return true; // EOF
            }
        }
        return false;
    }

    private static void initFiles(File srcFile, int groupNum) throws IOException {
        if (srcFile.getUsableSpace() < srcFile.length()) {
            throw new IOException("Insufficient remaining storage.");
        }
        File parentDir= srcFile.getParentFile();
        File tempDir = new File(parentDir + "/temp");
        if (!tempDir.exists() && tempDir.mkdir()) {
            tempDir.deleteOnExit();
        }
        for (int i = 0; i < groupNum; i++) {
            String fileName = i + ".txt";
            File file = new File(tempDir, fileName);
            if (!file.exists()) {
                file.createNewFile();
            }
            groupFiles.add(file);
        }
    }

    private static void initWriters() throws IOException {
        for (File file : groupFiles) {
            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
            writers.add(bufferedWriter);
        }
    }

    private static void closeWriters() throws IOException {
        for (BufferedWriter writer : writers) {
            writer.flush();
            writer.close();
        }
    }
}
