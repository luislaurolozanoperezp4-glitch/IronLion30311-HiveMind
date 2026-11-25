// Sources: IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/LogFiles.java @ a7b8f4c (tagPart)
//          IronLion30311BIOBUZZInicioDeTemporada / branch feature/turrubiates/vision-align / TeamCode/.../subsystems/ShooterLogger.java @ a7b8f4c (timestamped CSV under /sdcard/FIRST, buffered writer, periodic flush)
//          — adapted for BIOBUZZ (one CSV writer shared by every logger)
package org.firstinspires.ftc.teamcode.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Writes CSV log files on the Control Hub, under {@code /sdcard/FIRST/<folder>/}.
 * <p>
 * Use it like this:
 * <pre>
 *   DataLogger log = new DataLogger();
 *   log.open("launcher_logs", "TeleOpRed", "time_sec,rpm,volts");
 *   log.logRow(time, rpm, volts);   // once per loop
 *   log.close();                    // in stop()
 * </pre>
 * It never crashes the OpMode: if the file cannot be opened or written (for example in a unit
 * test on the computer, where there is no {@code /sdcard}), {@link #isOpen()} is false, every
 * call does nothing, and {@link #getLastError()} says why.
 */
public class DataLogger {

    /** Root folder of every log on the Control Hub. */
    public static final String DEFAULT_ROOT = "/sdcard/FIRST";

    private final File root;
    private BufferedWriter writer;
    private File file;
    private String lastError = "";
    private int rowCount = 0;

    /** Logs under {@link #DEFAULT_ROOT}. */
    public DataLogger() {
        this(new File(DEFAULT_ROOT));
    }

    /** @param root folder that holds the log folders (tests pass a temporary folder) */
    public DataLogger(File root) {
        this.root = root;
    }

    /**
     * Creates a new timestamped CSV file and writes its header line.
     *
     * @param folder folder under the root, for example {@code "launcher_logs"}
     * @param name   OpMode name, added to the file name (letters, digits and - are kept)
     * @param header comma-separated column names
     * @return true if the file is open
     */
    public boolean open(String folder, String name, String header) {
        close();
        try {
            File dir = new File(root, folder);
            if (!dir.exists() && !dir.mkdirs()) {
                lastError = "cannot create " + dir.getPath();
                return false;
            }
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
            file = new File(dir, folder + "_" + tagPart(name) + timestamp + ".csv");
            writer = new BufferedWriter(new FileWriter(file));
            writer.write(header);
            writer.newLine();
            rowCount = 0;
            return true;
        } catch (IOException | SecurityException e) {
            lastError = String.valueOf(e.getMessage());
            writer = null;
            return false;
        }
    }

    /**
     * Writes one row. Numbers are written as they are; text containing commas is quoted.
     *
     * @param values one value per column
     */
    public void logRow(Object... values) {
        if (writer == null) {
            return;
        }
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                line.append(',');
            }
            line.append(formatValue(values[i]));
        }
        try {
            writer.write(line.toString());
            writer.newLine();
            rowCount++;
        } catch (IOException e) {
            fail(e);
        }
    }

    /** Pushes buffered rows to the file (call about once per second, not every loop). */
    public void flush() {
        if (writer == null) {
            return;
        }
        try {
            writer.flush();
        } catch (IOException e) {
            fail(e);
        }
    }

    /** Closes the file. Safe to call more than once. */
    public void close() {
        if (writer == null) {
            return;
        }
        try {
            writer.close();
        } catch (IOException e) {
            lastError = String.valueOf(e.getMessage());
        }
        writer = null;
    }

    /** @return true while a file is open for writing */
    public boolean isOpen() {
        return writer != null;
    }

    /** @return rows written to the current file */
    public int getRowCount() {
        return rowCount;
    }

    /** @return the current file name, or "None" */
    public String getFileName() {
        return file != null ? file.getName() : "None";
    }

    /** @return the last error message, or an empty string */
    public String getLastError() {
        return lastError;
    }

    /**
     * File-name part for an OpMode name: letters, digits and - only, followed by _.
     *
     * @param tag OpMode name, or null
     * @return the cleaned name plus "_", or an empty string
     */
    public static String tagPart(String tag) {
        if (tag == null) {
            return "";
        }
        String clean = tag.replaceAll("[^A-Za-z0-9-]", "");
        return clean.isEmpty() ? "" : clean + "_";
    }

    private static String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof Double || value instanceof Float) {
            double number = ((Number) value).doubleValue();
            return Double.isNaN(number) ? "" : String.format(Locale.US, "%.4f", number);
        }
        String text = value.toString();
        if (text.indexOf(',') >= 0 || text.indexOf('"') >= 0) {
            return '"' + text.replace("\"", "\"\"") + '"';
        }
        return text;
    }

    private void fail(IOException e) {
        lastError = String.valueOf(e.getMessage());
        close();
    }
}
