package org.hucompute.tlgparser;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.*;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;

import static org.hucompute.tlgparser.TLGGraph.Directedness.DIRECTED;

public class TLGGraph {

    public static int MAX_THREADS = Runtime.getRuntime().availableProcessors()/4;

    private static Logger logger = LogManager.getLogger(TLGGraph.class);

    public enum Direction {IN, OUT, ANY};

    public enum Directedness {DIRECTED, UNDIRECTED};

    public enum Format {BorlandFormat, GML};

    protected Map<String, TLGNode> nodeMap;
    protected Map<String, TLGNode> languageTypeNameIndex;
    protected Map<String, TObjectLongHashMap<String>> nodeAttributeCountMap = new HashMap<>();
    protected Map<String, TObjectLongHashMap<String>> edgeAttributeCountMap = new HashMap<>();
    protected String head;
    protected TObjectLongHashMap<String> nodeTypes = new TObjectLongHashMap<>();
    protected TObjectLongHashMap<String> edgeTypes = new TObjectLongHashMap<>();
    protected Directedness directedness;

    private Map<Directedness, Integer> diameterCache = new HashMap<>();

    private TLGGraph(){}

    public TLGGraph(TLGGraph pTLGGraph) {
        nodeMap = new HashMap<>();
        Map<TLGNode, TLGNode> lOldNewMap = new HashMap<>();
        for (TLGNode lOldNode: pTLGGraph.nodeMap.values()) {
            TLGNode lNewNode = new TLGNode(this, lOldNode.id, new HashMap<>(lOldNode.properties));
            lOldNewMap.put(lOldNode, lNewNode);
            nodeMap.put(lNewNode.getId(), lNewNode);
        }
        for (TLGNode lOldNode: pTLGGraph.nodeMap.values()) {
            for (TLGEdge lOldEdge:lOldNode.getEdges(Direction.OUT)) {
                TLGEdge lNewEdge = new TLGEdge(this, lOldNewMap.get(lOldEdge.source), lOldNewMap.get(lOldEdge.target));
                lNewEdge.properties = new HashMap<>(lOldEdge.properties);
            }
        }
    }

    public String getHead() {
        return head;
    }

    public void setHead(String head) {
        this.head = head;
    }

    public static TLGGraph fromBorlandFormatFile(File pFile, Directedness pDirectedness) throws IOException {
        TLGGraph lTLGGraph = new TLGGraph();
        lTLGGraph.directedness = pDirectedness;
        lTLGGraph.initialize(pFile, Format.BorlandFormat);
        return lTLGGraph;
    }

    public static TLGGraph fromBorlandFormatFile(File pFile) throws IOException {
        TLGGraph lTLGGraph = new TLGGraph();
        lTLGGraph.directedness = DIRECTED;
        lTLGGraph.initialize(pFile, Format.BorlandFormat);
        return lTLGGraph;
    }

    public static TLGGraph fromGMLFile(File pFile, Directedness pDirectedness) throws IOException {
        TLGGraph lTLGGraph = new TLGGraph();
        lTLGGraph.directedness = pDirectedness;
        lTLGGraph.initializeFromGML(pFile, false);
        return lTLGGraph;
    }

    public static TLGGraph fromGMLFile(File pFile, Directedness pDirectedness, boolean pLabelAsID) throws IOException {
        TLGGraph lTLGGraph = new TLGGraph();
        lTLGGraph.directedness = pDirectedness;
        lTLGGraph.initializeFromGML(pFile, pLabelAsID);
        return lTLGGraph;
    }

    private void initialize(File pFile, Format pFormat) throws IOException {
        switch (pFormat) {
            case BorlandFormat: {
                initializeFromTGF(pFile);
                break;
            }
            case GML: {
                initializeFromGML(pFile, false);
                break;
            }
        }
    }

