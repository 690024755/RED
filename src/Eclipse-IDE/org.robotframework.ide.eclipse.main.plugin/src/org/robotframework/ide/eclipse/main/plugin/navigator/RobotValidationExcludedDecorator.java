/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.navigator;

import java.util.Optional;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ILightweightLabelDecorator;
import org.rf.ide.core.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.red.graphics.ColorsManager;

public class RobotValidationExcludedDecorator implements ILightweightLabelDecorator {

    public static final String ID = "org.robotframework.red.decorator.resource.validationExcluded";

    @Override
    public void addListener(final ILabelProviderListener listener) {
        // nothing to do here
    }

    @Override
    public void dispose() {
        // nothing to do here
    }

    @Override
    public boolean isLabelProperty(final Object element, final String property) {
        return false;
    }

    @Override
    public void removeListener(final ILabelProviderListener listener) {
        // nothing to do here
    }

    @Override
    public void decorate(final Object element, final IDecoration decoration) {
        final IResource resource = RedPlugin.getAdapter(element, IResource.class);
        if (resource != null && (resource.getType() == IResource.FILE || resource.getType() == IResource.FOLDER
                || resource.getType() == IResource.PROJECT)) {
            final RobotProject robotProject = RedPlugin.getModelManager()
                    .getModel()
                    .createRobotProject(resource.getProject());
            final Optional<RobotProjectConfig> openedConfig = robotProject.getOpenedProjectConfig();
            final RobotProjectConfig config = openedConfig.orElseGet(robotProject::getRobotProjectConfig);

            if (config.isExcludedPath(resource.getProjectRelativePath().toPortableString())) {
                decoration.addSuffix(" [excluded]");
                decoration.setForegroundColor(ColorsManager.getColor(200, 200, 200));
            }
        }
    }

}
