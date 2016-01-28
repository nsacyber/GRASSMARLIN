/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.importmodule.parser.cisco;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;

/**
 * This is meant to be the try-with-resources condition for {@link CiscoReader}.
 * This pre-parser Splits a single cisco config file into several valid command
 * files. This class fulfills its purpose upon construction.
 *
 * Given an input file with multiple "command-lines" like 'Switch1#show
 * interfaces', this will split the input file smaller files starting with the
 * "command-line", and ending with the line above the next "command-line"
 * encountered or the EOF.
 *
 * It will then map each of these "command-lines" to the File objects created by
 * splitting the input file. The key in the "command-line" to File map
 * {@link #splitFiles} is any text found after the {@link #NEW_COMMAND_TEXT}.
 *
 * The {@link #deviceName} is found before the occurrence of
 * {@link #NEW_COMMAND_SEP}.
 *
 * If successful CiscoCommandSplitter... 1. WILL have a valid device name {@link #getDeviceName()
 * }
 * 2. WILL have a valid map of temp null {@link #getSplitMap()
 * }. 3. WILL mark all temp files to be deleted upon process exit.
 *
 * Note, silent failures may be seen if {@link #getDeviceName() } tests true for {@link String#isEmpty()
 * }. Debug, {@link #debug}, if true, reports valid and invalid command sections
 * and copied lines by line number in the source file. Debug,
 * {@link #deleteOnExit}, if false will leave the files once the process exits.
 * Debug, {@link #debugOutputPath}, can be set an alternative output directory,
 * changes return of {@link #getOuputDirectory() }.
 *
 *
 * BESTDOG - 9/15/15 - init
 */
class CiscoCommandSplitter implements AutoCloseable {

    public static boolean debug = false;
    public static boolean deleteOnExit = true;
    public static String debugOutputPath = null;
    /**
     * character found on a line of a new command
     */
    static final String NEW_COMMAND_SEP = "#";
    /**
     * word indicating a "show" command
     */
    static final String NEW_COMMAND_TEXT = "show";
    /**
     * number of lines required for a valid command, less than this number is
     * thrown out
     */
    static final int LINES_REQUIRED = 5;
    /* original file */
    File originalFile;
    /**
     * map of the first line of each file ( the line with the command ) to the
     * split file.
     */
    Map<String, File> splitFiles;
    /**
     * internal reader for the input file
     */
    BufferedReader reader;
    /**
     * last line containg a new command
     */
    String lastCommandLine;
    /**
     * count of line between commands, too few and we throw out the command
     */
    int linesInCommand;
    /**
     * line number containing last seen command
     */
    int lastCommand;
    /**
     * count of all lines in file
     */
    int lineCount;
    /**
     * the name of the device seen before the {@link #NEW_COMMAND_SEP}.
     */
    String deviceName;
    /**
     * The invalid marker, if seen on a directly after a command-line indicates
     * it is valid, and should be discarded.
     */
    final static String INVALID_MARKER = "^";

    public CiscoCommandSplitter(File file) throws IOException {
        reader = getReader(file);
        originalFile = file;
        splitFiles = new HashMap<>();
        linesInCommand = 0;
        lastCommand = 0;
        lineCount = 0;
        split();
    }

    public Collection<File> getSplitFiles() {
        return splitFiles.values();
    }

    /**
     * List of a commands after the {@link #NEW_COMMAND_TEXT} String.
     *
     * @return List of strings or an empty list if no commands were split.
     */
    public Collection<String> getCommands() {
        return splitFiles.keySet();
    }

    /**
     * pulls the command out of a config file's command line.
     *
     * @param commandLine String containing the command line within a config
     * file.
     * @return String of the raw command, like "arp" of "Switch1#show arp", else
     * empty string on failure.
     */
    private String getCommand(String commandLine) {
        int pos = identEnd(commandLine, NEW_COMMAND_TEXT);
        return commandLine.substring(pos).trim();
    }

    /**
     * Gets a device name, if there are multiple device names the last name seen
     * will be set.
     *
     * @return The device name as seen in the file, or the empty String if there
     * are no seen commands.
     */
    public String getDeviceName() {
        return deviceName == null ? "" : deviceName;
    }

    /**
     * Gets a map of Files to their commands.
     *
     * @return
     */
    public Map<String, File> getSplitMap() {
        return splitFiles;
    }

    public int getLineCount() {
        return lineCount;
    }

    public File getOriginalFile() {
        return originalFile;
    }

    private void split() throws IOException {
        String line;
        while ((line = reader.readLine()) != null) {
            lineCount++;

            if (this.isNewCommand(line)) {
                trySplitFiles();
                lastCommandLine = line;
                lastCommand = lineCount;
                linesInCommand = 0;
            } else {
                if( line.trim().equals(INVALID_MARKER) ) {
                    linesInCommand = 0; // reset for invalid input
                } else {
                    linesInCommand++;
                }
            }

        }

        trySplitFiles();
    }

