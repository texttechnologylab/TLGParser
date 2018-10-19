package org.hucompute.tlgparser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class DiameterThread extends Thread {
    protected TLGGraph graph;
    protected Set<TLGNode> subSet;
    protected int diameter;
    protected TLGGraph.Directedness directedness;

    public DiameterThread(TLGGraph pGraph, Set<TLGNode> pNodeSubSet, TLGGraph.Directedness pDirectedness) {
        graph = pGraph;
        subSet = pNodeSubSet;
        directedness = pDirectedness;
    }

    public DiameterThread(TLGGraph pGraph, TLGGraph.Directedness pDirectedness) {
        graph = pGraph;
        subSet = new HashSet<>(pGraph.nodeMap.values());
        directedness = pDirectedness;
    }

    public void run() {
        diameter = 0;
        for (TLGNode lNode:subSet) {
            List<TLGNode> lQueue = new ArrayList<>();
            List<Integer> lDepthQueue = new ArrayList<>();
            Set<TLGNode> lKnown = new HashSet<>();
            lQueue.add(lNode);
            lDepthQueue.add(0);
            lKnown.add(lNode);
            while (lQueue.size() > 0) {
                TLGNode lCurrent = lQueue.remove(lQueue.size() - 1);
                int lCurrentDepth = lDepthQueue.remove(lDepthQueue.size() - 1);
                diameter = Math.max(diameter, lCurrentDepth);
                for (TLGNode lOther : lCurrent.getLinkedNodes(directedness.equals(TLGGraph.Directedness.DIRECTED) ? TLGGraph.Direction.OUT : TLGGraph.Direction.ANY)) {
                    if (!lKnown.contains(lOther)) {
                        lQueue.add(0, lOther);
                        lDepthQueue.add(0, lCurrentDepth + 1);
                        lKnown.add(lOther);
                    }
                }
            }
        }
        synchronized (graph) {
            graph.notifyAll();
        }
    }
}
