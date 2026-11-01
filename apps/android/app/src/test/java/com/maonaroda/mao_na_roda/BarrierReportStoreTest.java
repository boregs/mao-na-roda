package com.maonaroda.mao_na_roda;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import org.junit.Test;

public class BarrierReportStoreTest {
    @Test public void reportIsAvailableAfterStoreIsRecreated() throws Exception {
        File directory = Files.createTempDirectory("mao-na-roda-reports").toFile();
        try {
            BarrierReportStore first = new BarrierReportStore(directory);
            BarrierReport saved = first.add("Rampa inadequada", "Inclinação muito forte\nsem corrimão", "content://foto/1");
            BarrierReportStore reopened = new BarrierReportStore(directory);
            BarrierReport restored = reopened.find(saved.id);
            assertNotNull(restored);
            assertEquals(saved.category, restored.category);
            assertEquals(saved.description, restored.description);
            assertEquals(saved.photoUri, restored.photoUri);
            assertEquals(saved.createdAt, restored.createdAt);
        } finally {
            File data = new File(directory, "barrier-reports.txt");
            if (data.exists()) data.delete();
            directory.delete();
        }
    }

    @Test public void newestReportAppearsFirstInHistory() throws Exception {
        File directory = Files.createTempDirectory("mao-na-roda-reports").toFile();
        try {
            BarrierReportStore store = new BarrierReportStore(directory);
            store.add("Ausência de rampa", "Primeiro", "");
            BarrierReport second = store.add("Elevador quebrado", "Segundo", "");
            List<BarrierReport> reports = store.all();
            assertEquals(second.id, reports.get(0).id);
            assertEquals(2, reports.size());
        } finally {
            File data = new File(directory, "barrier-reports.txt");
            if (data.exists()) data.delete();
            directory.delete();
        }
    }
}
