package com.maonaroda.mao_na_roda;

import android.content.Context;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Private app storage for reports; records are local and are not uploaded or verified. */
final class BarrierReportStore {
    private final File file;

    BarrierReportStore(File filesDirectory) { file = new File(filesDirectory, "barrier-reports.txt"); }

    static BarrierReportStore from(Context context) { return new BarrierReportStore(context.getFilesDir()); }

    synchronized List<BarrierReport> all() throws IOException {
        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        if (backup.exists()) {
            if (!file.exists() && !backup.renameTo(file)) throw new IOException("Não foi possível recuperar as anotações.");
            if (file.exists()) backup.delete();
        }
        if (!file.exists()) return Collections.emptyList();
        List<BarrierReport> reports = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                try { reports.add(decode(line)); } catch (RuntimeException | IOException ignored) { }
            }
        }
        return reports;
    }

    synchronized BarrierReport find(String id) throws IOException {
        for (BarrierReport report : all()) if (report.id.equals(id)) return report;
        return null;
    }

    synchronized BarrierReport add(String category, String description, String photoUri) throws IOException {
        List<BarrierReport> reports = new ArrayList<>(all());
        BarrierReport report = new BarrierReport(UUID.randomUUID().toString(), category, description,
                System.currentTimeMillis(), photoUri == null ? "" : photoUri);
        reports.add(0, report);
        File temporary = new File(file.getParentFile(), file.getName() + ".tmp");
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(temporary), StandardCharsets.UTF_8))) {
            for (BarrierReport item : reports) { writer.write(encode(item)); writer.newLine(); }
        }
        // Windows não substitui um destino existente com File.renameTo.
        // Preservar a versão anterior permite recuperar interrupções entre as renomeações.
        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        if (file.exists() && !file.renameTo(backup)) throw new IOException("Não foi possível preservar as anotações.");
        if (!temporary.renameTo(file)) {
            backup.renameTo(file);
            throw new IOException("Não foi possível salvar o relato.");
        }
        backup.delete();
        return report;
    }

    private static String encode(BarrierReport report) {
        return field(report.id) + "\t" + field(report.category) + "\t" + field(report.description)
                + "\t" + report.createdAt + "\t" + field(report.photoUri);
    }

    private static BarrierReport decode(String line) throws IOException {
        String[] parts = line.split("\\t", -1);
        if (parts.length != 5) throw new IOException("Registro inválido");
        return new BarrierReport(unfield(parts[0]), unfield(parts[1]), unfield(parts[2]),
                Long.parseLong(parts[3]), unfield(parts[4]));
    }

    private static String field(String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        StringBuilder encoded = new StringBuilder(bytes.length * 2);
        for (byte valueByte : bytes) encoded.append(String.format(java.util.Locale.ROOT, "%02x", valueByte & 0xff));
        return encoded.toString();
    }
    private static String unfield(String value) {
        if (value.length() % 2 != 0) throw new IllegalArgumentException("Registro inválido");
        byte[] bytes = new byte[value.length() / 2];
        for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) Integer.parseInt(value.substring(i * 2, i * 2 + 2), 16);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
