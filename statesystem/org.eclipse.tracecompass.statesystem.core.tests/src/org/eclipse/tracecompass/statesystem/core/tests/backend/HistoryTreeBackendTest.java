/*******************************************************************************
 * Copyright (c) 2016 Ericsson, EfficiOS Inc. and others
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.tracecompass.statesystem.core.tests.backend;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.tracecompass.internal.statesystem.core.backend.historytree.HistoryTreeBackend;
import org.eclipse.tracecompass.statesystem.core.backend.IStateHistoryBackend;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Test the {@link HistoryTreeBackend} class.
 *
 * @author Patrick Tasse
 * @author Alexandre Montplaisir
 */
@RunWith(Parameterized.class)
public class HistoryTreeBackendTest extends StateHistoryBackendTestBase {

    /** State system ID */
    protected static final @NonNull String SSID = "test";
    /** Provider version */
    protected static final int PROVIDER_VERSION = 0;

    /** Default maximum number of children nodes */
    protected static final int MAX_CHILDREN = 2;
    /** Default block size */
    protected static final int BLOCK_SIZE = 4096;

    /** ReOpen test parameter */
    protected final boolean fReOpen;

    /** Set of created history tree files */
    protected Set<File> fHistoryTreeFiles = new HashSet<>();
    /** Map of backends to history tree file */
    protected Map<IStateHistoryBackend, File> fBackendMap = new HashMap<>();
    /** Maximum number of children nodes */
    protected int fMaxChildren = MAX_CHILDREN;
    /** Block size */
    protected int fBlockSize = BLOCK_SIZE;

    /**
     * @return the test parameters
     */
    @Parameters(name = "ReOpen={0}")
    public static Collection<Boolean> parameters() {
        return Arrays.asList(Boolean.FALSE, Boolean.TRUE);
    }

    /**
     * Constructor
     *
     * @param reOpen
     *            True if the backend should be disposed and re-opened as a new
     *            backend from the file, or false to use the backend as-is
     */
    public HistoryTreeBackendTest(Boolean reOpen) {
        fReOpen = reOpen;
    }

    /**
     * Test cleanup
     */
    @After
    public void teardown() {
        for (IStateHistoryBackend backend : fBackendMap.keySet()) {
            backend.dispose();
        }
        for (File historyTreeFile : fHistoryTreeFiles) {
            historyTreeFile.delete();
        }
    }

    @Override
    protected IStateHistoryBackend getBackendForBuilding(long startTime) throws IOException {
        File historyTreeFile = File.createTempFile("HistoryTreeBackendTest", ".ht");
        fHistoryTreeFiles.add(historyTreeFile);
        HistoryTreeBackend backend = new HistoryTreeBackend(SSID, historyTreeFile, PROVIDER_VERSION, startTime, fBlockSize, fMaxChildren);
        fBackendMap.put(backend, historyTreeFile);
        return backend;
    }

    @Override
    protected IStateHistoryBackend getBackendForQuerying(IStateHistoryBackend backend) throws IOException {
        if (!fReOpen) {
            return backend;
        }
        File historyTreeFile = fBackendMap.remove(backend);
        backend.dispose();
        HistoryTreeBackend reOpenedBackend = new HistoryTreeBackend(SSID, historyTreeFile, PROVIDER_VERSION);
        fBackendMap.put(reOpenedBackend, historyTreeFile);
        return reOpenedBackend;
    }
}
