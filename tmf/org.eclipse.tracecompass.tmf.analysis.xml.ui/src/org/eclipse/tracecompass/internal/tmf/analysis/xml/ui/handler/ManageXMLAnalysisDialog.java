/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.handler;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.Activator;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlAnalysisModuleSource;
import org.eclipse.tracecompass.tmf.analysis.xml.core.module.XmlUtils;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfCommonProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectModelElement;
import org.eclipse.tracecompass.tmf.ui.project.model.TmfProjectRegistry;
import org.eclipse.tracecompass.tmf.ui.project.model.TraceUtils;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog for XML analysis files
 *
 * @author Jean-Christian Kouame
 */
public class ManageXMLAnalysisDialog extends Dialog {

    private final String XML_FILTER_EXTENSION = "*.xml"; //$NON-NLS-1$
    private List fAnalysesList;
    private Button fDeleteButton;
    private Button fImportButton;
    private Button fExportButton;
    private Label fInvalidFileLabel;

    /**
     * Constructor
     *
     * @param parent
     *            Parent shell of this dialog
     */
    public ManageXMLAnalysisDialog(Shell parent) {
        super(parent);
        setShellStyle(SWT.RESIZE | SWT.MAX | getShellStyle());
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        getShell().setText(Messages.ManageXMLAnalysisDialog_ManageXmlAnalysesFiles);

        Composite composite = (Composite) super.createDialogArea(parent);
        composite.setLayout(new GridLayout(2, false));

        Composite listContainer = new Composite(composite, SWT.NONE);
        listContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        GridLayout lcgl = new GridLayout();
        lcgl.marginHeight = 0;
        lcgl.marginWidth = 0;
        listContainer.setLayout(lcgl);

        fAnalysesList = new List(listContainer, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        fAnalysesList.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        fAnalysesList.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                if (fAnalysesList.getSelectionCount() == 0) {
                    fDeleteButton.setEnabled(false);
                    fExportButton.setEnabled(false);
                } else {
                    fDeleteButton.setEnabled(true);
                    fExportButton.setEnabled(true);
                    handleSelection(fAnalysesList.getSelection());
                }
            }
        });

        fInvalidFileLabel = new Label(listContainer, SWT.ICON_ERROR);
        fInvalidFileLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        fInvalidFileLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
        fInvalidFileLabel.setText(Messages.ManageXMLAnalysisDialog_FileValidationError);
        fInvalidFileLabel.setVisible(false);

        Composite buttonContainer = new Composite(composite, SWT.NULL);
        buttonContainer.setLayout(new GridLayout());
        buttonContainer.setLayoutData(new GridData(SWT.CENTER, SWT.TOP, false, false));

        fImportButton = new Button(buttonContainer, SWT.PUSH);
        fImportButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        fImportButton.setText(Messages.ManageXMLAnalysisDialog_Import);
        fImportButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                importAnalysis();
            }
        });

        fExportButton = new Button(buttonContainer, SWT.PUSH);
        fExportButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        fExportButton.setText(Messages.ManageXMLAnalysisDialog_Export);
        fExportButton.setEnabled(false);
        fExportButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                exportAnalysis();
            }
        });

        fDeleteButton = new Button(buttonContainer, SWT.PUSH);
        fDeleteButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
        fDeleteButton.setText(Messages.ManageXMLAnalysisDialog_Delete);
        fDeleteButton.setEnabled(false);
        fDeleteButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

            @Override
            public void widgetSelected(SelectionEvent e) {
                deleteAnalysis();
            }

        });

        fillAnalysesList();

        getShell().setMinimumSize(300, 275);
        return composite;
    }

    private void handleSelection(String[] selection) {
        Map<String, File> files = XmlUtils.listFiles();
        File file = files.get(createXmlFileString(selection[0]));
        if (file != null && XmlUtils.xmlValidate(file).isOK()) {
            fInvalidFileLabel.setVisible(false);
        } else {
            fInvalidFileLabel.setVisible(true);
        }
    }

    private static void deleteSupplementaryFile(String xmlFile) {
        // 1. Look for all traces that have this analysis
        // 2. Close them if they are opened.
        // 3. Delete the related supplementary files
        java.util.List<IResource> resourceToDelete = new ArrayList<>();
        java.util.List<String> ids = XmlUtils.getAnalysisIdsFromFile(xmlFile);
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects(0);
        for (IProject project : projects) {
            TmfProjectElement pElement = TmfProjectRegistry.getProject(project);
            if (pElement != null) {
                java.util.List<TmfCommonProjectElement> tElements = new ArrayList<>();
                tElements.addAll(pElement.getTracesFolder().getTraces());
                tElements.addAll(pElement.getExperimentsFolder().getExperiments());
                for (TmfCommonProjectElement tElement : tElements) {
                    boolean closeEditor = false;
                    for (IResource resource : tElement.getSupplementaryResources()) {
                        for (String id : ids) {
                            if (resource.getName().startsWith(id)) {
                                resourceToDelete.add(resource);
                                closeEditor = true;
                            }
                        }
                    }
                    if (closeEditor) {
                        tElement.closeEditors();
                    }
                }
            }
        }
        for (IResource resource : resourceToDelete) {
            try {
                resource.delete(false, null);
            } catch (CoreException e) {
                Activator.logError(NLS.bind(Messages.ManageXMLAnalysisDialog_DeleteFileError, resource.getName()));
            }
        }
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, false);
    }

    private void fillAnalysesList() {
        fAnalysesList.removeAll();
        Map<String, File> files = XmlUtils.listFiles();
        for (String file : files.keySet()) {
            // Remove the extension from the file path. The extension is at the
            // end of the file path
            IPath path = new Path(file);
            fAnalysesList.add(path.removeFileExtension().toString());
        }
        fDeleteButton.setEnabled(false);
        fExportButton.setEnabled(false);
    }

    private void importAnalysis() {
        FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.OPEN);
        dialog.setText(Messages.ManageXMLAnalysisDialog_SelectFileImport);
        dialog.setFilterNames(new String[] { Messages.ManageXMLAnalysisDialog_ImportXmlFile + " (*.xml)" }); //$NON-NLS-1$
        dialog.setFilterExtensions(new String[] { XML_FILTER_EXTENSION });
        String path = dialog.open();
        if (path != null) {
            File file = new File(path);
            IStatus status = XmlUtils.xmlValidate(file);
            if (status.isOK()) {
                status = XmlUtils.addXmlFile(file);
                if (status.isOK()) {
                    fillAnalysesList();
                    XmlAnalysisModuleSource.notifyModuleChange();
                    /*
                     * FIXME: It refreshes the list of analysis under a trace,
                     * but since modules are instantiated when the trace opens,
                     * the changes won't apply to an opened trace, it needs to
                     * be closed then reopened
                     */
                    refreshProject();
                } else {
                    Activator.logError(Messages.ManageXMLAnalysisDialog_ImportFileFailed);
                    TraceUtils.displayErrorMsg(Messages.ManageXMLAnalysisDialog_ImportFileFailed, status.getMessage());
                }
            } else {
                Activator.logError(Messages.ManageXMLAnalysisDialog_ImportFileFailed);
                TraceUtils.displayErrorMsg(Messages.ManageXMLAnalysisDialog_ImportFileFailed, status.getMessage());
            }
        }
    }

    private void exportAnalysis() {
        FileDialog dialog = new FileDialog(Display.getCurrent().getActiveShell(), SWT.SAVE);
        dialog.setText(NLS.bind(Messages.ManageXMLAnalysisDialog_SelectFileExport, fAnalysesList.getSelection()[0]));
        dialog.setFilterExtensions(new String[] { XML_FILTER_EXTENSION, "*" }); //$NON-NLS-1$
        String selection = createXmlFileString(fAnalysesList.getSelection()[0]);
        dialog.setFileName(selection);
        String path = dialog.open();
        if (path != null) {
            if (!XmlUtils.exportXmlFile(selection, path).isOK()) {
                Activator.logError(NLS.bind(Messages.ManageXMLAnalysisDialog_FailedToExport, selection));
            }
        }
    }

    private void deleteAnalysis() {
        boolean confirm = MessageDialog.openQuestion(
                getShell(),
                Messages.ManageXMLAnalysisDialog_DeleteFile,
                NLS.bind(Messages.ManageXMLAnalysisDialog_DeleteConfirmation, fAnalysesList.getSelection()[0]));
        if (confirm) {
            String selection = createXmlFileString(fAnalysesList.getSelection()[0]);
            deleteSupplementaryFile(selection);
            XmlUtils.deleteFile(selection);
            fillAnalysesList();
            fInvalidFileLabel.setVisible(false);
            XmlAnalysisModuleSource.notifyModuleChange();
            /*
             * FIXME: It refreshes the list of analysis under a trace, but since
             * modules are instantiated when the trace opens, the changes won't
             * apply to an opened trace, it needs to be closed then reopened
             */
            refreshProject();
        }
    }

    /**
     * Refresh the selected project with the new XML file import
     */
    private static void refreshProject() {
        // Check if we are closing down
        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
        if (window == null) {
            return;
        }

        // Get the selection
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IWorkbenchPart part = page.getActivePart();
        if (part == null) {
            return;
        }
        ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
        if (selectionProvider == null) {
            return;
        }
        ISelection selection = selectionProvider.getSelection();

        if (selection instanceof TreeSelection) {
            TreeSelection sel = (TreeSelection) selection;
            // There should be only one item selected as per the plugin.xml
            Object element = sel.getFirstElement();
            if (element instanceof TmfProjectModelElement) {
                ((TmfProjectModelElement) element).getProject().refresh();
            }
        }
    }

    private static String createXmlFileString(String baseName) {
        IPath path = new Path(baseName).addFileExtension(XmlUtils.XML_EXTENSION);
        return path.toString();
    }
}
