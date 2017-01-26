/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project;

import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport.DryRunLibraryImportStatus;
import org.rf.ide.core.dryrun.RobotDryRunLibraryImport.DryRunLibraryType;
import org.rf.ide.core.project.RobotProjectConfig;
import org.rf.ide.core.project.RobotProjectConfig.ReferencedLibrary;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.RedWorkspace;
import org.robotframework.ide.eclipse.main.plugin.launch.RobotLaunchConfigurationDelegate;
import org.robotframework.ide.eclipse.main.plugin.model.RobotProject;
import org.robotframework.ide.eclipse.main.plugin.model.RobotSuiteFile;
import org.robotframework.red.swt.SwtThread;

import com.google.common.base.Optional;
import com.google.common.io.Files;

/**
 * @author mmarzec
 */
public class LibrariesAutoDiscoverer extends AbstractAutoDiscoverer {

    private IEventBroker eventBroker;

    private boolean isSummaryWindowEnabled;

    private Optional<String> libraryNameToDiscover = Optional.absent();

    public LibrariesAutoDiscoverer(final RobotProject robotProject, final Collection<IResource> suiteFiles,
            final IEventBroker eventBroker) {
        this(robotProject, suiteFiles, eventBroker, null);
    }

    public LibrariesAutoDiscoverer(final RobotProject robotProject, final Collection<IResource> suiteFiles,
            final IEventBroker eventBroker, final String libraryNameToDiscover) {
        this(robotProject, suiteFiles, true);
        this.eventBroker = eventBroker;
        this.libraryNameToDiscover = libraryNameToDiscover != null && !libraryNameToDiscover.isEmpty()
                ? Optional.of(libraryNameToDiscover) : Optional.<String> absent();
    }

    public LibrariesAutoDiscoverer(final RobotProject robotProject, final Collection<IResource> suiteFiles,
            final boolean isSummaryWindowEnabled) {
        super(robotProject, suiteFiles);
        this.isSummaryWindowEnabled = isSummaryWindowEnabled;
    }

    @Override
    public void start(final Shell parent) {
        if (startDryRun()) {
            final WorkspaceJob wsJob = new WorkspaceJob("Discovering libraries") {

                @Override
                public IStatus runInWorkspace(final IProgressMonitor monitor) throws CoreException {
                    try {
                        startDiscovering(monitor, new DryRunTargetsCollector());
                        startAddingLibrariesToProjectConfiguration(monitor);
                        if (isSummaryWindowEnabled) {
                            SwtThread.syncExec(new Runnable() {

                                @Override
                                public void run() {
                                    new LibrariesAutoDiscovererWindow(parent, dryRunOutputParser.getImportedLibraries())
                                            .open();
                                }
                            });
                        }
                    } catch (final InvocationTargetException e) {
                        MessageDialog.openError(parent, "Discovering libraries",
                                "Problems occurred during discovering libraries: " + e.getCause().getMessage());
                    } finally {
                        stopDryRun();
                    }

                    return Status.OK_STATUS;
                }

                @Override
                protected void canceling() {
                    isSummaryWindowEnabled = false;
                    dryRunHandler.destroyDryRunProcess();
                    this.cancel();
                }
            };
            wsJob.setUser(true);
            wsJob.schedule();
        }
    }

    private void startAddingLibrariesToProjectConfiguration(final IProgressMonitor monitor) {
        if (libraryNameToDiscover.isPresent()) {
            dryRunOutputParser.filterImportedLibrariesByName(libraryNameToDiscover.get());
            // after this filtering RobotDryRunOutputParser will return only one imported library or
            // nothing
        }
        final List<RobotDryRunLibraryImport> dryRunLibrariesImports = filterUnknownDryRunLibraryImports(
                dryRunOutputParser.getImportedLibraries());
        if (!dryRunLibrariesImports.isEmpty()) {
            RobotProjectConfig config = robotProject.getOpenedProjectConfig();
            final boolean inEditor = config != null;
            if (config == null) {
                config = new RedEclipseProjectConfigReader().readConfiguration(robotProject.getConfigurationFile());
            }
            final List<RobotDryRunLibraryImport> dryRunLibrariesToAdd = filterExistingReferencedLibraries(
                    dryRunLibrariesImports, config.getLibraries());

            final SubMonitor subMonitor = SubMonitor.convert(monitor);
            subMonitor.setWorkRemaining(dryRunLibrariesToAdd.size() + 1);
            final RedEclipseProjectConfigUpdater configUpdater = new RedEclipseProjectConfigUpdater(config,
                    robotProject);
            for (final RobotDryRunLibraryImport libraryImport : dryRunLibrariesToAdd) {
                subMonitor.subTask("Adding discovered library to project configuration: " + libraryImport.getName());
                configUpdater.addLibrary(libraryImport);
                subMonitor.worked(1);
            }

            subMonitor.subTask("Updating project configuration...");
            final List<ReferencedLibrary> addedLibs = configUpdater.getAddedLibraries();
            if (!addedLibs.isEmpty()) {
                sendProjectConfigChangedEvent(addedLibs);
                if (!inEditor) {
                    new RedEclipseProjectConfigWriter().writeConfiguration(config, robotProject);
                }
            }
            subMonitor.worked(1);
            subMonitor.done();
        }
    }

