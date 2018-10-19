package org.hucompute.tlgparser;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TLGEdge {

    protected static Pattern propertyPattern = Pattern.compile("\\[(.*?)¤(.*?)¤\\]¤");
    protected TLGGraph graph;
    protected Map<String, String> properties = new HashMap<>();
    protected TLGNode source;
    protected TLGNode target;
    protected double similarity;

    public TLGEdge(TLGGraph pTLGGraph, String pLine) {
        graph = pTLGGraph;
        Matcher lMatcher = propertyPattern.matcher(pLine);
        while (lMatcher.find()) {
            if (lMatcher.groupCount() == 2) {
                properties.put(lMatcher.group(1), lMatcher.group(2));
                TObjectLongHashMap<String> lMap = graph.edgeAttributeCountMap.get(lMatcher.group(1));
                if (lMap == null) {
                    lMap = new TObjectLongHashMap<>();
                    graph.edgeAttributeCountMap.put(lMatcher.group(1), lMap);
                }
                lMap.adjustOrPutValue(lMatcher.group(2), 1, 1);
            }
        }
        source = pTLGGraph.nodeMap.get(pLine.substring(0, pLine.indexOf("¤")));
        source.edges.add(this);
        pLine = pLine.substring(pLine.indexOf("¤")+1);
        target = pTLGGraph.nodeMap.get(pLine.substring(0, pLine.indexOf("¤")));
        assert target != null;
        target.edges.add(this);
        pLine = pLine.substring(pLine.indexOf("¤")+1);
        if (Character.isDigit(pLine.charAt(0))) {
            similarity = Double.parseDouble(pLine.substring(0, pLine.indexOf("¤")));
        }
        if (properties.containsKey("Type")) {
            String lType = properties.get("Type");
            if (lType.length() > 0) {
                graph.edgeTypes.adjustOrPutValue(lType, 1, 1);
            }
        }
    }

    public double getSimilarity() {
        return similarity;
    }

    public void remove() {
        source.edges.remove(this);
        target.edges.remove(this);
    }

    public TLGEdge(TLGGraph pTLGGraph, TLGNode pSource, TLGNode pTarget) {
        graph = pTLGGraph;
        source = pSource;
        source.edges.add(this);
        target = pTarget;
        assert target != null;
        target.edges.add(this);
        properties = new HashMap<>();
    }

    public Map<String, String> getProperties() {
        return properties;
    }

    public String getProperty(String pKey, String pDefault) {
        String lResult = properties.get(pKey);
        return lResult != null ? lResult : pDefault;
    }

    public TLGNode getSource() {
        return source;
    }

    public TLGNode getTarget() {
        return target;
    }

    public TLGNode getOther(TLGNode pNode) {
        if (pNode.equals(source)) {
            return target;
        }
        else {
            return source;
        }
    }

}
