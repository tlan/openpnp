package org.openpnp.machine.reference.driver.test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Action;
import javax.swing.Icon;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceActuator;
import org.openpnp.machine.reference.ReferenceDriver;
import org.openpnp.machine.reference.ReferenceHead;
import org.openpnp.machine.reference.ReferenceHeadMountable;
import org.openpnp.machine.reference.ReferenceNozzle;
import org.openpnp.machine.reference.ReferencePasteDispenser;
import org.openpnp.model.Location;
import org.openpnp.spi.HeadMountable;
import org.openpnp.spi.PropertySheetHolder;

public class ExtendedTestDriver implements ReferenceDriver {
    boolean enabled = false;
    Map<String, Axis> axes = new HashMap<>();
    
    public void addAxis(String name, Axis.Type type) {
        if (axes.containsKey(name)) {
            throw new Error("Axis " + name + " already exists.");
        }
        axes.put(name, new Axis(name, type));
    }
    
    public void mapAxis(HeadMountable hm, String axisName) {
        
    }
    
    @Override
    public void setEnabled(boolean enabled) throws Exception {
        this.enabled = enabled;
    }

    @Override
    public Location getLocation(ReferenceHeadMountable hm) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void home(ReferenceHead head) throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }

    @Override
    public void moveTo(ReferenceHeadMountable hm, Location location, double speed)
            throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }
    
    @Override
    public void pick(ReferenceNozzle nozzle) throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }

    @Override
    public void place(ReferenceNozzle nozzle) throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }

    @Override
    public void actuate(ReferenceActuator actuator, boolean on) throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }

    @Override
    public void actuate(ReferenceActuator actuator, double value) throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }

    @Override
    public void dispense(ReferencePasteDispenser dispenser, Location startLocation,
            Location endLocation, long dispenseTimeMilliseconds) throws Exception {
        // TODO Auto-generated method stub
        checkEnabled();
    }
    
    @Override
    public Wizard getConfigurationWizard() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getPropertySheetHolderTitle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public PropertySheet[] getPropertySheets() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Action[] getPropertySheetHolderActions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Icon getPropertySheetHolderIcon() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() throws IOException {
    }

    private void checkEnabled() throws Exception {
        if (!enabled) {
            throw new Exception("Driver is not yet enabled!");
        }
    }
    
    public static class Axis {
        public enum Type {
            X,
            Y,
            Z,
            C
        }
        
        public final String name;
        public final Type type;
        public double coordinate;
        
        public Axis(String name, Type type) {
            this.name = name;
            this.type = type;
        }
    }
}
