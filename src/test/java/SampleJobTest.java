import java.io.File;

import org.junit.Test;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferencePnpJobProcessor;
import org.openpnp.machine.reference.driver.NullDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.Job;
import org.openpnp.spi.Camera;

import com.google.common.io.Files;

public class SampleJobTest {


    /**
     * Loads the pnp-test job that is included in the samples and attempts to run it within a test
     * harness. The job is expected to complete successfully without throwing any exceptions.
     * 
     * This test is intended to exercise the basic job processing functions, image processing,
     * vision, feeder handling and fiducial handling. It's intended to act as a smoke test for large
     * changes.
     */
    @Test
    public void testSampleJob() throws Exception {
        File workingDirectory = Files.createTempDir();
        workingDirectory = new File(workingDirectory, ".openpnp");
        System.out.println("Configuration directory: " + workingDirectory);

        Configuration.initialize(workingDirectory);
        Configuration.get().load();

        ReferenceMachine machine = (ReferenceMachine) Configuration.get().getMachine();

        NullDriver driver = (NullDriver) machine.getDriver();
        driver.setFeedRateMmPerMinute(0);

        Camera camera = machine.getDefaultHead().getDefaultCamera();
        camera.setSettleTimeMs(0);

        ReferencePnpJobProcessor jobProcessor = (ReferencePnpJobProcessor) machine.getPnpJobProcessor();
        jobProcessor.addTextStatusListener((text) -> {
            System.out.println(text);
        });

        File jobFile = new File("samples");
        jobFile = new File(jobFile, "pnp-test");
        jobFile = new File(jobFile, "pnp-test.job.xml");
        Job job = Configuration.get().loadJob(jobFile);

        machine.setEnabled(true);
        jobProcessor.initialize(job);
        try {
            while (jobProcessor.next());
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
