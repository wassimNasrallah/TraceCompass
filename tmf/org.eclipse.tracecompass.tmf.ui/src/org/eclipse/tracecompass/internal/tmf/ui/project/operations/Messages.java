/*******************************************************************************
 * Copyright (c) 2016 Ericsson
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
  *******************************************************************************/

package org.eclipse.tracecompass.internal.tmf.ui.project.operations;

import org.eclipse.osgi.util.NLS;

/**
 * The messages for workspace operations.
 * @author Bernd Hufmann
 */
public class Messages extends NLS {
    private static final String BUNDLE_NAME = "org.eclipse.tracecompass.internal.tmf.ui.project.operations.messages"; //$NON-NLS-1$

    /**
     * The task name for removing of a trace for an experiment.
     */
    public static String SelectTracesWizardPage_TraceRemovalTask;

    /**
     * The task name for selecting of a trace for an experiment.
     */
    public static String SelectTracesWizardPage_TraceSelectionTask;

    /**
     * The error message when selecting of traces for an experiment fails.
     */
    public static String SelectTracesWizardPage_SelectionError;

    /** The error message when an experiment could not be created */
    public static String NewExperimentOperation_CreationError;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
