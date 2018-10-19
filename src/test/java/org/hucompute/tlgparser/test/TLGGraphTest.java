package org.hucompute.tlgparser.test;

import org.hucompute.tlgparser.GraphSimilarityThread;
import org.hucompute.tlgparser.TLGEdge;
import org.hucompute.tlgparser.TLGGraph;
import org.hucompute.tlgparser.TLGNode;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TLGGraphTest {

    @Test
    public void checkDiameter() {
        try {
            TLGGraph lGraph = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirected.tlg"), TLGGraph.Directedness.DIRECTED);
            Assert.assertEquals(3, lGraph.getDiameter(TLGGraph.Directedness.DIRECTED));
            Assert.assertEquals(4, lGraph.getDiameter(TLGGraph.Directedness.UNDIRECTED));
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkDiameterMT() {
        try {
            TLGGraph lGraph = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirected.tlg"), TLGGraph.Directedness.DIRECTED);
            Assert.assertEquals(3, lGraph.getDiameterMT(TLGGraph.Directedness.DIRECTED));
            Assert.assertEquals(4, lGraph.getDiameterMT(TLGGraph.Directedness.UNDIRECTED));
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkSphericSimilarityIDBased() {
        try {
            TLGGraph lGraph1 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirectedIDsOnly.tlg"), TLGGraph.Directedness.DIRECTED);
            TLGGraph lGraph2 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirectedIDsOnly.tlg"), TLGGraph.Directedness.DIRECTED);
            TLGNode lNode1 = lGraph1.getNodeByID("1");
            TLGNode lNode2 = lGraph2.getNodeByID("1");
            for (TLGNode lNode:new TLGNode[]{lNode1, lNode2}) {
                {
                    Map<Integer, Set<TLGNode>> lMap = lNode.getSphereMap(Integer.MAX_VALUE, TLGGraph.Directedness.DIRECTED);
                    Assert.assertEquals(4, lMap.size());
                    Assert.assertEquals(1, lMap.get(0).size());
                    Assert.assertEquals(2, lMap.get(1).size());
                    Assert.assertEquals(1, lMap.get(2).size());
                    Assert.assertEquals(1, lMap.get(3).size());
                }
                {
                    Map<Integer, Set<TLGNode>> lMap = lNode.getSphereMap(Integer.MAX_VALUE, TLGGraph.Directedness.UNDIRECTED);
                    Assert.assertEquals(5, lMap.size());
                    Assert.assertEquals(1, lMap.get(0).size());
                    Assert.assertEquals(2, lMap.get(1).size());
                    Assert.assertEquals(1, lMap.get(2).size());
                    Assert.assertEquals(1, lMap.get(3).size());
                    Assert.assertEquals(1, lMap.get(4).size());
                }
            }
            Assert.assertEquals(1.0, lNode1.getSphereBasedNeighbourhoodSimilarity(lNode2, TLGGraph.Directedness.DIRECTED), 0);
            List<TLGGraph> lGraphs = new ArrayList<>();
            lGraphs.add(lGraph1);
            lGraphs.add(lGraph2);
            double[][] lMatrix = TLGGraph.getGraphSimilarities(GraphSimilarityThread.GraphSimMethod.sphere, lGraphs, TLGGraph.Directedness.DIRECTED, 1);
            Assert.assertEquals(1, lMatrix[0][1], 0);
            Assert.assertEquals(1, lMatrix[1][0], 0);
            lMatrix = TLGGraph.getGraphSimilarities(lGraphs, "1", TLGGraph.Directedness.DIRECTED);
            Assert.assertEquals(1, lMatrix[0][1], 0);
            Assert.assertEquals(1, lMatrix[1][0], 0);
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkBFGMLEquivalence() {
        try {
            TLGGraph lGraph1 = TLGGraph.fromGMLFile(new File("src/test/resources/6NodeDirected.gml"), TLGGraph.Directedness.DIRECTED);
            TLGGraph lGraph2 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirected.tlg"), TLGGraph.Directedness.DIRECTED);
            Assert.assertTrue(lGraph1.isEqualDirected(lGraph2));
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkGraphIsEqual() {
        try {
            TLGGraph lGraph1 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirected.tlg"), TLGGraph.Directedness.DIRECTED);
            TLGGraph lGraph2 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirected.tlg"), TLGGraph.Directedness.DIRECTED);
            Assert.assertTrue(lGraph1.isEqualDirected(lGraph2));
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkUnionGraphDirected() {
        try {
            TLGGraph lGraph1 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/UnionGraphTest1.tlg"), TLGGraph.Directedness.DIRECTED);
            TLGGraph lGraph2 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/UnionGraphTest2.tlg"), TLGGraph.Directedness.DIRECTED);
            TLGGraph lGraphUnion = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/UnionGraphTestUnion.tlg"), TLGGraph.Directedness.DIRECTED);
            List<TLGGraph> lList = new ArrayList<>();
            lList.add(lGraph1);
            lList.add(lGraph2);
            Assert.assertTrue(TLGGraph.buildUnionGraph(lList, TLGGraph.Directedness.DIRECTED).isEqualDirected(lGraphUnion));
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void checkBFGMLEquivalenceLabelBased() {
        try {
            TLGGraph lGraph1 = TLGGraph.fromGMLFile(new File("src/test/resources/6NodeDirected.gml"), TLGGraph.Directedness.DIRECTED, true);
            TLGGraph lGraph2 = TLGGraph.fromBorlandFormatFile(new File("src/test/resources/6NodeDirectedLabelBased.tlg"), TLGGraph.Directedness.DIRECTED);
            Assert.assertEquals(lGraph1.getNodes().size(), lGraph2.getNodes().size());
            for (TLGNode lNode1:lGraph1.getNodes()) {
                Assert.assertTrue(lGraph2.getNodeByID(lNode1.getId()) != null);
                TLGNode lNode2 = lGraph2.getNodeByID(lNode1.getId());
                Assert.assertEquals(lNode1.getProperties().size(), lNode2.getProperties().size());
                for (Map.Entry<String, String> lEntry:lNode1.getProperties().entrySet()) {
                    Assert.assertTrue(lNode2.getProperties().containsKey(lEntry.getKey()));
                    Assert.assertEquals(lEntry.getValue(), lNode2.getProperty(lEntry.getKey(), null));
                }
                Assert.assertEquals(lNode1.getEdges(TLGGraph.Direction.OUT).size(), lNode2.getEdges(TLGGraph.Direction.OUT).size());
                for (TLGEdge lEdge1:lNode1.getEdges(TLGGraph.Direction.OUT)) {
                    boolean lFoundMatchingEdge = false;
                    for (TLGEdge lEdge2:lNode2.getEdges(TLGGraph.Direction.OUT)) {
                        if (lEdge1.getSource().getId().equals(lEdge2.getSource().getId())
                                && lEdge1.getTarget().getId().equals(lEdge2.getTarget().getId())) {
                            if (lEdge1.getProperties().size() == lEdge2.getProperties().size()) {
                                lFoundMatchingEdge = true;
                                for (Map.Entry<String, String> lEntry:lEdge1.getProperties().entrySet()) {
                                    if (!lEdge2.getProperties().containsKey(lEntry.getKey())) {
                                        lFoundMatchingEdge = false;
                                        break;
                                    }
                                    if (!lEntry.getValue().equals(lEdge2.getProperty(lEntry.getKey(), null))) {
                                        lFoundMatchingEdge = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (lFoundMatchingEdge) break;
                    }
                    Assert.assertTrue(lFoundMatchingEdge);
                }
            }
        }
        catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {

    }

}
