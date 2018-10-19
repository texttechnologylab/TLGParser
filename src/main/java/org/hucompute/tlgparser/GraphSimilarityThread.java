package org.hucompute.tlgparser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class GraphSimilarityThread extends Thread {

    private static Logger logger = LogManager.getLogger(GraphSimilarityThread.class);

    public enum GraphSimMethod {sphere, ged, veo};

    protected TLGGraph graph1;
    protected TLGGraph graph2;
    protected TLGGraph.Directedness directedness;
    protected double similarity;
    protected Object monitor;
    protected Exception exception;

    /**
     *
     * @param pMonitor
     * @param pGraph1
     * @param pGraph2
     * @param pDirectedness
     */
    public GraphSimilarityThread(Object pMonitor, TLGGraph pGraph1, TLGGraph pGraph2, TLGGraph.Directedness pDirectedness) {
        monitor = pMonitor;
        graph1 = pGraph1;
        graph2 = pGraph2;
        directedness = pDirectedness;
    }

    public abstract void run();

    public TLGGraph getGraph1() {
        return graph1;
    }

    public TLGGraph getGraph2() {
        return graph2;
    }

    public TLGGraph.Directedness getDirectedness() {
        return directedness;
    }

    public double getSimilarity() {
        return similarity;
    }

}
