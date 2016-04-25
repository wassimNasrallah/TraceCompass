/*******************************************************************************
 * Copyright (c) 2015 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.tracecompass.analysis.graph.core.base;

import java.util.Collections;
import java.util.Map;

/**
 * Interface that the objects in a graph may implement
 *
 * @author Geneviève Bastien
 */
public interface IGraphWorker {

    /**
     * Get the host ID of the trace this worker belongs to
     *
     * @return The host ID of the trace this worker belongs to
     */
    String getHostId();

    /**
     * Get additional information on this worker at time t. This would be
     * textual information, in the form of key, value pairs, that could be
     * displayed for instance as extra columns for this worker in a graph view.
     *
     * @return A key, value map of information this worker provides.
     * @since 2.0
     */
    default Map<String, String> getWorkerInformation() {
        return Collections.EMPTY_MAP;
    }

    /**
     * Get additional information on this worker at time t. This would be
     * textual information, in the form of key, value pairs, that could be
     * displayed for instance as a tooltip in the graph view.
     *
     * @param t
     *            Time at which to get the information
     * @return A key, value map of information this worker provides.
     * @since 2.0
     */
    default Map<String, String> getWorkerInformation(long t) {
        return Collections.EMPTY_MAP;
    }

}
