/*
 * Copyright 2015 Nokia Solutions and Networks
 * Licensed under the Apache License, Version 2.0,
 * see license.txt file for details.
 */
package org.robotframework.ide.eclipse.main.plugin.project.build.fix;

import javax.inject.Inject;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.StatusManager;
import org.robotframework.ide.eclipse.main.plugin.RedPlugin;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfig;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigReader;
import org.robotframework.ide.eclipse.main.plugin.project.RobotProjectConfigWriter;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditor;
import org.robotframework.ide.eclipse.main.plugin.project.editor.RedProjectEditorInput;

/**
 * @author Michal Anglart
 *
 */
public abstract class RedConfigFileCompletionProposal implements ICompletionProposal {

    @Inject
    protected IEventBroker eventBroker;

    private final IMarker marker;

    private final IFile externalFile;

    private final String shortDescription;

    private final String detailedDescription;

    public RedConfigFileCompletionProposal(final IMarker marker, final IFile externalFile,
            final String shortDescritption, final String detailedDescription) {
        this.marker = marker;
        this.externalFile = externalFile;
        this.shortDescription = shortDescritption;
        this.detailedDescription = detailedDescription;

        final IEclipseContext context = PlatformUI.getWorkbench().getService(IEclipseContext.class).getActiveLeaf();
        ContextInjectionFactory.inject(this, context);
    }

    @Override
    public void apply(final IDocument currentFileDocument) {
        try {
            final IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
            final FileEditorInput input = new FileEditorInput(externalFile);
            final IEditorPart editor = page.findEditor(input);

            final RedProjectEditorInput redProjectInput = findInput(editor);
            final RobotProjectConfig config = provideConfig(redProjectInput);

            if (apply(externalFile, config)) {
                marker.delete();

                if (redProjectInput == null) { // there is no editor opened
                    final RobotProjectConfigWriter writer = new RobotProjectConfigWriter();
                    writer.writeConfiguration(config, externalFile.getProject());
                }

                final IEditorRegistry editorRegistry = PlatformUI.getWorkbench().getEditorRegistry();
                final IEditorDescriptor desc = editorRegistry.findEditor(RedProjectEditor.ID);
                page.openEditor(input, desc.getId());
                fireEvents();
            }
        } catch (final ProposalApplyingException | CoreException e) {
            StatusManager.getManager().handle(new Status(IStatus.ERROR, RedPlugin.PLUGIN_ID, e.getMessage(), e));
        }
    }

    protected void fireEvents() {
        // nothing to do here
    }

    private RedProjectEditorInput findInput(final IEditorPart editor) {
        return editor instanceof RedProjectEditor ? ((RedProjectEditor) editor).getRedProjectEditorInput() : null;
    }

    private RobotProjectConfig provideConfig(final RedProjectEditorInput redProjectInput) {
        if (redProjectInput == null) {
            final RobotProjectConfigReader reader = new RobotProjectConfigReader();
            return reader.readConfiguration(externalFile);
        } else {
            return redProjectInput.getProjectConfiguration();
        }
    }

    public abstract boolean apply(final IFile externalFile, RobotProjectConfig config) throws ProposalApplyingException;

    @Override
    public Point getSelection(final IDocument document) {
        return null;
    }

    @Override
    public String getAdditionalProposalInfo() {
        return detailedDescription;
    }

    @Override
    public String getDisplayString() {
        return shortDescription;
    }

    @Override
    public Image getImage() {
        return null;
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    static class ProposalApplyingException extends RuntimeException {

        public ProposalApplyingException(final String message) {
            super(message);
        }

        public ProposalApplyingException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }
}
