import net.sf.mpxj.*;
import net.sf.mpxj.reader.UniversalProjectReader;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Minimal MPP -> CSV converter using MPXJ.
 * Usage: java -jar app.jar <input.mpp> <output_dir>
 */
public class MppToCsv {
    private static final DateTimeFormatter ISO = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneOffset.UTC);

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java -jar app.jar <input.mpp> <output_dir>");
            System.exit(2);
        }
        Path in = Path.of(args[0]);
        Path outDir = Path.of(args[1]);
        try {
            Files.createDirectories(outDir);
            ProjectFile pf = new UniversalProjectReader().read(in.toFile());

            Path tasksCsv = outDir.resolve("tasks.csv");
            Path resourcesCsv = outDir.resolve("resources.csv");
            Path assignmentsCsv = outDir.resolve("assignments.csv");

            writeAllTasks(pf, tasksCsv);
            writeAllResources(pf, resourcesCsv);
            writeAllAssignments(pf, assignmentsCsv);

            System.out.println("OK\t" + tasksCsv + "\t" + resourcesCsv + "\t" + assignmentsCsv);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    private static void writeAllTasks(ProjectFile pf, Path csv) throws IOException {
        java.util.List<TaskField> fields = new java.util.ArrayList<>(pf.getTasks().getPopulatedFields());
        fields.sort(java.util.Comparator.comparing(FieldType::name));
        String[] header = fields.stream().map(FieldType::name).toArray(String[]::new);
        List<String[]> rows = new ArrayList<>();
        for (Task t : pf.getTasks()) {
            String[] row = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                row[i] = getValueAsText(t, fields.get(i));
            }
            rows.add(row);
        }
        writeCsv(csv, header, rows);
    }

    private static void writeAllResources(ProjectFile pf, Path csv) throws IOException {
        java.util.List<ResourceField> fields = new java.util.ArrayList<>(pf.getResources().getPopulatedFields());
        fields.sort(java.util.Comparator.comparing(FieldType::name));
        String[] header = fields.stream().map(FieldType::name).toArray(String[]::new);
        List<String[]> rows = new ArrayList<>();
        for (Resource r : pf.getResources()) {
            String[] row = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                row[i] = getValueAsText(r, fields.get(i));
            }
            rows.add(row);
        }
        writeCsv(csv, header, rows);
    }

    private static void writeAllAssignments(ProjectFile pf, Path csv) throws IOException {
        java.util.List<AssignmentField> fields = new java.util.ArrayList<>(pf.getResourceAssignments().getPopulatedFields());
        fields.sort(java.util.Comparator.comparing(FieldType::name));
        String[] header = fields.stream().map(FieldType::name).toArray(String[]::new);
        List<String[]> rows = new ArrayList<>();
        for (ResourceAssignment ra : pf.getResourceAssignments()) {
            String[] row = new String[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                row[i] = getValueAsText(ra, fields.get(i));
            }
            rows.add(row);
        }
        writeCsv(csv, header, rows);
    }

    // --- value formatting helper ---
    private static String getValueAsText(FieldContainer container, FieldType type) {
        Object value = container.get(type);
        if (value == null) return "";
        if (value instanceof Boolean) return ((Boolean) value) ? "true" : "false";
        if (value instanceof java.time.LocalDateTime) return value.toString();
        if (value instanceof java.time.LocalDate) return value.toString();
        if (value instanceof java.time.LocalTime) return value.toString();
        return String.valueOf(value);
    }


    // --- CSV helpers ---
    private static void writeCsv(Path path, String[] header, List<String[]> rows) throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8))) {
            pw.println(joinCsv(header));
            for (String[] row : rows) pw.println(joinCsv(row));
        }
    }

    private static String joinCsv(String[] cols) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cols.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(quoteCsv(cols[i]));
        }
        return sb.toString();
    }

    private static String quoteCsv(String v) {
        if (v == null) v = "";
        // Always quote and escape quotes
        String q = v.replace("\"", "\"\"");
        return '"' + q + '"';
    }

    // --- formatting helpers ---
    private static String d(Date dt) {
        return dt == null ? "" : ISO.format(dt.toInstant());
    }
    private static String n(Number n) {
        return n == null ? "" : String.valueOf(n);
    }
    private static String s(String s) {
        return s == null ? "" : s;
    }
    private static String b(Boolean b) {
        return b != null && b ? "true" : "false";
    }
}

