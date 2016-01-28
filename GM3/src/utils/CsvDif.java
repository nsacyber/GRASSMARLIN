package utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * 12.10.2015 - CC - New...
 */
public class CsvDif {

    private static String concat(List<String> sepLine) {
        StringBuilder builder = new StringBuilder();
        sepLine.stream().forEach(s -> {
            if(builder.length()!=0) {
                builder.append(",");
            }
            builder.append(s);
        });
        return builder.toString();
    }

    public static void getDif(String path1, String path2) {
        Path file1 = Paths.get(path1);
        Path file2 = Paths.get(path2);
        ArrayList<List<String>> listOfCsvDataFromFile1 = new ArrayList<>();
        ArrayList<List<String>> listOfCsvDataFromFile2 = new ArrayList<>();
        try(Stream<String> linesFromFile1 = Files.lines(file1);
            Stream<String> linesFromFile2 = Files.lines(file2)) {
            linesFromFile1.forEach(line -> listOfCsvDataFromFile1.add(Arrays.asList(line.split(","))));
            linesFromFile2.forEach(line -> listOfCsvDataFromFile2.add(Arrays.asList(line.split(","))));
            if(listOfCsvDataFromFile1.size() != listOfCsvDataFromFile2.size()) {
                System.err.println("Files have different number of lines.");
            }
            IntStream.range(0,listOfCsvDataFromFile1.size()).forEach(i -> {
                List<String> first = listOfCsvDataFromFile1.get(i);
                List<String> second = listOfCsvDataFromFile2.get(i);
                if(first.size() != second.size()) {
                    System.err.println("Line ["+i+"] is a different size:");
                    System.err.println(concat(first));
                    System.err.println(concat(second));
                }
                IntStream.range(0,first.size()).forEach(y -> {
                    if(!first.get(y).equals(second.get(y))) {
                        System.err.println("Difference in Line ["+i+"]: ["+first.get(y)+"] vs ["+second.get(y)+"]");
                        System.err.println(concat(first));
                        System.err.println(concat(second));
                    }
                });
            });
        }
        catch (IOException e) {
            System.err.println("error reading files");
            e.printStackTrace();
        }


    }

    public static void main(String[] args) {
        if(args.length != 2) {
            System.err.println("Usage:CsvDif filePath1 filePath2");
            System.exit(1);
        }
        else {
            CsvDif.getDif(args[0],args[1]);
        }
    }
}
