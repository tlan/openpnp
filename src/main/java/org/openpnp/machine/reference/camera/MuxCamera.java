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

import org.openpnp.gui.support.Wizard;
import org.openpnp.machine.reference.ReferenceCamera;
import org.openpnp.model.Configuration;
import org.openpnp.spi.Actuator;
import org.openpnp.spi.Camera;
import org.openpnp.spi.PropertySheetHolder;
import org.simpleframework.xml.Attribute;

public class MuxCamera extends ReferenceCamera {
    @Attribute(required=false)
    private String cameraId;

    @Attribute(required=false)
    private String actuatorId;
    
    @Attribute(required=false)
    private Boolean actuatorBooleanValue;
    
    @Attribute(required=false)
    private long actuatorDelayMillis = 500;
    
    private Camera camera = null;
    private Actuator actuator = null;

    /**
     * TODO STOPSHIP 
     * 
     * Actually, this whole thing is wrong. Each camera needs it's own transforms, calibration,
     * scripts, etc. Right now using capture on the main camera is going to run through it's
     * transformations and stuff, and we don't want that. So this might need to maybe be
     * an extension of OpenPnpCaptureCamera so that it can do lower level control and
     * use internal capture.  
     * 
     * All MuxCameras attached to the same Camera need to communicate through
     * a Mux object so that we can keep broadcasting images to the currently selected camera.
     * Basically, so they can all know which of them is selected. Oh, and also so we don't
     * fire and sleep the actuator for every capture.
     */
    
    private synchronized void initialize() {
        if (camera != null) {
            return;
        }
        camera = Configuration.get().getMachine().getCamera(cameraId);
        actuator = Configuration.get().getMachine().getActuator(actuatorId);
        System.out.println(camera.getName());
        System.out.println(actuator.getName());
    }
    
    @Override
    protected BufferedImage internalCapture() {
        initialize();
        try {
            actuator.actuate(actuatorBooleanValue);
            Thread.sleep(actuatorDelayMillis);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // TODO STOPSHIP this is gonna cause scripts to run more than once and other bad stuff
        // so we need to actually override capture() instead.
        BufferedImage image = camera.capture();
        broadcastCapture(image);
        return image;
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
        return null;
    }
}
