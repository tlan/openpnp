import java.io.File;

import org.apache.commons.io.FileUtils;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.model.Board;
import org.openpnp.model.Board.Side;
import org.openpnp.model.BoardLocation;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Placement;
import org.openpnp.spi.Machine;

import com.google.common.io.Files;

public class LargeJobTest {
//    @Test
    public void testLargeJob() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        // Copy the required configuration files over to the new configuration
        // directory.
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/LargeJobTest/machine.xml"),
                new File(workingDirectory, "machine.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/LargeJobTest/packages.xml"),
                new File(workingDirectory, "packages.xml"));
        FileUtils.copyURLToFile(ClassLoader.getSystemResource("config/LargeJobTest/parts.xml"),
                new File(workingDirectory, "parts.xml"));

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        Machine machine = Configuration.get().getMachine();

        Job job = createSimpleJob();

        ReferencePnpJobProcessor jobProcessor = (ReferencePnpJobProcessor) machine.getPnpJobProcessor();
        
        jobProcessor.setAutoSaveJob(false);
        jobProcessor.setAutoSaveConfiguration(false);
        machine.setEnabled(true);
        jobProcessor.initialize(job);
        while (jobProcessor.next());
    }

    private Job createSimpleJob() {
        Job job = new Job();

        Board board = new Board();
        board.setName("test");

        for (int i = 0; i < 2000; i++) {
            board.addPlacement(createPlacement("R1" + i, "R0603", Math.random() * 100, Math.random() * 100, 0, Math.random() * 90, Side.Top));
        }
        for (int i = 0; i < 1000; i++) {
            board.addPlacement(createPlacement("R2" + i, "R0805", Math.random() * 100, Math.random() * 100, 0, Math.random() * 90, Side.Top));
        }
        for (int i = 0; i < 1000; i++) {
            board.addPlacement(createPlacement("IC" + i, "TQFP-48", Math.random() * 100, Math.random() * 100, 0, Math.random() * 90, Side.Top));
        }

        BoardLocation boardLocation = new BoardLocation(board);
        boardLocation.setLocation(new Location(LengthUnit.Millimeters, 0, 0, 0, 0));
        boardLocation.setSide(Side.Top);

        job.addBoardLocation(boardLocation);

        return job;
    }

    public static Placement createPlacement(String id, String partId, double x, double y, double z,
            double rotation, Side side) {
        Placement placement = new Placement(id);
        placement.setPart(Configuration.get().getPart(partId));
        placement.setLocation(new Location(LengthUnit.Millimeters, x, y, z, rotation));
        placement.setSide(side);
        return placement;
    }
}
