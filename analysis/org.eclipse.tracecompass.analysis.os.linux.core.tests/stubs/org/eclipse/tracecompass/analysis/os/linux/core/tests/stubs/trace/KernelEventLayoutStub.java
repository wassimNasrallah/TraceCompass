/*******************************************************************************
 * Copyright (c) 2016 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.os.linux.core.tests.stubs.trace;

import org.eclipse.tracecompass.analysis.os.linux.core.trace.DefaultEventLayout;
import org.eclipse.tracecompass.analysis.os.linux.core.trace.IKernelAnalysisEventLayout;

/**
 * Class to extend to be able to set the event names for the os unit tests.
 *
 * @author Geneviève Bastien
 */
public class KernelEventLayoutStub extends DefaultEventLayout {

    /**
     * Protected constructor
     */
    protected KernelEventLayoutStub() {
        super();
    }

    private static final IKernelAnalysisEventLayout INSTANCE = new KernelEventLayoutStub();

    /**
     * Get an instance of this event layout
     *
     * This object is completely immutable, so no need to create additional
     * instances via the constructor.
     *
     * @return The instance
     */
    public static IKernelAnalysisEventLayout getInstance() {
        return INSTANCE;
    }

}