    /**
     * Importing as directed or undirected, discards Loops and Parallel Edges (for directed graphs will still allow A>B and B>A)
     * @param pFile
     * @throws IOException
     */
    private void initializeFromGML(File pFile, boolean pLabelAsID) throws IOException {
        head = (directedness.equals(DIRECTED) ? "directed\n" : "undirected\n")+ "SimilarityGraph\n" +
                "Vertex Attributes:"+(pLabelAsID?"":"[Label¤String];")+"\n" +
                "Edge Attributes:\n" +
                "ProbabilityMassOfGraph: 0\n";

        nodeMap = new HashMap<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(pFile)), Charset.forName("UTF-8")));
        String lLine = null;
        Map<String, String> lIDLabelMap = new HashMap<>();
        Map<String, String> lLabelIDMap = new HashMap<>();
        //Set<String> lParallelEdgeBlocker = new HashSet<>();
        while ((lLine = lReader.readLine()) != null) {
            lLine = lLine.trim();
            if (lLine.startsWith("node")) {
                String lID = null;
                String lLabel = null;
                while (!(lLine = lReader.readLine()).trim().startsWith("]")) {
                    lLine = lLine.trim();
                    if (lLine.startsWith("id")) {
                        lID = lLine.substring(lLine.lastIndexOf(" ")+1);
                    }
                    else if (lLine.startsWith("label")) {
                        lLabel = lLine.substring(lLine.indexOf("\"")+1, lLine.lastIndexOf("\""));
                    }
                }
                if (lLabel != null) {
                    lIDLabelMap.put(lID, lLabel);
                    if (pLabelAsID) {
                        if (lLabelIDMap.containsKey(lLabel)) {
                            throw new IOException("Duplicate Label: "+lLabel);
                        }
                    }
                    lLabelIDMap.put(lLabel, lID);
                }
                else {
                    if (pLabelAsID) throw new IOException("Label is null for ID: "+lID);
                }
                Map<String, String> lParameters = new HashMap<>();
                if (!pLabelAsID && (lLabel != null)) {
                    lParameters.put("Label", lLabel);
                }
                TLGNode lNode = pLabelAsID ? new TLGNode(this, lLabel, lParameters) : new TLGNode(this, lID, lParameters);
                nodeMap.put(lNode.getId(), lNode);
            }
            else if (lLine.startsWith("edge")) {
                String lSource = null;
                String lTarget = null;
                String lType = null;
                while (!(lLine = lReader.readLine()).trim().startsWith("]")) {
                    lLine = lLine.trim();
                    if (lLine.startsWith("source")) {
                        lSource = lLine.substring(lLine.lastIndexOf(" ")+1);
                    }
                    else if (lLine.startsWith("target")) {
                        lTarget = lLine.substring(lLine.lastIndexOf(" ")+1);
                    }
                    else if (lLine.startsWith("type")) {
                        lType = lLine.substring(lLine.indexOf("\"")+1, lLine.lastIndexOf("\""));
                    }
                    else if (lLine.startsWith("Type")) {
                        lType = lLine.substring(lLine.indexOf("\"")+1, lLine.lastIndexOf("\""));
                    }
                }
                //if (!lSource.equals(lTarget)) {
                    String lKey = directedness.equals(Directedness.UNDIRECTED) ? getSortedIdPair(lSource, lTarget) : lSource+"\t"+lTarget;
                    //if (!lParallelEdgeBlocker.contains(lKey)) {
                    //    lParallelEdgeBlocker.add(lKey);
                        TLGEdge lEdge = pLabelAsID ? new TLGEdge(this, nodeMap.get(lIDLabelMap.get(lSource)), nodeMap.get(lIDLabelMap.get(lTarget))) : new TLGEdge(this, nodeMap.get(lSource), nodeMap.get(lTarget));
                        if (lType != null) lEdge.properties.put("Type", lType);
                    //}
                    //}
                //}
            }
        }
        lReader.close();
    }

    private void initializeFromTGF(File pFile) throws IOException {
        nodeMap = new HashMap<>();
        languageTypeNameIndex = new HashMap<>();
        BufferedReader lReader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(pFile)), Charset.forName("UTF-8")));
        StringBuilder lHeadBuilder = new StringBuilder();
        String lLine = null;
        while (!(lLine = lReader.readLine()).startsWith("Vertices:")) {
            lHeadBuilder.append(lLine+"\n");
        }
        head = lHeadBuilder.toString();
        while (!(lLine = lReader.readLine()).startsWith("Edges:")) {
            TLGNode lTLGNode = new TLGNode(this, lLine);
            String lPOS = lTLGNode.getProperty("POS", null);
            String lName = lTLGNode.getProperty("Name", null);
            String lLanguage = lTLGNode.getProperty("Language", null);
            String lType = lTLGNode.getProperty("Type", null);
            if ((lType != null) && (lType.length() > 0)) {
                nodeTypes.adjustOrPutValue(lType, 1, 1);
            }
            if ((lPOS != null) && (lName != null) && (lLanguage != null) && (lType != null)) {
                if (lType.equals("SuperLemma")) languageTypeNameIndex.put(lLanguage+"\t"+lPOS+"\t"+lName, lTLGNode);
            }
            nodeMap.put(lTLGNode.getId(), lTLGNode);
            if (nodeMap.size() % 1000 == 0) {
                logger.info(nodeMap.size()+" nodes read");
            }
        }
        logger.info(nodeMap.size()+" nodes read");
        long lEdgesRead = 0;
        while ((lLine = lReader.readLine()) != null) {
            if (lLine.length() > 0) {
                lEdgesRead++;
                new TLGEdge(this, lLine);
                if (lEdgesRead % 1000 == 0) {
                    logger.info(lEdgesRead+" edges read");
                }
            }
        }
        logger.info(lEdgesRead+" edges read");
        lReader.close();
    }

    /**
     * Get Diameter of Graph multithreaded. Results are cached and computed only once.
     * @param pDirectedness
     * @return Diameter of Graph
     */
    public int getDiameter(Directedness pDirectedness) {
        synchronized (diameterCache) {
            Integer lCachedResult = diameterCache.get(pDirectedness);
            if (lCachedResult != null) {
                return lCachedResult;
            }
        }
        int lResult = 0;
        long lMax = nodeMap.size();
        int lCurrentPerc = 0;
        long lCounter = 0;
        for (TLGNode lNode:nodeMap.values()) {
            lCounter++;
            int lPerc = (int)Math.floor(lCounter*100/(double)lMax);
            if (lPerc != lCurrentPerc) {
                logger.info("Diameter: "+lPerc+"%");
                lCurrentPerc = lPerc;
            }
            List<TLGNode> lQueue = new ArrayList<>();
            List<Integer> lDepthQueue = new ArrayList<>();
            Set<TLGNode> lKnown = new HashSet<>();
            lQueue.add(lNode);
            lDepthQueue.add(0);
            lKnown.add(lNode);
            while (lQueue.size() > 0) {
                TLGNode lCurrent = lQueue.remove(lQueue.size()-1);
                int lCurrentDepth = lDepthQueue.remove(lDepthQueue.size()-1);
                lResult = Math.max(lResult, lCurrentDepth);
                for (TLGNode lOther:lCurrent.getLinkedNodes(pDirectedness.equals(Directedness.DIRECTED) ? Direction.OUT : Direction.ANY)) {
                    if (!lKnown.contains(lOther)) {
                        lQueue.add(0, lOther);
                        lDepthQueue.add(0, lCurrentDepth+1);
                        lKnown.add(lOther);
                    }
                }
            }
        }
        synchronized (diameterCache) {
            diameterCache.put(pDirectedness, lResult);
        }
        return lResult;
    }

    /**
     * Get Diameter of Graph multithreaded. Results are cached and computed only once.
     * @param pDirectedness
     * @return Diameter of Graph
     */
    public int getDiameterMT(Directedness pDirectedness) {
        synchronized (diameterCache) {
            Integer lCachedResult = diameterCache.get(pDirectedness);
            if (lCachedResult != null) {
                return lCachedResult;
            }
        }
        logger.info("Diameter: 0%");
        int lResult = 0;
        int lMaxThreads = MAX_THREADS;
        final TLGGraph _this = this;
        List<Set<TLGNode>> lPartition = new ArrayList<>();
        for (int i=0; i<lMaxThreads; i++) {
            lPartition.add(new HashSet<>());
        }
        int lCycle = 0;
        for (TLGNode lNode:nodeMap.values()) {
            lPartition.get(lCycle).add(lNode);
            lCycle++;
            if (lCycle == lPartition.size()) lCycle = 0;
        }
        List<DiameterThread> lThreads = new ArrayList<>();
        for (Set<TLGNode> p:lPartition) {
            DiameterThread lDiameterThread = new DiameterThread(this, p, pDirectedness);
            lThreads.add(lDiameterThread);
            lDiameterThread.start();
        }
        while (lThreads.size() > 0) {
            Iterator<DiameterThread> i = lThreads.iterator();
            while (i.hasNext()) {
                DiameterThread lDiameterThread = i.next();
                if (lDiameterThread.getState().equals(Thread.State.TERMINATED)) {
                    logger.info("Diameter: "+((lMaxThreads-lThreads.size())*100d/lMaxThreads)+"%");
                    i.remove();
                    lResult = Math.max(lResult, lDiameterThread.diameter);
                }
            }
            synchronized (this) {
                try {
                    this.wait(100);
                }
                catch (InterruptedException e) {
                }
            }
        }
        logger.info("Diameter: 100%");
        synchronized (diameterCache) {
            diameterCache.put(pDirectedness, lResult);
        }
        return lResult;
    }

    public TLGNode createNode(TLGNode pNodeFromOtherGraph) {
        assert !nodeMap.containsKey(pNodeFromOtherGraph.getId());
        String lType = pNodeFromOtherGraph.getProperty("Type", null);
        if ((lType != null) && (lType.length() > 0)) {
            nodeTypes.adjustOrPutValue(lType, 1, 1);
        }
        TLGNode lNode = new TLGNode(this, pNodeFromOtherGraph.id, new HashMap<>(pNodeFromOtherGraph.getProperties()));
        nodeMap.put(lNode.getId(), lNode);
        return lNode;
    }

    public Collection<TLGNode> getNodes() {
        return nodeMap.values();
    }

    public TLGNode getBFNodeByTypeAndName(String pLanguage, String pPOS, String pName) {
        return languageTypeNameIndex.get(pLanguage+"\t"+pPOS+"\t"+pName);
    }

    public TLGNode getNodeByID(String pID) {
        return nodeMap.get(pID);
    }

    public void saveSubGraph(File pFile, Set<TLGNode> pIncludedNodes, boolean pUndirectedEdges) throws IOException {
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pFile)), Charset.forName("UTF-8")));
        lWriter.print(head);
        lWriter.println("Vertices:");
        int lNodeCounter = 0;
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            StringBuilder lLine = new StringBuilder();
            lLine.append(lNode.id+"¤");
            for (Map.Entry<String, String> lEntry:lNode.getProperties().entrySet()) {
                lLine.append("["+lEntry.getKey()+"¤"+lEntry.getValue()+"¤]¤");
            }
            lWriter.println(lLine.toString());
        }
        logger.info("Exporting Nodes: "+lNodeCounter);
        lWriter.println("Edges:");
        lNodeCounter = 0;
        Set<String> lSkipSet = new HashSet<>();
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                if (pIncludedNodes.contains(lEdge.target)) {
                    String lKey = null;
                    if (lEdge.source.id.compareTo(lEdge.target.id) < 0) {
                        lKey = lEdge.source.id+"\t"+lEdge.target.id;
                    }
                    else {
                        lKey = lEdge.target.id+"\t"+lEdge.source.id;
                    }
                    if (!pUndirectedEdges || (!lSkipSet.contains(lKey))) {
                        lSkipSet.add(lKey);
                        StringBuilder lLine = new StringBuilder();
                        lLine.append(lEdge.source.id + "¤");
                        lLine.append(lEdge.target.id + "¤");
                        lLine.append("1.0¤");
                        for (Map.Entry<String, String> lEntry : lEdge.getProperties().entrySet()) {
                            lLine.append("[" + lEntry.getKey() + "¤" + lEntry.getValue() + "¤]¤");
                        }
                        lWriter.println(lLine.toString());
                    }
                }
            }
        }
        logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
        lWriter.close();
    }

    /**
     * Format for http://gedevo.mpi-inf.mpg.de/
     * Cannot export isolated Nodes
     * @param pFile
     * @throws IOException
     */
    public void saveGraphSIF(File pFile) throws IOException {
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pFile)), Charset.forName("UTF-8")));
        Set<TLGNode> lNodes = new HashSet<>(nodeMap.values());
        for (TLGNode lNode:lNodes) {
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                String lKey = lEdge.getSource().getId()+" d "+lEdge.getTarget().getId();
                lWriter.println(lKey);
            }
        }
        lWriter.close();
    }

    public void saveGraph(File pFile) throws IOException {
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pFile)), Charset.forName("UTF-8")));
        lWriter.print(head);
        lWriter.println("Vertices:");
        int lNodeCounter = 0;
        Set<TLGNode> lNodes = new HashSet<>(nodeMap.values());
        for (TLGNode lNode:lNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Nodes: "+lNodeCounter+"/"+lNodes.size());
            }
            StringBuilder lLine = new StringBuilder();
            lLine.append(lNode.id+"¤");
            for (Map.Entry<String, String> lEntry:lNode.getProperties().entrySet()) {
                lLine.append("["+lEntry.getKey()+"¤"+lEntry.getValue()+"¤]¤");
            }
            lWriter.println(lLine.toString());
        }
        logger.info("Exporting Nodes: "+lNodeCounter);
        lWriter.println("Edges:");
        lNodeCounter = 0;
        for (TLGNode lNode:lNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+lNodes.size());
            }
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                StringBuilder lLine = new StringBuilder();
                lLine.append(lEdge.source.id + "¤");
                lLine.append(lEdge.target.id + "¤");
                lLine.append(lEdge.similarity+"¤");
                for (Map.Entry<String, String> lEntry : lEdge.getProperties().entrySet()) {
                    lLine.append("[" + lEntry.getKey() + "¤" + lEntry.getValue() + "¤]¤");
                }
                lWriter.println(lLine.toString());
            }
        }
        logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+lNodes.size());
        lWriter.close();
    }

    public List<Set<TLGNode>> getWeaklyConnectedComponents() {
        TObjectIntHashMap<TLGNode> lNodeComponentMap = new TObjectIntHashMap<>();
        for (TLGNode lNode:getNodes()) {
            lNodeComponentMap.put(lNode, -1);
        }
        int lClusterCounter = -1;
        List<Set<TLGNode>> lResult = new ArrayList<>();
        Set<TLGNode> lCurrentComponent = null;
        for (TLGNode lNode:getNodes()) {
            if (lNodeComponentMap.get(lNode) == -1) {
                lClusterCounter++;
                lCurrentComponent = new HashSet<>();
                lResult.add(lCurrentComponent);
                List<TLGNode> lQueue = new ArrayList<>();
                Set<TLGNode> lKnown = new HashSet<>();
                lQueue.add(lNode);
                lKnown.add(lNode);
                while (lQueue.size() > 0) {
                    TLGNode lSub = lQueue.remove(lQueue.size()-1);
                    lNodeComponentMap.put(lSub, lClusterCounter);
                    lCurrentComponent.add(lSub);
                    for (TLGNode lOther:lSub.getLinkedNodes(Direction.ANY)) {
                        if (!lKnown.contains(lOther)) {
                            lKnown.add(lOther);
                            lQueue.add(0, lOther);
                        }
                    }
                }
            }
        }
        lResult.sort((s1,s2)->Integer.compare(s2.size(), s1.size()));
        return lResult;
    }

    public void saveSubGraph(File pFile, Set<TLGNode> pIncludedNodes, Set<String> pEdgeTypes, boolean pNormalizeParallelEdges) throws IOException {
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pFile)), Charset.forName("UTF-8")));
        lWriter.print(head);
        lWriter.println("Vertices:");
        int lNodeCounter = 0;
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            StringBuilder lLine = new StringBuilder();
            lLine.append(lNode.id+"¤");
            for (Map.Entry<String, String> lEntry:lNode.getProperties().entrySet()) {
                lLine.append("["+lEntry.getKey()+"¤"+lEntry.getValue()+"¤]¤");
            }
            lWriter.println(lLine.toString());
        }
        logger.info("Exporting Nodes: "+lNodeCounter);
        lWriter.println("Edges:");
        lNodeCounter = 0;
        Set<String> lEdgeKeeper = new HashSet<>();
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                if (pEdgeTypes.contains(lEdge.getProperty("Type", ""))) {
                    if (pIncludedNodes.contains(lEdge.target)) {
                        String lKey = lEdge.source.id+"\t"+lEdge.target.id;
                        if (!lEdgeKeeper.contains(lKey) || !pNormalizeParallelEdges) {
                            lEdgeKeeper.add(lKey);
                            StringBuilder lLine = new StringBuilder();
                            lLine.append(lEdge.source.id + "¤");
                            lLine.append(lEdge.target.id + "¤");
                            lLine.append("1.0¤");
                            for (Map.Entry<String, String> lEntry : lEdge.getProperties().entrySet()) {
                                lLine.append("[" + lEntry.getKey() + "¤" + lEntry.getValue() + "¤]¤");
                            }
                            lWriter.println(lLine.toString());
                        }
                    }
                }
            }
        }
        logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
        lWriter.close();
    }

    public void saveSubGraphGML(File pFile, Set<TLGNode> pIncludedNodes, Set<String> pEdgeTypes, boolean pNormalizeParallelEdges) throws IOException {
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pFile)), Charset.forName("UTF-8")));
        lWriter.println("graph [");
        int lNodeCounter = 0;
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            String lName = lNode.getProperty("Name", "");
            lWriter.println("node [");
            lWriter.println("id "+lNode.id);
            lWriter.println("label \""+lName.replace("\"", "'")+"\"");
            lWriter.println("]");
        }
        logger.info("Exporting Nodes: "+lNodeCounter);
        lNodeCounter = 0;
        Set<String> lEdgeKeeper = new HashSet<>();
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                if (pEdgeTypes.contains(lEdge.getProperty("Type", ""))) {
                    if (pIncludedNodes.contains(lEdge.target)) {
                        String lKey = lEdge.source.id+"\t"+lEdge.target.id;
                        if (!lEdgeKeeper.contains(lKey) || !pNormalizeParallelEdges) {
                            lEdgeKeeper.add(lKey);
                            lWriter.println("edge [");
                            lWriter.println("source "+lEdge.source.id);
                            lWriter.println("target "+lEdge.target.id);
                            lWriter.println("]");
                        }
                    }
                }
            }
        }
        lWriter.println("]");
        logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
        lWriter.close();
    }

    public double getClusteringCoefficientWattsStrogatz() {
        double lResult = 0;
        long lNodeCount = 0;
        long lCounter = 0;
        int lLastPerc = -1;
        for (TLGNode lNode:getNodes()) {
            lCounter++;
            int lPerc = (int)Math.floor((lCounter * 100d)/getNodes().size());
            if (lPerc != lLastPerc) {
                logger.info("ClusteringCoefficientWattsStrogatz: "+lPerc+"%");
                lLastPerc = lPerc;
            }
            Set<TLGNode> lNeighbours = lNode.getLinkedNodes(Direction.ANY);
            lNeighbours.remove(lNode); // Paranoia- should not be necessary
            long lNeighbourCount = lNeighbours.size();
            if (lNeighbourCount >= 2) {
                long lMaxEdges = (lNeighbourCount * (lNeighbourCount - 1)) / 2;
                Set<String> lEdges = new HashSet<>();
                for (TLGNode lNeighbour : lNeighbours) {
                    for (TLGEdge lEdge : lNeighbour.getEdges(Direction.OUT)) {
                        if (lNeighbours.contains(lEdge.getTarget()) && !lEdge.getSource().equals(lEdge.getTarget())) {
                            if (lEdge.getSource().getId().compareTo(lEdge.getTarget().getId()) < 0) {
                                lEdges.add(lEdge.getSource().getId()+"\t"+lEdge.getTarget().getId());
                            }
                            else {
                                lEdges.add(lEdge.getTarget().getId()+"\t"+lEdge.getSource().getId());
                            }
                        }
                    }
                }
                lResult += lEdges.size() / (double) lMaxEdges;
                lNodeCount++;
            }
        }
        return lResult/lNodeCount;
    }

    public void saveSubGraphGML(File pFile, Set<TLGNode> pIncludedNodes, boolean pUndirectedEdges) throws IOException {
        long lEdgesRead = 0;
        long lExportedEdgeCounter = 0;
        PrintWriter lWriter = new PrintWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(pFile)), Charset.forName("ISO-8859-1")));
        lWriter.println("graph [");
        int lNodeCounter = 0;
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            String lName = lNode.getProperty("Label", "");
            lWriter.println("node [");
            lWriter.println("id "+lNode.id);
            lWriter.println("label \""+lName.replace("\"", "'").replace("&", "&amp;")+"\"");
            lWriter.println("]");
        }
        logger.info("Exporting Nodes: "+lNodeCounter);
        lNodeCounter = 0;
        Set<String> lSkipSet = new HashSet<>();
        for (TLGNode lNode:pIncludedNodes) {
            lNodeCounter++;
            if (lNodeCounter % 1000 == 0) {
                logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
            }
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                if (pIncludedNodes.contains(lEdge.target)) {
                    lEdgesRead++;
                    String lKey = null;
                    if (lEdge.source.id.compareTo(lEdge.target.id) < 0) {
                        lKey = lEdge.source.id+"\t"+lEdge.target.id;
                    }
                    else {
                        lKey = lEdge.target.id+"\t"+lEdge.source.id;
                    }
                    if (!pUndirectedEdges || (!lSkipSet.contains(lKey))) {
                        lExportedEdgeCounter++;
                        lSkipSet.add(lKey);
                        lWriter.println("edge [");
                        lWriter.println("source " + lEdge.source.id);
                        lWriter.println("target " + lEdge.target.id);
                        lWriter.println("]");
                    }
                }
            }
        }
        lWriter.println("]");
        logger.info("Exporting Edges of Nodes: "+lNodeCounter+"/"+pIncludedNodes.size());
        logger.info("Exported "+lExportedEdgeCounter+" edges of "+lEdgesRead+" potential edges");
        lWriter.close();
    }

    public static String getDecimalFormat(double pDouble) {
        return new DecimalFormat("#.########").format(pDouble).replace(",", ".");
    }

    public double[] getDiameterAndGeodesicDistanceUnDirected() throws Exception {
        List<TLGNode> lNodes = new ArrayList<>(getNodes());
        long lDistanceSum = 0;
        long lNodePairs = 0;
        long lTotal = (lNodes.size() * (lNodes.size()-1))/2;
        int lDiameter = 0;
        int lLastPerc = 0;
        for (int i=0; i<lNodes.size()-1; i++) {
            for (int k=i+1; k<lNodes.size(); k++) {
                TLGNode lStart = lNodes.get(i);
                TLGNode lEnd = lNodes.get(k);
                Set<TLGNode> lKnown = new HashSet<>();
                LinkedList<TLGNode> lQueue = new LinkedList<>();
                LinkedList<Integer> lDistQueue = new LinkedList<>();
                lKnown.add(lStart);
                lQueue.add(lStart);
                lDistQueue.add(0);
                int lDistance = 0;
                while (lQueue.size() > 0) {
                    TLGNode lNode = lQueue.remove(lQueue.size()-1);
                    int lDist = lDistQueue.remove(lDistQueue.size()-1);
                    if (lNode.equals(lEnd)) {
                        lDistance = lDist;
                        break;
                    }
                    for (TLGNode lOther:lNode.getLinkedNodes(Direction.ANY)) {
                        if (!lKnown.contains(lOther)) {
                            lKnown.add(lOther);
                            lQueue.add(0, lOther);
                            lDistQueue.add(0, lDist+1);
                        }
                    }
                }
                lDistanceSum += lDistance;
                lNodePairs++;
                if (lDistance > lDiameter) {
                    lDiameter = lDistance;
                }
                int lPerc = (int)Math.round((lNodePairs * 100)/(double)lTotal);
                if (lPerc != lLastPerc) {
                    lLastPerc = lPerc;
                    System.out.println(lPerc+"%, Diameter: "+lDiameter);
                }
            }
        }
        return new double[]{lDiameter, lDistanceSum/(double)lNodePairs};
    }

    public List<List<TLGNode>> computeEccentricity(TLGNode pStartNode, Set<String> pEdgeTypes) {
        List<TLGNode> lQueue = new ArrayList<>();
        List<Integer> lDistanceQueue = new ArrayList<>();
        Set<TLGNode> lKnown = new HashSet<>();
        Map<TLGNode, TLGNode> lPredecessorMap = new HashMap<>();
        lQueue.add(pStartNode);
        lDistanceQueue.add(0);
        lKnown.add(pStartNode);
        Set<TLGNode> lMaxSet = new HashSet<>();
        int lMaxDistance = 0;
        while (lQueue.size() > 0) {
            TLGNode lNode = lQueue.remove(lQueue.size()-1);
            int lDistance = lDistanceQueue.remove(lDistanceQueue.size()-1);
            if (lDistance > lMaxDistance) {
                lMaxDistance = lDistance;
                lMaxSet.clear();
                lMaxSet.add(lNode);
            }
            else if (lDistance == lMaxDistance) {
                lMaxSet.add(lNode);
            }
            for (TLGEdge lEdge:lNode.getEdges(Direction.OUT)) {
                if (pEdgeTypes.contains(lEdge.getProperty("Type", ""))) {
                    TLGNode lTarget = lEdge.getTarget();
                    if (!lKnown.contains(lTarget)) {
                        lQueue.add(0, lTarget);
                        lDistanceQueue.add(0, lDistance+1);
                        lKnown.add(lTarget);
                        lPredecessorMap.put(lTarget, lNode);
                    }
                }
            }
        }
        List<List<TLGNode>> lResult = new ArrayList<>();
        for (TLGNode lEndNode:lMaxSet) {
            List<TLGNode> lList = new ArrayList<>();
            lResult.add(lList);
            TLGNode lWanderer = lEndNode;
            lList.add(lWanderer);
            while (!lWanderer.equals(pStartNode)) {
                lWanderer = lPredecessorMap.get(lWanderer);
                lList.add(0, lWanderer);
            }
        }
        return lResult;
    }

    public double[] getDiameterAndGeodesicDistanceUnDirectedSampled(long pSamples) throws Exception {
        List<TLGNode> lNodes = new ArrayList<>(getNodes());
        Set<String> lRandomPairs = new HashSet<>();
        while (lRandomPairs.size() < pSamples) {
            int lStartIndex = (int)Math.floor(Math.random()*lNodes.size());
            int lEndIndex = (int)Math.floor(Math.random()*lNodes.size());
            if (lStartIndex != lEndIndex) {
                String lKey;
                if (lStartIndex < lEndIndex) {
                    lKey = lStartIndex+"\t"+lEndIndex;
                }
                else {
                    lKey = lEndIndex+"\t"+lStartIndex;
                }
                if (!lRandomPairs.contains(lKey)) {
                    lRandomPairs.add(lKey);
                }
            }
        }
        long lDistanceSum = 0;
        long lNodePairs = 0;
        long lTotal = lRandomPairs.size();
        int lDiameter = 0;
        int lLastPerc = 0;

        for (String lKey:lRandomPairs) {
            String[] lFields = lKey.split("\t", -1);
            TLGNode lStart = lNodes.get(Integer.parseInt(lFields[0]));
            TLGNode lEnd = lNodes.get(Integer.parseInt(lFields[1]));
            Set<TLGNode> lKnown = new HashSet<>();
            LinkedList<TLGNode> lQueue = new LinkedList<>();
            LinkedList<Integer> lDistQueue = new LinkedList<>();
            lKnown.add(lStart);
            lQueue.add(lStart);
            lDistQueue.add(0);
            int lDistance = 0;
            while (lQueue.size() > 0) {
                TLGNode lNode = lQueue.remove(lQueue.size()-1);
                int lDist = lDistQueue.remove(lDistQueue.size()-1);
                if (lNode.equals(lEnd)) {
                    lDistance = lDist;
                    break;
                }
                for (TLGNode lOther:lNode.getLinkedNodes(Direction.ANY)) {
                    if (!lKnown.contains(lOther)) {
                        lKnown.add(lOther);
                        lQueue.add(0, lOther);
                        lDistQueue.add(0, lDist+1);
                    }
                }
            }
            lDistanceSum += lDistance;
            lNodePairs++;
            if (lDistance > lDiameter) {
                lDiameter = lDistance;
            }
            int lPerc = (int)Math.round((lNodePairs * 100)/(double)lTotal);
            if (lPerc != lLastPerc) {
                lLastPerc = lPerc;
                System.out.println(lPerc+"%, Diameter: "+lDiameter);
            }
        }
        return new double[]{lDiameter, lDistanceSum/(double)lNodePairs};
    }

    public static String getSortedIdPair(TLGNode pNode1, TLGNode pNode2) {
        if (pNode1.id.compareTo(pNode2.id) <=0) {
            return pNode1.id+"\t"+pNode2.id;
        }
        else {
            return pNode2.id+"\t"+pNode1.id;
        }
    }

    public static String getSortedIdPair(String pNode1, String pNode2) {
        if (pNode1.compareTo(pNode2) <=0) {
            return pNode1+"\t"+pNode2;
        }
        else {
            return pNode2+"\t"+pNode1;
        }
    }

    public static TObjectIntHashMap<String> getUndirectedShortestPathsLengths(Set<TLGNode> pNodes) {
        TObjectIntHashMap<String> lResult = new TObjectIntHashMap<>();
        List<TLGNode> lNodes = new ArrayList<>(pNodes);
        for (int i=0; i<lNodes.size()-1; i++) {
            TLGNode lNode1 = lNodes.get(i);
            for (int k=i+1; k<lNodes.size(); k++) {
                TLGNode lNode2 = lNodes.get(k);
                if (!lResult.containsKey(getSortedIdPair(lNode1, lNode2))) {
                    List<TLGNode> lQueue = new ArrayList<>();
                    List<Integer> lDistanceQueue = new ArrayList<>();
                    Set<TLGNode> lKnown = new HashSet<>();
                    lQueue.add(lNode1);
                    lDistanceQueue.add(0);
                    lKnown.add(lNode1);
                    while (lQueue.size() > 0) {
                        TLGNode lNode = lQueue.remove(lQueue.size()-1);
                        int lDistance = lDistanceQueue.remove(lDistanceQueue.size()-1);
                        for (TLGNode lOther:lNode.getLinkedNodes(Direction.ANY)) {
                            if (pNodes.contains(lOther) && !lKnown.contains(lOther)) {
                                lQueue.add(0, lOther);
                                lDistanceQueue.add(0, lDistance+1);
                                lKnown.add(lOther);
                                lResult.put(getSortedIdPair(lNode1, lOther), lDistance+1);
                            }
                        }
                    }
                }
            }
        }
        return lResult;
    }

    public static double getFuzzyJaccardSimilarity(TLGGraph pGraph1, TLGGraph pGraph2) {
        TLGGraph lGraph1 = new TLGGraph(pGraph1);
        TLGGraph lGraph2 = new TLGGraph(pGraph2);
        // Collect Nodes unique to Graph1
        Set<TLGNode> lNodesUniqueTo1 = new HashSet<>();
        for (String lNode1ID:lGraph1.nodeMap.keySet()) {
            if (!lGraph2.nodeMap.containsKey(lNode1ID)) {
                lNodesUniqueTo1.add(lGraph1.nodeMap.get(lNode1ID));
            }
        }
        // Collect Nodes unique to Graph2
        Set<TLGNode> lNodesUniqueTo2 = new HashSet<>();
        for (String lNode2ID:lGraph2.nodeMap.keySet()) {
            if (!lGraph1.nodeMap.containsKey(lNode2ID)) {
                lNodesUniqueTo2.add(lGraph2.nodeMap.get(lNode2ID));
            }
        }
        // Add Nodes Unique in Graph2 to Graph1
        for (TLGNode lNode2:lNodesUniqueTo2) {
            lGraph1.createNode(lNode2);
        }
        // Add Nodes Unique in Graph1 to Graph2
        for (TLGNode lNode1:lNodesUniqueTo1) {
            lGraph2.createNode(lNode1);
        }
        // Components of Graph1
        List<Set<TLGNode>> lComponents1 = lGraph1.getWeaklyConnectedComponents();
        long lDelta1 = 0;
        TObjectIntHashMap<String> lGraph1ShortestPathLengths = new TObjectIntHashMap<>();
        for (Set<TLGNode> lSet:lComponents1) {
            TObjectIntHashMap<String> lMap = getUndirectedShortestPathsLengths(lSet);
            int lDiameter = 0;
            for (int i:lMap.values()) {
                if (i>lDiameter) lDiameter = i;
            }
            lGraph1ShortestPathLengths.putAll(lMap);
            lDelta1 += lDiameter + 1;
        }
        // Components of Graph2
        List<Set<TLGNode>> lComponents2 = lGraph2.getWeaklyConnectedComponents();
        long lDelta2 = 0;
        TObjectIntHashMap<String> lGraph2ShortestPathLengths = new TObjectIntHashMap<>();
        for (Set<TLGNode> lSet:lComponents2) {
            TObjectIntHashMap<String> lMap = getUndirectedShortestPathsLengths(lSet);
            int lDiameter = 0;
            for (int i:lMap.values()) {
                if (i>lDiameter) lDiameter = i;
            }
            lGraph2ShortestPathLengths.putAll(lMap);
            lDelta2 += lDiameter + 1;
        }
        // Compute Result
        Set<TLGNode> lAllNodes = new HashSet<>(lGraph1.getNodes());
        List<TLGNode> lNodeList = new ArrayList<>(lAllNodes);
        double lZaehler = 0;
        double lNenner = 0;
        for (int i=0; i<lNodeList.size()-1; i++) {
            TLGNode lNode1 = lNodeList.get(i);
            for (int k=i+1; k<lNodeList.size(); k++) {
                TLGNode lNode2 = lNodeList.get(k);
                String lKey = getSortedIdPair(lNode1, lNode2);
                double lDistance1 = lGraph1ShortestPathLengths.containsKey(lKey) ? 1d/lGraph1ShortestPathLengths.get(lKey) : 1d/lDelta1;
                double lDistance2 = lGraph2ShortestPathLengths.containsKey(lKey) ? 1d/lGraph2ShortestPathLengths.get(lKey) : 1d/lDelta2;
                lZaehler += Math.min(lDistance1, lDistance2);
                lNenner += Math.max(lDistance1, lDistance2);
            }
        }
        return lZaehler/lNenner;
    }

    public static double[][] getFuzzyJaccardSimilarities(List<TLGGraph> pGraphs, int pThreads) {
        double[][] lResult = new double[pGraphs.size()][pGraphs.size()];
        Set<Thread> lThreads = new HashSet<>();
        long lCounter = 0;
        long lMax = (pGraphs.size() * (pGraphs.size()-1))/2;
        for (int i=0; i<pGraphs.size()-1; i++) {
            TLGGraph lGraph1 = pGraphs.get(i);
            for (int k=i+1; k<pGraphs.size(); k++) {
                lCounter++;
                System.out.println(lCounter+"/"+lMax);
                TLGGraph lGraph2 = pGraphs.get(k);
                while (lThreads.size() >= pThreads) {
                    Iterator<Thread> m = lThreads.iterator();
                    while (m.hasNext()) {
                        Thread lThread = m.next();
                        if (lThread.getState().equals(Thread.State.TERMINATED)) {
                            m.remove();
                        }
                    }
                    if (lThreads.size() >= pThreads) {
                        try {
                            Thread.sleep(50);
                        }
                        catch (InterruptedException e) {
                        }
                    }
                }
                Thread lThread = new Thread(new FuzzyJaccardSimilarityTask(lGraph1, lGraph2, lResult, i, k));
                lThreads.add(lThread);
                lThread.start();
            }
        }
        while (lThreads.size() > 0) {
            Iterator<Thread> m = lThreads.iterator();
            while (m.hasNext()) {
                Thread lThread = m.next();
                if (lThread.getState().equals(Thread.State.TERMINATED)) {
                    m.remove();
                }
            }
            if (lThreads.size() >= pThreads) {
                try {
                    Thread.sleep(50);
                }
                catch (InterruptedException e) {
                }
            }
        }
        return lResult;
    }

    public static TLGGraph buildUnionGraph(Collection<TLGGraph> pGraphs, Directedness pDirectedness) {
        TLGGraph lResult = null;
        for (TLGGraph lGraph:pGraphs) {
            if (lResult == null) {
                lResult = new TLGGraph(lGraph);
            }
            else {
                // Add Nodes
                for (TLGNode lNode:lGraph.getNodes()) {
                    if (!lResult.nodeMap.containsKey(lNode.getId())) {
                        lResult.createNode(lNode);
                    }
                }
                // Add Edges
                for (TLGNode lCurrentNode:lGraph.getNodes()) {
                    TLGNode lResultNode = lResult.getNodeByID(lCurrentNode.getId());
                    Set<TLGNode> lResultLinkedNodes = lResultNode.getLinkedNodes(pDirectedness.equals(Directedness.UNDIRECTED) ? Direction.ANY : Direction.OUT);
                    for (TLGNode lOtherNode:lCurrentNode.getLinkedNodes(pDirectedness.equals(Directedness.UNDIRECTED) ? Direction.ANY : Direction.OUT)) {
                        if (!lResultLinkedNodes.contains(lOtherNode)) {
                            new TLGEdge(lResult, lResultNode, lResult.getNodeByID(lOtherNode.getId()));
                        }
                    }
                }
            }
        }
        return lResult;
    }

    public boolean isEqualDirected(TLGGraph pGraph) {
        for (TLGNode lNode1:getNodes()) {
            if (pGraph.getNodeByID(lNode1.getId()) == null) return false;
            TLGNode lNode2 = pGraph.getNodeByID(lNode1.getId());
            if (lNode1.getProperties().size() != lNode2.getProperties().size()) return false;
            for (Map.Entry<String, String> lEntry:lNode1.getProperties().entrySet()) {
                if (!lNode2.getProperties().containsKey(lEntry.getKey())) return false;
                if (!lEntry.getValue().equals(lNode2.getProperty(lEntry.getKey(), null))) return false;
            }
            if (lNode1.getEdges(TLGGraph.Direction.OUT).size() != lNode2.getEdges(TLGGraph.Direction.OUT).size()) return false;
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
                if (!lFoundMatchingEdge) return false;
            }
        }
        return true;
    }

    public static double getSphereBasedJaccardSimilarity(TLGGraph pGraph1, TLGGraph pGraph2, Directedness pDirectedness) throws TLGGraphException {
        Set<TLGNode> lAllNodes = new HashSet<>();
        lAllNodes.addAll(pGraph1.getNodes());
        lAllNodes.addAll(pGraph2.getNodes());
        double lSum = 0;
        for (TLGNode lNode1:pGraph1.getNodes()) {
            TLGNode lNode2 = pGraph2.getNodeByID(lNode1.getId());
            if (lNode2 != null) {
                lSum += lNode1.getSphereBasedNeighbourhoodSimilarity(lNode2, pDirectedness);
            }
        }
        return lSum/lAllNodes.size();
    }

    public static double[][] getGraphSimilarities(GraphSimilarityThread.GraphSimMethod pGraphSimMethod, List<TLGGraph> pGraphs, Directedness pDirectedness, int pMaxThreads) {
        double[][] lResult = new double[pGraphs.size()][pGraphs.size()];
        long lCounter = 0;
        long lMax = (pGraphs.size() * (pGraphs.size()-1))/2;
        Map<GraphSimilarityThread, int[]> lThreadMap = new HashMap<>();
        Object lMonitor = new Object();
        for (int i=0; i<pGraphs.size()-1; i++) {
            TLGGraph lGraph1 = pGraphs.get(i);
            for (int k=i+1; k<pGraphs.size(); k++) {
                lCounter++;
                logger.info(lCounter+"/"+lMax);
                TLGGraph lGraph2 = pGraphs.get(k);
                while (lThreadMap.size() >= pMaxThreads) {
                    try {
                        synchronized (lMonitor) {
                            lMonitor.wait(100l);
                        }
                    }
                    catch (InterruptedException e) {
                        logger.error(e.getMessage(), e);
                    }
                    Set<GraphSimilarityThread> lFinished = new HashSet<>();
                    for (Map.Entry<GraphSimilarityThread, int[]> lEntry:lThreadMap.entrySet()) {
                        if (lEntry.getKey().getState().equals(Thread.State.TERMINATED)) {
                            lFinished.add(lEntry.getKey());
                            lResult[lEntry.getValue()[0]][lEntry.getValue()[1]] = lEntry.getKey().getSimilarity();
                            lResult[lEntry.getValue()[1]][lEntry.getValue()[0]] = lEntry.getKey().getSimilarity();
                        }
                    }
                    for (GraphSimilarityThread lThread:lFinished) {
                        lThreadMap.remove(lThread);
                    }
                }
                GraphSimilarityThread lThread = null;
                switch (pGraphSimMethod) {
                    case sphere: {
                        lThread = new SphereBasedJaccardSimilarityThread(lMonitor, lGraph1, lGraph2, pDirectedness);
                        break;
                    }
                    case veo: {
                        lThread = new VEOSimilarityTask(lMonitor, lGraph1, lGraph2, pDirectedness);
                        break;
                    }
                }
                lThreadMap.put(lThread, new int[]{i,k});
                lThread.start();
            }
        }
        while (lThreadMap.size() > 0) {
            try {
                synchronized (lMonitor) {
                    lMonitor.wait(100l);
                }
            }
            catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
            }
            Set<GraphSimilarityThread> lFinished = new HashSet<>();
            for (Map.Entry<GraphSimilarityThread, int[]> lEntry:lThreadMap.entrySet()) {
                if (lEntry.getKey().getState().equals(Thread.State.TERMINATED)) {
                    lFinished.add(lEntry.getKey());
                    lResult[lEntry.getValue()[0]][lEntry.getValue()[1]] = lEntry.getKey().getSimilarity();
                    lResult[lEntry.getValue()[1]][lEntry.getValue()[0]] = lEntry.getKey().getSimilarity();
                }
            }
            for (GraphSimilarityThread lThread:lFinished) {
                lThreadMap.remove(lThread);
            }
        }
        return lResult;
    }

    public static double[][] getGraphSimilarities(List<TLGGraph> pGraphs, String pSingleNodeID, Directedness pDirectedness) throws TLGGraphException {
        double[][] lResult = new double[pGraphs.size()][pGraphs.size()];
        long lCounter = 0;
        long lMax = (pGraphs.size() * (pGraphs.size()-1))/2;
        for (int i=0; i<pGraphs.size()-1; i++) {
            TLGGraph lGraph1 = pGraphs.get(i);
            for (int k=i+1; k<pGraphs.size(); k++) {
                lCounter++;
                System.out.println(lCounter+"/"+lMax);
                TLGGraph lGraph2 = pGraphs.get(k);
                lResult[i][k] = lGraph1.getNodeByID(pSingleNodeID).getSphereBasedNeighbourhoodSimilarity(lGraph2.getNodeByID(pSingleNodeID), pDirectedness);
                lResult[k][i] = lResult[i][k];
            }
        }
        return lResult;
    }



    public static void main(String[] args) throws Exception {

    }
}
