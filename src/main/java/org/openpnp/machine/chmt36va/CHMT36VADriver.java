package org.openpnp.machine.chmt36va;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.openpnp.gui.support.PropertySheetWizardAdapter;
import org.openpnp.machine.chmt36va.Commands.CmdDownLamp;
import org.openpnp.machine.chmt36va.Commands.CmdFilmPull;
import org.openpnp.machine.chmt36va.Commands.CmdMachineReset;
import org.openpnp.machine.chmt36va.Commands.CmdNozzle1Blow;
import org.openpnp.machine.chmt36va.Commands.CmdNozzle1Down;
import org.openpnp.machine.chmt36va.Commands.CmdNozzle1Vacuum;
import org.openpnp.machine.chmt36va.Commands.CmdNozzle2Blow;
import org.openpnp.machine.chmt36va.Commands.CmdNozzle2Down;
import org.openpnp.machine.chmt36va.Commands.CmdNozzle2Vacuum;
import org.openpnp.machine.chmt36va.Commands.CmdPin;
import org.openpnp.machine.chmt36va.Commands.CmdPump;
import org.openpnp.machine.chmt36va.Commands.CmdReqProcessInfo;
import org.openpnp.machine.chmt36va.Commands.CmdSelectBottomCamera;
import org.openpnp.machine.chmt36va.Commands.CmdSelectTopCamera;
import org.openpnp.machine.chmt36va.Commands.CmdToOrigZero;
import org.openpnp.machine.chmt36va.Commands.CmdToSetPos;
import org.openpnp.machine.chmt36va.Commands.CmdUpLamp;
import org.openpnp.machine.chmt36va.Numerics.PositionReport;
import org.openpnp.machine.chmt36va.Statuses.UnknownStatus1;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceMachine;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.machine.reference.driver.AbstractReferenceDriver;
import org.openpnp.model.Configuration;
import org.openpnp.model.LengthUnit;
import org.openpnp.model.Location;
import org.openpnp.model.Named;
import org.openpnp.spi.Nozzle;
import org.pmw.tinylog.Logger;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root
public class CHMT36VADriver extends AbstractReferenceDriver implements Named, Runnable {
    @Attribute(required = false)
    protected LengthUnit units = LengthUnit.Millimeters;

    @Attribute(required = false)
    protected int timeoutMilliseconds = 5000;

    @Attribute(required = false)
    protected int connectWaitTimeMilliseconds = 3000;
    
    @Attribute(required = false)
    protected String name = "CHMT36VADriver";

    private Thread readerThread;
    private boolean disconnectRequested;
    private boolean connected;
    private Set<Nozzle> pickedNozzles = new HashSet<>();
    private Protocol protocol;
    // TODO STOPSHIP make sure queue is getting drained
    private LinkedBlockingQueue<Packet> responseQueue = new LinkedBlockingQueue<>();
    
    double x = 0, y = 0, z = 0, rotation = 0;
    
    public synchronized void connect() throws Exception {
        // Make sure required objects exist
        ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
        
        ReferenceActuator a = (ReferenceActuator) machine.getActuatorByName("CameraUpLamp");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("CameraUpLamp");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("CameraDownLamp");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("CameraDownLamp");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("CameraSelectUp");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("CameraSelectUp");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("DragPin");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("DragPin");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("FilmPull");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("FilmPull");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("Pump");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Pump");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("Nozzle1Vacuum");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Nozzle1Vacuum");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("Nozzle2Vacuum");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Nozzle2Vacuum");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("Nozzle1Down");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Nozzle1Down");
            machine.addActuator(a);
        }
        
        a = (ReferenceActuator) machine.getActuatorByName("Nozzle2Down");
        if (a == null) {
            a = new ReferenceActuator();
            a.setName("Nozzle2Down");
            machine.addActuator(a);
        }
        
        ReferenceNozzle n = (ReferenceNozzle) machine.getDefaultHead().getNozzle("N1");
        if (n == null) {
            n = new ReferenceNozzle("N1");
            n.setName("N1");
            machine.getDefaultHead().addNozzle(n);
        }
        
        n = (ReferenceNozzle) machine.getDefaultHead().getNozzle("N2");
        if (n == null) {
            n = new ReferenceNozzle("N2");
            n.setName("N2");
            machine.getDefaultHead().addNozzle(n);
        }
        
        getCommunications().connect();

        connected = false;
        readerThread = new Thread(this);
        readerThread.setDaemon(true);
        readerThread.start();

