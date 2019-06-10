/*
 * Copyright (C) 2011 Jason von Nieda <jason@vonnieda.org>
 * 
 * This file is part of OpenPnP.
 * 
 * OpenPnP is free software: you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * OpenPnP is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenPnP. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * For more information about OpenPnP visit http://openpnp.org
 */

package org.openpnp.machine.reference.camera;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.machine.reference.camera.wizards.SwitcherCameraConfigurationWizard;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

public class SwitcherCamera extends ReferenceCamera {
    @Attribute(required=false)
    private int switcher = 0;
    
    @Attribute(required=false)
    private String cameraId;

    @Attribute(required=false)
    private String actuatorId;
    
    @Attribute(required=false)
    private double actuatorDoubleValue;
    
    @Attribute(required=false)
    private long actuatorDelayMillis = 500;
    
    private Camera camera = null;
    private Actuator actuator = null;
    
    private static Map<Integer, Camera> switchers = new HashMap<>();

    @Override
    public synchronized BufferedImage internalCapture() {
        if (!ensureOpen()) {
            return null;
        }
        synchronized (switchers) {
            if (switchers.get(switcher) != this) {
                try {
                    if (Configuration.get().getMachine().isEnabled()) {
                        actuator.actuate(actuatorDoubleValue);
                        Thread.sleep(actuatorDelayMillis);
                        switchers.put(switcher, this);
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }                
            }
        }
        return ((ReferenceCamera) camera).internalCapture();
    }

    private synchronized boolean ensureOpen() {
        if (camera == null) {
            camera = Configuration.get().getMachine().getCamera(cameraId);
        }
        if (actuator == null) {
            actuator = Configuration.get().getMachine().getActuator(actuatorId);
        }
        return camera != null && actuator != null;
    }
    
    @Override
    public String getPropertySheetHolderTitle() {
        return getClass().getSimpleName() + " " + getName();
    }

    @Override
    public PropertySheetHolder[] getChildPropertySheetHolders() {
        return null;
    }

    @Override
    public Wizard getConfigurationWizard() {
        return new SwitcherCameraConfigurationWizard(this);
    }

    public String getCameraId() {
        return cameraId;
    }

    public void setCameraId(String cameraId) {
        this.cameraId = cameraId;
        camera = Configuration.get().getMachine().getCamera(cameraId);
        firePropertyChange("cameraId", null, cameraId);
    }

    public String getActuatorId() {
        return actuatorId;
    }

    public void setActuatorId(String actuatorId) {
        this.actuatorId = actuatorId;
        actuator = Configuration.get().getMachine().getActuator(actuatorId);
        firePropertyChange("actuatorId", null, actuatorId);
    }
    
    public double getActuatorDoubleValue() {
        return actuatorDoubleValue;
    }

    public void setActuatorDoubleValue(double actuatorDoubleValue) {
        this.actuatorDoubleValue = actuatorDoubleValue;
        firePropertyChange("actuatorDoubleValue", null, actuatorDoubleValue);
    }

    public long getActuatorDelayMillis() {
        return actuatorDelayMillis;
    }

    public void setActuatorDelayMillis(long actuatorDelayMillis) {
        this.actuatorDelayMillis = actuatorDelayMillis;
        firePropertyChange("actuatorDelayMillis", null, actuatorDelayMillis);
    }

    public int getSwitcher() {
        return switcher;
    }

    public void setSwitcher(int switcher) {
        this.switcher = switcher;
        firePropertyChange("switcher", null, switcher);
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        setCameraId(camera.getId());
    }

    public Actuator getActuator() {
        return actuator;
    }

    public void setActuator(Actuator actuator) {
        setActuatorId(actuator.getId());
    }
}
