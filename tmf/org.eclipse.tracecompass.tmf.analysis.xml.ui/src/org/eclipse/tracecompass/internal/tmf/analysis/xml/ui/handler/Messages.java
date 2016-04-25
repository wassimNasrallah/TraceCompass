/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.handler;

import org.eclipse.osgi.util.NLS;

/**
 * Externalized messages for the XML analysis module package
 *
 * @author Jean-Christian Kouame
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.tmf.analysis.xml.ui.handler.messages"; //$NON-NLS-1$

    /** Import XML file title */
    public static String ManageXMLAnalysisDialog_ImportXmlFile;

    /**
     * Delete
     */
    public static String ManageXMLAnalysisDialog_Delete;

    /**
     * Delete confirmation message
     */
    public static String ManageXMLAnalysisDialog_DeleteConfirmation;

    /**
     * Delete a file
     */
    public static String ManageXMLAnalysisDialog_DeleteFile;

    /**
     * Delete file error message
     */
    public static String ManageXMLAnalysisDialog_DeleteFileError;

    /**
     * Export XML file message
     */
    public static String ManageXMLAnalysisDialog_Export;

    /**
     * Export failed message
     */
    public static String ManageXMLAnalysisDialog_FailedToExport;

    /**
     * Import XML file message
     */
    public static String ManageXMLAnalysisDialog_Import;

    /**
     * Import file failed message
     */
    public static String ManageXMLAnalysisDialog_ImportFileFailed;

    /**
     * Manage XML analyses files message
     */
    public static String ManageXMLAnalysisDialog_ManageXmlAnalysesFiles;

    /**
     * Select file to export message
     */
    public static String ManageXMLAnalysisDialog_SelectFileExport;

    /**
     * Select file to import message
     */
    public static String ManageXMLAnalysisDialog_SelectFileImport;

    /**
     * Invalid file message
     */
    public static String ManageXMLAnalysisDialog_FileValidationError;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