//        // Wait a bit while the controller starts up
//        Thread.sleep(connectWaitTimeMilliseconds);
        
        File licenseFile = new File(Configuration.get().getConfigurationDirectory(), "LICENSE.smt");
        if (!licenseFile.exists()) {
            throw new Exception(String.format("Unable to load %s, please COPY your LICENSE.smt file to %s.", 
                    licenseFile.getAbsolutePath(), 
                    Configuration.get().getConfigurationDirectory()));
        }
        protocol = new Protocol(licenseFile);
        
        // Disable the machine
        setEnabled(false);
        
        // Send startup Gcode
        CmdMachineReset cmd1 = new CmdMachineReset();
        send(cmd1);
        Thread.sleep(4000);
        
        CmdSelectTopCamera cmd2 = new CmdSelectTopCamera();
        send(cmd2);
        
        connected = true;
    }

    @Override
    public void setEnabled(boolean enabled) throws Exception {
        if (enabled && !connected) {
            connect();
        }
        if (connected) {
            if (enabled) {
//                sendGcode(getCommand(null, CommandType.ENABLE_COMMAND));
            }
            else {
//                sendGcode(getCommand(null, CommandType.DISABLE_COMMAND));
            }
        }
    }

    @Override
    public void dispense(ReferencePasteDispenser dispenser,Location startLocation,Location endLocation,long dispenseTimeMilliseconds) throws Exception {
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        CmdToOrigZero cmd = new CmdToOrigZero();
        send(cmd);

        while (true) {
            Packet p = responseQueue.take();
            if (p instanceof CmdReqProcessInfo) {
                Logger.debug("Homing complete.");
                break;
            }
            if (p instanceof PositionReport) {
                PositionReport report = (PositionReport) p;
                this.x = report.deltaX / 100.;
                this.y = report.deltaY / 100.;
                ReferenceMachine machine = ((ReferenceMachine) Configuration.get().getMachine());
                machine.fireMachineHeadActivity(head);
            }
        }
        
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        return new Location(units, x, y, z, rotation).add(hm.getHeadOffsets());
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        location = location.convertToUnits(units);
        location = location.subtract(hm.getHeadOffsets());

        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        double rotation = location.getRotation();

        // Handle NaNs, which means don't move this axis for this move. We just copy the existing
        // coordinate.
        if (Double.isNaN(x)) {
            x = this.x;
        }
        if (Double.isNaN(y)) {
            y = this.y;
        }
        if (Double.isNaN(z)) {
            z = this.z;
        }
        if (Double.isNaN(rotation)) {
            rotation = this.rotation;
        }
        
        responseQueue.clear();
        if (x != this.x || y != this.y) {
            PositionReport report = new PositionReport();
            report.startX = (int) (x * 100.);
            report.startY = (int) (y * 100.);
            report.deltaX = 0;
            report.deltaY = 0;
            report.curDeviceSelId = 3;
            report.curDebugSpeed = 1;
            send(report);
            CmdToSetPos cmd = new CmdToSetPos();
            send(cmd);

            Packet p = expect(CmdReqProcessInfo.class, 2000);
            if (p == null) {
                Logger.warn("Move timed out. Ignoring it until we know more.");
            }
            
            this.x = x;
            this.y = y;
        }

        // TODO STOPSHIP
        if (hm.getName().equals("N1")) {
            if (z < 0) {
                
            }
        }
        else if (hm.getName().equals("N2")) {
            
        }
    }

    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        pickedNozzles.add(nozzle);
        if (pickedNozzles.size() > 0) {
            // TODO STOPSHIP turn on pump
        }
        // TODO STOPSHIP send pick
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        // TODO STOPSHIP send place
        
        pickedNozzles.remove(nozzle);
        if (pickedNozzles.size() < 1) {
            // TODO STOPSHIP turn off pump
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        if (actuator.getName().equals("CameraUpLamp")) {
            CmdUpLamp cmd = new CmdUpLamp();
            cmd.on = on;
            send(cmd);
        }
        else if (actuator.getName().equals("CameraDownLamp")) {
            CmdDownLamp cmd = new CmdDownLamp();
            cmd.on = on;
            send(cmd);
        }
        else if (actuator.getName().equals("DragPin")) {
            CmdPin cmd = new CmdPin();
            cmd.down = on;
            send(cmd);
        }
        else if (actuator.getName().equals("FilmPull")) {
            CmdFilmPull cmd = new CmdFilmPull();
            send(cmd);
        }
        else if (actuator.getName().equals("Pump")) {
            CmdPump cmd = new CmdPump();
            cmd.on = on;
            send(cmd);
        }
        else if (actuator.getName().equals("CameraSelectUp")) {
            if (on) {
                CmdSelectBottomCamera cmd = new CmdSelectBottomCamera();
                send(cmd);
            }
            else {
                CmdSelectTopCamera cmd = new CmdSelectTopCamera();
                send(cmd);
            }
        }
        else if (actuator.getName().equals("Nozzle1Vacuum")) {
            if (on) {
                CmdNozzle1Vacuum cmd = new CmdNozzle1Vacuum();
                send(cmd);
            }
            else {
                CmdNozzle1Blow cmd = new CmdNozzle1Blow();
                send(cmd);
            }
        }
        else if (actuator.getName().equals("Nozzle2Vacuum")) {
            if (on) {
                CmdNozzle2Vacuum cmd = new CmdNozzle2Vacuum();
                send(cmd);
            }
            else {
                CmdNozzle2Blow cmd = new CmdNozzle2Blow();
                send(cmd);
            }
        }
        // TODO STOPSHIP these not working correctly, maybe need to send a set position thing
        else if (actuator.getName().equals("Nozzle1Down")) {
            CmdNozzle1Down cmd = new CmdNozzle1Down();
            cmd.down = on;
            send(cmd);
        }
        // TODO STOPSHIP these not working correctly, maybe need to send a set position thing
        else if (actuator.getName().equals("Nozzle2Down")) {
            CmdNozzle2Down cmd = new CmdNozzle2Down();
            cmd.down = on;
            send(cmd);
        }
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        // TODO STOPSHIP actuate
    }
    
    @Override
    public String actuatorRead(ReferenceActuator actuator) throws Exception {
        // TODO STOPSHIP actuate
        return null;
    }

    public synchronized void disconnect() {
        disconnectRequested = true;
        connected = false;

        try {
            if (readerThread != null && readerThread.isAlive()) {
                readerThread.join(3000);
            }
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }

        try {
            getCommunications().disconnect();
        }
        catch (Exception e) {
            Logger.error("disconnect()", e);
        }
        disconnectRequested = false;
    }

    public synchronized void send(Packet p) throws Exception {
        byte[] b = protocol.encode(p);
        Logger.debug(">> " + p);
        for (int i = 0; i < b.length; i++) {
            getCommunications().write(b[i]);
        }
    }
    
    public Packet expect(Class<? extends Packet> c) throws InterruptedException {
        while (true) {
            Packet p = responseQueue.take();
            if (p.getClass().equals(c)) {
                return p;
            }
        }
    }

    public Packet expect(Class<? extends Packet> c, long millisecondsToWait) throws InterruptedException {
        while (true) {
            Packet p = responseQueue.poll(millisecondsToWait, TimeUnit.MILLISECONDS);
            if (p == null) {
                return null;
            }
            if (p.getClass().equals(c)) {
                return p;
            }
        }
    }

    public static int deleteUpTo(byte[] a, int offset) {
        int length = a.length - offset;
        System.arraycopy(a, offset, a, 0, length);
        for (int i = length; i < a.length; i++) {
            a[i] = 0;
        }
        return offset;
    }
    
    public void run() {
        byte[] buffer = new byte[10 * 1024];
        int offset = 0;
        while (!disconnectRequested) {
            try {
                try {
                    int b = getCommunications().read();
                    buffer[offset++] = (byte) b;
                }
                catch (TimeoutException e) {
                    continue;
                }
                
                do {
                    // Search for a footer in the buffer
                    int footerIndex = indexOf(buffer, 0, Protocol.FOOTER);
                    if (footerIndex == -1) {
                        break;
                    }
                    
                    // If a footer is found, search for it's nearest header
                    int headerIndex = lastIndexOf(buffer, footerIndex, Protocol.HEADER);
                    if (headerIndex == -1) {
                        // If there is a footer in the buffer but no header we've received an incomplete
                        // packet, so dump everything up to and including the footer by moving all the
                        // bytes past the footer down to the beginning of the buffer.
                        offset -= deleteUpTo(buffer, footerIndex + 4);
                        break;
                    }
                    
                    // Extract the packet
                    byte[] packet = new byte[footerIndex - headerIndex + 4];
                    System.arraycopy(buffer, headerIndex, packet, 0, packet.length);
                    offset -= deleteUpTo(buffer, footerIndex + 4);
                    
                    // Decode the packet
                    Packet p = protocol.decode(packet);
                    if (p == null) {
                        Logger.debug("<< Unknown Packet " + Protocol.bytesToHexString(packet));
                        continue;
                    }
                    Logger.debug("<< " + p.getClass().getSimpleName() + ": " + p);
                    
                    responseQueue.offer(p);
                } while (true);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    public static int indexOf(byte[] haystack, int offset, byte[] needle) {
        int needleOffset = 0;
        int matchStart = offset;
        for (int i = offset; i < haystack.length - offset; i++) {
            if (haystack[i] == needle[needleOffset]) {
                needleOffset++;
                if (needleOffset == needle.length) {
                    return matchStart;
                }
            }
            else {
                needleOffset = 0;
                matchStart = i + 1;
            }
        }
        return -1;
    }
    
    public static int lastIndexOf(byte[] haystack, int offset, byte[] needle) {
        int needleOffset = needle.length - 1;
        int matchStart = offset;
        for (int i = offset; i >= 0; i--) {
            if (haystack[i] == needle[needleOffset]) {
                needleOffset--;
                if (needleOffset == -1) {
                    return matchStart;
                }
                matchStart--;
            }
            else {
                needleOffset = needle.length - 1;
                matchStart = i - 1;
            }
        }
        return -1;
    }    
    
    @Override
    public PropertySheet[] getPropertySheets() {
        return new PropertySheet[] {
                new PropertySheetWizardAdapter(super.getConfigurationWizard(), "Communications")
        };
    }
    
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        firePropertyChange("name", null, getName());
    }
    
    public LengthUnit getUnits() {
        return units;
    }

    public void setUnits(LengthUnit units) {
        this.units = units;
    }

    public int getTimeoutMilliseconds() {
        return timeoutMilliseconds;
    }

    public void setTimeoutMilliseconds(int timeoutMilliseconds) {
        this.timeoutMilliseconds = timeoutMilliseconds;
    }

    public int getConnectWaitTimeMilliseconds() {
        return connectWaitTimeMilliseconds;
    }

    public void setConnectWaitTimeMilliseconds(int connectWaitTimeMilliseconds) {
        this.connectWaitTimeMilliseconds = connectWaitTimeMilliseconds;
    }
}
