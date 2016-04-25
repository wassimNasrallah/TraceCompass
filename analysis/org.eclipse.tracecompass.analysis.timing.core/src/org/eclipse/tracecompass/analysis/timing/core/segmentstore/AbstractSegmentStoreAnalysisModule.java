/*******************************************************************************
 * Copyright (c) 2015 Ericsson
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.tracecompass.analysis.timing.core.segmentstore;

import static org.eclipse.tracecompass.common.core.NonNullUtils.checkNotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.tracecompass.segmentstore.core.ISegment;
import org.eclipse.tracecompass.segmentstore.core.ISegmentStore;
import org.eclipse.tracecompass.segmentstore.core.treemap.TreeMapStore;
import org.eclipse.tracecompass.tmf.core.analysis.TmfAbstractAnalysisModule;
import org.eclipse.tracecompass.tmf.core.event.ITmfEvent;
import org.eclipse.tracecompass.tmf.core.exceptions.TmfAnalysisException;
import org.eclipse.tracecompass.tmf.core.request.ITmfEventRequest;
import org.eclipse.tracecompass.tmf.core.request.TmfEventRequest;
import org.eclipse.tracecompass.tmf.core.segment.ISegmentAspect;
import org.eclipse.tracecompass.tmf.core.trace.ITmfTrace;
import org.eclipse.tracecompass.tmf.core.trace.TmfTraceManager;

/**
 * Abstract analysis module to generate a segment store. It is a base class that
 * can be used as a shortcut by analysis who just need to build a single segment
 * store.
 *
 * @author Bernd Hufmann
 * @since 2.0
 *
 */
public abstract class AbstractSegmentStoreAnalysisModule extends TmfAbstractAnalysisModule implements ISegmentStoreProvider {

    private final ListenerList fListeners = new ListenerList(ListenerList.IDENTITY);

    private @Nullable ISegmentStore<ISegment> fSegmentStore;

    private @Nullable ITmfEventRequest fOngoingRequest = null;

    @Override
    public void addListener(IAnalysisProgressListener listener) {
        fListeners.add(listener);
    }

    @Override
    public void removeListener(IAnalysisProgressListener listener) {
        fListeners.remove(listener);
    }

    /**
     * Returns all the listeners
     *
     * @return latency listeners
     */
    protected Iterable<IAnalysisProgressListener> getListeners() {
        List<IAnalysisProgressListener> listeners = new ArrayList<>();
        for (Object listener : fListeners.getListeners()) {
            if (listener != null) {
                listeners.add((IAnalysisProgressListener) listener);
            }
        }
        return listeners;
    }

    @Override
    public Iterable<ISegmentAspect> getSegmentAspects() {
        return Collections.emptyList();
    }

    /**
     * Returns the file name for storing segment store
     *
     * @return segment store fine name, or null if you don't want a file
     */
    protected @Nullable String getDataFileName() {
        return null;
    }

    /**
     * Returns the analysis request for creating the segment store
     *
     * @param segmentStore
     *            a segment store to fill
     * @return the segment store analysis request implementation
     */
    protected abstract AbstractSegmentStoreAnalysisRequest createAnalysisRequest(ISegmentStore<ISegment> segmentStore);

    /**
     * Read an object from the ObjectInputStream.
     *
     * @param ois
     *            the ObjectInputStream to used
     * @return the read object
     * @throws ClassNotFoundException
     *             - Class of a serialized object cannot be found.
     * @throws IOException
     *             - Any of the usual Input/Output related exceptions.
     */
    protected abstract Object[] readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException;

    @Override
    public @Nullable ISegmentStore<ISegment> getSegmentStore() {
        return fSegmentStore;
    }

    @Override
    protected void canceling() {
        ITmfEventRequest req = fOngoingRequest;
        if ((req != null) && (!req.isCompleted())) {
            req.cancel();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        ISegmentStore<ISegment> store = fSegmentStore;
        if (store != null) {
            store.dispose();
        }
    }

    @Override
    protected boolean executeAnalysis(IProgressMonitor monitor) throws TmfAnalysisException {
        ITmfTrace trace = checkNotNull(getTrace());

        final @Nullable String dataFileName = getDataFileName();
        if (dataFileName != null) {
            /* See if the data file already exists on disk */
            String dir = TmfTraceManager.getSupplementaryFileDir(trace);
            final Path file = Paths.get(dir, dataFileName);

            if (Files.exists(file)) {
                /* Attempt to read the existing file */
                try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(file))) {
                    Object[] segmentArray = readObject(ois);
                    final ISegmentStore<ISegment> store = new TreeMapStore<>();
                    for (Object element : segmentArray) {
                        if (element instanceof ISegment) {
                            ISegment segment = (ISegment) element;
                            store.add(segment);
                        }
                    }
                    fSegmentStore = store;
                    for (IAnalysisProgressListener listener : getListeners()) {
                        listener.onComplete(this, store);
                    }
                    return true;
                } catch (IOException | ClassNotFoundException | ClassCastException e) {
                    /*
                     * We did not manage to read the file successfully, we will
                     * just fall-through to rebuild a new one.
                     */
                    try {
                        Files.delete(file);
                    } catch (IOException e1) {
                    }
                }
            }
        }
        ISegmentStore<ISegment> segmentStore = new TreeMapStore<>();

        /* Cancel an ongoing request */
        ITmfEventRequest req = fOngoingRequest;
        if ((req != null) && (!req.isCompleted())) {
            req.cancel();
        }

        /* Create a new request */
        req = createAnalysisRequest(segmentStore);
        fOngoingRequest = req;
        trace.sendRequest(req);

        try {
            req.waitForCompletion();
        } catch (InterruptedException e) {
        }

        /* Do not process the results if the request was cancelled */
        if (req.isCancelled() || req.isFailed()) {
            return false;
        }

        /* The request will fill 'syscalls' */
        fSegmentStore = segmentStore;

        if (dataFileName != null) {
            String dir = TmfTraceManager.getSupplementaryFileDir(trace);
            final Path file = Paths.get(dir, dataFileName);

            /* Serialize the collections to disk for future usage */
            try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(file))) {
                oos.writeObject(segmentStore.toArray());
            } catch (IOException e) {
                /*
                 * Didn't work, oh well. We will just re-read the trace next
                 * time
                 */
            }
        }

        for (IAnalysisProgressListener listener : getListeners()) {
            listener.onComplete(this, segmentStore);
        }

        return true;
    }

    /**
     * Abstract event request to fill a a segment store
     */
    protected static abstract class AbstractSegmentStoreAnalysisRequest extends TmfEventRequest {

        private final ISegmentStore<ISegment> fFullLatencyStore;

        /**
         * Constructor
         *
         * @param latencyStore
         *            a latency segment store to fill
         */
        public AbstractSegmentStoreAnalysisRequest(ISegmentStore<ISegment> latencyStore) {
            super(ITmfEvent.class, 0, ITmfEventRequest.ALL_DATA, ExecutionType.BACKGROUND);
            /*
             * We do NOT make a copy here! We want to modify the list that was
             * passed in parameter.
             */
            fFullLatencyStore = latencyStore;
        }

        /**
         * Returns the segment store
         *
         * @return the segment store
         */
        public ISegmentStore<ISegment> getSegmentStore() {
            return fFullLatencyStore;
        }
    }
}