    /**
     * Attempts to create a new temp file with the lines from
     * {@link #lastCommand} until the last line read.
     *
     * @throws IOException Throws if {@link #splitFiles} already contains
     * {@link #lastCommandLine} and on other IO failures.
     */
    private void trySplitFiles() throws IOException {
        int start = lastCommand;
        int end = lastCommand + linesInCommand;
        String command = lastCommandLine;

        if (sectionValid()) {
            if (debug) {
                System.out.println(String.format("[ OK ] lines %d-%d: %s", start, end, command));
            }

            String prettyCommand = getCommand(lastCommandLine);

            if( prettyCommand.isEmpty() ) {
                System.out.println( prettyCommand );
            } else if (splitFiles.containsKey(prettyCommand)) {
                throw new IOException(String.format("Cannot process duplicate commands, \"%s\"", command));
            } else {
                File tempFile = copyLinesToTempFile(prettyCommand, start, end, getOriginalFile());
                setDeviceName(lastCommandLine);
                splitFiles.put(prettyCommand, tempFile);
            }

        } else {
            if (debug) {
                System.out.println(String.format("[ BAD ] lines %d-%d: %s", start, end, command));
            }
        }
    }

    /**
     * Expects a line that can pass the {@link #sectionValid() } check.
     *
     * @param commandLine A line containing a Cisco device name and a
     * {@link #NEW_COMMAND_SEP} character.
     */
    private void setDeviceName(String commandLine) {
        int offset = commandLine.indexOf(NEW_COMMAND_SEP);
        if (offset != -1) {
            this.deviceName = commandLine.substring(0, offset).trim();
        }
    }

    /**
     * Copies lines from the source file to a where lineNumber <= end && lineNumber
     * >= start
     *
     * @param start First line to be copied.
     * @param end Last line to be copied.
     * @param source Source file from where to copy.
     * @return The newly creates and written temp file set to delete on JVM
     * exit.
     * @throws IOException Thrown is source fails to open, tempFile fails to
     * write, or other IO failures.
     */
    private File copyLinesToTempFile(String prefix, int start, int end, File source) throws IOException {
        File f = getTempFile(prefix, CiscoCommandSplitter.deleteOnExit);
        Writer writer = getWriter(f);
        BufferedReader reader = getReader(source);
        String line;

        int skip = start - 1; // minus one to copy it before we actual count it

        while ((line = reader.readLine()) != null) {
            if (skip != 0) {
                skip--;
            } else if (start <= end) {

                if (debug) {
                    System.out.println(String.format("Writing line %d: %s", start, line));
                }

                writer.write(line);
                writer.append("\n");
                start++;
            } else {
                break;
            }
        }

        writer.close();
        reader.close();
        return f;
    }

    /**
     * Checks if the different between the last seen command line and the
     * current line is greater then the {@link #LINES_REQUIRED}.
     *
     * @return True if there appears a substantial amount of lines for a valid
     * command.
     */
    private boolean sectionValid() {
        if (lineCount == lastCommand || lastCommandLine == null) {
            return false;
        }
        return (lineCount - lastCommand) > LINES_REQUIRED;
    }

    private BufferedReader getReader(File file) throws FileNotFoundException {
        return new BufferedReader(new FileReader(file));
    }

    private FileWriter getWriter(File file) throws IOException {
        if (!file.exists()) {
            file.createNewFile();
        }
        return new FileWriter(file);
    }

    private File getOuputDirectory() {
        File file;
        if (debugOutputPath != null) {
            file = new File(debugOutputPath);
        } else {
            file = FileUtils.getTempDirectory();
        }
        return file;
    }

    private File getTempFile(String prefix, boolean deleteOnExit) throws IOException {
        String finalPrefix = prefix == null ? "GM3" : prefix;
        File f = File.createTempFile(
                finalPrefix,
                Long.toHexString(System.nanoTime()),
                getOuputDirectory()
        );
        if (deleteOnExit) {
            f.deleteOnExit();
        }
        return f;
    }

    private boolean isNewCommand(String line) {
        int sep = line.indexOf(NEW_COMMAND_SEP);
        return sep > -1 ? valid(sep, line, NEW_COMMAND_TEXT) : false;
    }

    private int identStart(final String line, String identifier) {
        return identLocation(line, identifier, false);
    }

    private int identEnd(final String line, final String identifier) {
        return identLocation(line, identifier, true);
    }

    private int identLocation(final String line, String identifier, final boolean indexAfter) {
        int pos = -1;
        while( pos == -1 ) {
            pos = line.indexOf(identifier);
            if( pos == -1 ) {
                identifier = identifier.substring(0, identifier.length()-1);
            }
        }
        return indexAfter ? pos + identifier.length() : pos;
    }

    /**
     * Will help identify short commands, "sh" instead of "show".
     * @param startIndex Index to find results after.
     * @param line Line to search.
     * @param identifier Identifier to find atleast 2 starting characters in the line from.
     * @return True if the identifier had two characters found after the startIndex, else false.
     */
    private boolean valid(final int startIndex, final String line, String identifier) {
        int pos = identStart(line, identifier);
        return pos != -1 && pos > startIndex && identifier.length() >= 2;
    }

    @Override
    public void close() throws Exception {
        if (reader != null) {
            reader.close();
        }
    }

}
