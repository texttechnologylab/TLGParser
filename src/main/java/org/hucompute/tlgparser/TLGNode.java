package org.hucompute.tlgparser;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TLGNode {

    protected static Pattern propertyPattern = Pattern.compile("\\[(.*?)¤(.*?)¤\\]¤");
    protected String id;
    protected Map<String, String> properties = new HashMap<>();
    protected TLGGraph graph;
    protected Set<TLGEdge> edges = new HashSet<>();
    private Map<Integer, Set<TLGNode>> cachedSphereMap = null;
    private int cachedSphereMaxDepth = 0;
    private TLGGraph.Directedness cachedSphereDirectedness = TLGGraph.Directedness.DIRECTED;

    public TLGNode(TLGGraph pTLGGraph, String pID, Map<String, String> pProperties) {
        graph = pTLGGraph;
        id = pID;
        properties = pProperties;
    }

    public TLGNode(TLGGraph pTLGGraph, String pLine) {
        graph = pTLGGraph;
        id = pLine.substring(0, pLine.indexOf("¤"));
        Matcher lMatcher = propertyPattern.matcher(pLine);
        while (lMatcher.find()) {
            if (lMatcher.groupCount() == 2) {
                properties.put(lMatcher.group(1), lMatcher.group(2));
                TObjectLongHashMap<String> lMap = graph.nodeAttributeCountMap.get(lMatcher.group(1));
                if (lMap == null) {
                    lMap = new TObjectLongHashMap<>();
                    graph.nodeAttributeCountMap.put(lMatcher.group(1), lMap);
                }
                lMap.adjustOrPutValue(lMatcher.group(2), 1, 1);
            }
        }
    }

    public void removeEdge(TLGEdge pEdge) {
        pEdge.getSource().edges.remove(pEdge);
        pEdge.getTarget().edges.remove(pEdge);
    }

    public Set<TLGEdge> getEdges(TLGGraph.Direction pDirection) {
        switch (pDirection) {
            case IN: {
                Set<TLGEdge> lResult = new HashSet<>();
                for (TLGEdge lEdge:edges) {
                    if (lEdge.target.equals(this)) {
                        lResult.add(lEdge);
                    }
                }
                return lResult;
            }
            case OUT: {
                Set<TLGEdge> lResult = new HashSet<>();
                for (TLGEdge lEdge:edges) {
                    if (lEdge.source.equals(this)) {
                        lResult.add(lEdge);
                    }
                }
                return lResult;
            }
            case ANY:
            default: {
                return edges;
            }
        }
    }

    public Set<TLGNode> getNodes(TLGGraph.Direction pDirection, String pEdgeProperty, String pValue) {
        Set<TLGNode> lResult = new HashSet<>();
        for (TLGEdge lEdge:edges) {
            switch (pDirection) {
                case IN: {
                    if (lEdge.target.equals(this)) {
                        if (lEdge.getProperty(pEdgeProperty, "").equals(pValue)) {
                            lResult.add(lEdge.source);
                        }
                    }
                    break;
                }
                case OUT: {
                    if (lEdge.source.equals(this)) {
                        if (lEdge.getProperty(pEdgeProperty, "").equals(pValue)) {
                            lResult.add(lEdge.target);
                        }
                    }
                    break;
                }
                case ANY:
                default: {
                    if (lEdge.getProperty(pEdgeProperty, "").equals(pValue)) {
                        lResult.add(lEdge.source.equals(this) ? lEdge.target : lEdge.source);
                    }
                }
            }
        }
        return lResult;
    }

    public Set<TLGNode> getLinkedNodes(TLGGraph.Direction pDirection) {
        Set<TLGNode> lResult = new HashSet<>();
        for (TLGEdge lEdge:edges) {
            switch (pDirection) {
                case IN: {
                    if (lEdge.target.equals(this)) {
                        lResult.add(lEdge.source);
                    }
                    break;
                }
                case OUT: {
                    if (lEdge.source.equals(this)) {
                        lResult.add(lEdge.target);
                    }
                    break;
                }
                case ANY:
                default: {
                    lResult.add(lEdge.source.equals(this) ? lEdge.target : lEdge.source);
                }
            }
        }
        return lResult;
    }

    public String getId() {
        return id;
    }

    public String getId(String pIDProperty) {
        if (pIDProperty == null) {
            return id;
        }
        else {
            return properties.get(pIDProperty);
        }
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String pKey, String pDefault) {
        String lResult = properties.get(pKey);
        return lResult != null ? lResult : pDefault;
    }

    public synchronized Map<Integer, Set<TLGNode>> getSphereMap(int pMaxSpheres, TLGGraph.Directedness pDirectedness) {
        if ((pMaxSpheres == cachedSphereMaxDepth) && (cachedSphereMap != null) && (pDirectedness.equals(cachedSphereDirectedness))) {
            return cachedSphereMap;
        }
        Map<Integer, Set<TLGNode>> lThisSphereMap = new HashMap<>();
        int lThisMaxDepth = 0;
        {
            List<TLGNode> lQueue = new ArrayList<>();
            List<Integer> lDistanceQueue = new ArrayList<>();
            Set<TLGNode> lKnown = new HashSet<>();
            lQueue.add(this);
            lDistanceQueue.add(0);
            lKnown.add(this);
            while (lQueue.size() > 0) {
                TLGNode lNode = lQueue.remove(lQueue.size()-1);
                int lDistance = lDistanceQueue.remove(lDistanceQueue.size()-1);
                lThisMaxDepth = Math.max(lThisMaxDepth, lDistance);
                if (!lThisSphereMap.containsKey(lDistance)) {
                    lThisSphereMap.put(lDistance, new HashSet<>());
                }
                lThisSphereMap.get(lDistance).add(lNode);
                for (TLGNode lOther:lNode.getLinkedNodes(pDirectedness.equals(TLGGraph.Directedness.UNDIRECTED) ? TLGGraph.Direction.ANY : TLGGraph.Direction.OUT)) {
                    if (!lKnown.contains(lOther)) {
                        if (lDistance < pMaxSpheres) {
                            lKnown.add(lOther);
                            lQueue.add(0, lOther);
                            lDistanceQueue.add(0, lDistance + 1);
                        }
                    }
                }
            }
        }
        cachedSphereMap = lThisSphereMap;
        cachedSphereDirectedness = pDirectedness;
        cachedSphereMaxDepth = pMaxSpheres;
        return cachedSphereMap;
    }

    public double getSphereBasedNeighbourhoodSimilarity(TLGNode pOtherNode, TLGGraph.Directedness pDirectedness) {
        Map<Integer, Set<TLGNode>> lThisSphereMap = getSphereMap(Integer.MAX_VALUE, pDirectedness);
        Map<Integer, Set<TLGNode>> lOtherSphereMap = pOtherNode.getSphereMap(Integer.MAX_VALUE, pDirectedness);
        double lResult = 0;
        int lN = Math.max(lThisSphereMap.size()-1, lOtherSphereMap.size()-1);
        for (int i=1; i<=lN; i++) {
            Set<TLGNode> lSet1 = lThisSphereMap.containsKey(i) ? lThisSphereMap.get(i) : new HashSet<>();
            Set<TLGNode> lSet2 = lOtherSphereMap.containsKey(i) ? lOtherSphereMap.get(i) : new HashSet<>();
            int lIntersectionSize = 0;
            for (TLGNode lNode:lSet1) {
                if (lSet2.contains(lNode)) lIntersectionSize++;
            }
            Set<TLGNode> lUnion = new HashSet<>();
            lUnion.addAll(lSet1);
            lUnion.addAll(lSet2);
            lResult += (lN+1-i) *  (lUnion.size() > 0 ? lIntersectionSize / (double)lUnion.size() : 1);
        }
        return lN > 0 ? lResult * (2d/(lN*(lN+1))) : 1;
    }

    /**
     * @deprecated
     * @param pOtherNode
     * @param pMaxSpheres
     * @param pDirectedness
     * @return
     */
    public double getSphereBasedJaccardSimilarity(TLGNode pOtherNode, int pMaxSpheres, TLGGraph.Directedness pDirectedness, String pIDProperty) {
        Map<Integer, Set<TLGNode>> lThisSphereMap = getSphereMap(pMaxSpheres, pDirectedness);
        Map<Integer, Set<TLGNode>> lOtherSphereMap = pOtherNode.getSphereMap(pMaxSpheres, pDirectedness);
        if ((lThisSphereMap.get(1) == null) && (lOtherSphereMap.get(1) == null)) return 1.0;
        double lResult = 0;
        double lNormalizer = 0;
        for (int i=1; i<=pMaxSpheres; i++) {
            Set<TLGNode> lSet1 = lThisSphereMap.containsKey(i) ? lThisSphereMap.get(i) : new HashSet<>();
            Set<TLGNode> lSet2 = lOtherSphereMap.containsKey(i) ? lOtherSphereMap.get(i) : new HashSet<>();
            Set<String> lLabelSet1 = new HashSet<>();
            for (TLGNode lNode:lSet1) lLabelSet1.add(lNode.getId(pIDProperty));
            Set<String> lLabelSet2 = new HashSet<>();
            for (TLGNode lNode:lSet2) lLabelSet2.add(lNode.getId(pIDProperty));
            Set<String> lIntersection = new HashSet<>();
            for (String lNode:lLabelSet1) {
                if (lLabelSet2.contains(lNode)) lIntersection.add(lNode);
            }
            Set<String> lUnion = new HashSet<>();
            lUnion.addAll(lLabelSet1);
            lUnion.addAll(lLabelSet2);
            if (lUnion.size() > 0) {
                double lSphereJaccard = lIntersection.size() / (double)lUnion.size();
                lNormalizer += 1d/i;
                lResult += lSphereJaccard * (1d/i);
            }
            else {
                break;
            }
        }
        return lNormalizer > 0 ? lResult/lNormalizer : 0;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof TLGNode)) return false;
        return id.equals(((TLGNode)obj).id);
    }
}
