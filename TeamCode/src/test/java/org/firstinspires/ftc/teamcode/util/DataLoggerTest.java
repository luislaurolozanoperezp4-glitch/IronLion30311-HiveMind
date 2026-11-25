// Sources: new for IronLion30311-HiveMind
package org.firstinspires.ftc.teamcode.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

public class DataLoggerTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void writesHeaderAndRows() throws Exception {
        DataLogger log = new DataLogger(folder.getRoot());
        assertTrue(log.open("test_logs", "Tele Op!", "a,b,c"));
        log.logRow(1, 2.5, "x,y");
        log.close();
        File[] files = new File(folder.getRoot(), "test_logs").listFiles();
        assertEquals(1, files.length);
        assertTrue(files[0].getName().startsWith("test_logs_TeleOp_"));
        List<String> lines = Files.readAllLines(files[0].toPath(), StandardCharsets.UTF_8);
        assertEquals("a,b,c", lines.get(0));
        assertEquals("1,2.5000,\"x,y\"", lines.get(1));
    }

    @Test
    public void failsSafelyWhenTheFolderCannotBeCreated() throws Exception {
        File notADirectory = folder.newFile("not_a_dir");
        DataLogger log = new DataLogger(notADirectory);
        assertFalse(log.open("logs", "x", "h"));
        log.logRow(1, 2);
        log.flush();
        log.close();
        assertFalse(log.isOpen());
    }
}
