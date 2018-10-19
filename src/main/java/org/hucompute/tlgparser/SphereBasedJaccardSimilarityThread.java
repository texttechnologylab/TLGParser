package org.hucompute.tlgparser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

public class SphereBasedJaccardSimilarityThread extends GraphSimilarityThread {

    private static Logger logger = LogManager.getLogger(SphereBasedJaccardSimilarityThread.class);

    /**
     *
     * @param pMonitor
     * @param pGraph1
     * @param pGraph2
     * @param pDirectedness
     */
    public SphereBasedJaccardSimilarityThread(Object pMonitor, TLGGraph pGraph1, TLGGraph pGraph2, TLGGraph.Directedness pDirectedness) {
        super(pMonitor, pGraph1, pGraph2, pDirectedness);
    }

    public void run() {
        try {
            Set<TLGNode> lAllNodes = new HashSet<>();
            lAllNodes.addAll(graph1.getNodes());
            lAllNodes.addAll(graph2.getNodes());
            double lSum = 0;
            int lCounter = 0;
            long lStart = System.currentTimeMillis();
            long lInterval = System.currentTimeMillis();
            for (TLGNode lNode1 : graph1.getNodes()) {
                if (System.currentTimeMillis() - lInterval > 10000) {
                    lInterval = System.currentTimeMillis();
                    logger.info("Thread "+Thread.currentThread().getId()+": "+lCounter+"/"+graph1.getNodes().size()+", "+(System.currentTimeMillis()-lStart)+"ms elapsed");
                }
                lCounter++;
                TLGNode lNode2 = graph2.getNodeByID(lNode1.getId());
                if (lNode2 != null) {
                    lSum += lNode1.getSphereBasedNeighbourhoodSimilarity(lNode2, directedness);
                }
            }
            similarity = lSum / lAllNodes.size();
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
        catch (Exception e) {
            logger.error(e.getMessage(), e);
            exception = e;
            System.exit(0);
        }
    }

}