    private void sendProjectConfigChangedEvent(final List<ReferencedLibrary> addedLibs) {
        final RedProjectConfigEventData<List<ReferencedLibrary>> eventData = new RedProjectConfigEventData<>(
                robotProject.getConfigurationFile(), addedLibs);
        if (eventBroker == null) {
            eventBroker = PlatformUI.getWorkbench().getService(IEventBroker.class);
        }
        if (eventBroker != null) {
            eventBroker.send(RobotProjectConfigEvents.ROBOT_CONFIG_LIBRARIES_STRUCTURE_CHANGED, eventData);
        }
    }

    private List<RobotDryRunLibraryImport> filterUnknownDryRunLibraryImports(
            final List<RobotDryRunLibraryImport> importedLibraries) {
        final List<RobotDryRunLibraryImport> filteredLibrariesImports = newArrayList();
        for (final RobotDryRunLibraryImport dryRunLibraryImport : importedLibraries) {
            if (dryRunLibraryImport.getType() != DryRunLibraryType.UNKNOWN) {
                filteredLibrariesImports.add(dryRunLibraryImport);
            }
        }
        return filteredLibrariesImports;
    }

    private List<RobotDryRunLibraryImport> filterExistingReferencedLibraries(
            final List<RobotDryRunLibraryImport> importedLibraries, final List<ReferencedLibrary> referencedLibraries) {
        final List<RobotDryRunLibraryImport> filteredLibrariesImports = newArrayList();
        final List<String> currentLibrariesNames = newArrayList();
        for (final ReferencedLibrary referencedLibrary : referencedLibraries) {
            currentLibrariesNames.add(referencedLibrary.getName());
        }
        for (final RobotDryRunLibraryImport dryRunLibraryImport : importedLibraries) {
            if (!currentLibrariesNames.contains(dryRunLibraryImport.getName())) {
                filteredLibrariesImports.add(dryRunLibraryImport);
            } else {
                dryRunLibraryImport.setStatusAndAdditionalInfo(DryRunLibraryImportStatus.ALREADY_EXISTING,
                        "Library '" + dryRunLibraryImport.getName() + "' already existing in project configuration.");
            }
        }
        return filteredLibrariesImports;
    }

    private class DryRunTargetsCollector implements IDryRunTargetsCollector {

        private final List<String> suiteNames = newArrayList();

        private final List<String> additionalProjectsLocations = newArrayList();

        @Override
        public void collectSuiteNamesAndAdditionalProjectsLocations() {
            final List<String> resourcesPaths = newArrayList();
            for (final IResource resource : suiteFiles) {
                RobotSuiteFile suiteFile = null;
                if (resource.getType() == IResource.FILE) {
                    suiteFile = RedPlugin.getModelManager().createSuiteFile((IFile) resource);
                }
                if (suiteFile != null && suiteFile.isResourceFile()) {
                    final IPath resourceFilePath = RedWorkspace.Paths
                            .toWorkspaceRelativeIfPossible(resource.getProjectRelativePath());
                    resourcesPaths.add(resourceFilePath.toString());
                } else {
                    if (resource.isLinked()) {
                        collectLinkedSuiteNamesAndProjectsLocations(resource);
                    } else {
                        suiteNames.add(RobotLaunchConfigurationDelegate.createSuiteName(resource));
                    }
                }
            }
            if (!resourcesPaths.isEmpty()) {
                final File tempSuiteFileWithResources = dryRunHandler.createTempSuiteFile(resourcesPaths,
                        new ArrayList<String>());
                if (tempSuiteFileWithResources != null) {
                    suiteNames.add(Files.getNameWithoutExtension(tempSuiteFileWithResources.getPath()));
                    additionalProjectsLocations.add(tempSuiteFileWithResources.getParent());
                }
            }
        }

        private void collectLinkedSuiteNamesAndProjectsLocations(final IResource resource) {
            final IPath linkedFileLocation = resource.getLocation();
            if (linkedFileLocation != null) {
                final File linkedFile = linkedFileLocation.toFile();
                if (linkedFile.exists()) {
                    suiteNames.add(Files.getNameWithoutExtension(linkedFile.getName()));
                    final String linkedFileParentPath = linkedFile.getParent();
                    if (!additionalProjectsLocations.contains(linkedFileParentPath)) {
                        additionalProjectsLocations.add(linkedFileParentPath);
                    }
                }
            }
        }

        @Override
        public List<String> getSuiteNames() {
            return suiteNames;
        }

        @Override
        public List<String> getAdditionalProjectsLocations() {
            return additionalProjectsLocations;
        }

    }

}
