package org.hucompute.tlgparser;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Verted/Edge Overlap (VEO) according to Papadimitriou et al. 2008
 */
public class VEOSimilarityTask extends GraphSimilarityThread {

    private static Logger logger = LogManager.getLogger(VEOSimilarityTask.class);

    public VEOSimilarityTask(Object pMonitor, TLGGraph pGraph1, TLGGraph pGraph2, TLGGraph.Directedness pDirectedness) {
        super(pMonitor, pGraph1, pGraph2, pDirectedness);
    }

    @Override
    public void run() {
        try {
            Set<String> lG1Nodes = new HashSet<>(graph1.nodeMap.keySet());
            Set<String> lG2Nodes = new HashSet<>(graph2.nodeMap.keySet());
            Set<String> lG1Edges = new HashSet<>();
            for (TLGNode lNode:graph1.getNodes()) {
                for (TLGNode lTarget : lNode.getLinkedNodes(TLGGraph.Direction.OUT)) {
                    lG1Edges.add(lNode.getId() + "\t" + lTarget.getId());
                    if (directedness.equals(TLGGraph.Directedness.UNDIRECTED)) {
                        lG1Edges.add(lTarget.getId() + "\t" + lNode.getId());
                    }
                }
            }
            Set<String> lG2Edges = new HashSet<>();
            for (TLGNode lNode:graph2.getNodes()) {
                for (TLGNode lTarget:lNode.getLinkedNodes(TLGGraph.Direction.OUT)) {
                    lG2Edges.add(lNode.getId()+"\t"+lTarget.getId());
                    if (directedness.equals(TLGGraph.Directedness.UNDIRECTED)) {
                        lG2Edges.add(lTarget.getId()+"\t"+lNode.getId());
                    }
                }
            }
            if ((lG1Edges.size()+lG2Edges.size()+lG1Nodes.size()+lG2Nodes.size()) == 0) {
                similarity = 1;
            }
            else {
                similarity = 2d * (CollectionUtil.getIntersection(lG1Edges, lG2Edges).size() + CollectionUtil.getIntersection(lG1Nodes, lG2Nodes).size()) /
                        (double) (lG1Edges.size() + lG2Edges.size() + lG1Nodes.size() + lG2Nodes.size());
            }
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

    public static void main(String[] args) throws Exception {
        TLGGraph lGraph1 = TLGGraph.fromBorlandFormatFile(new File("testdata/3.bf.txt"));
        TLGGraph lGraph2 = TLGGraph.fromBorlandFormatFile(new File("testdata/4.bf.txt"));
        VEOSimilarityTask lVEOSimilarityTask = new VEOSimilarityTask(new Object(), lGraph1, lGraph2, TLGGraph.Directedness.DIRECTED);
        lVEOSimilarityTask.run();
        System.out.println(lVEOSimilarityTask.getSimilarity());
    }
}
