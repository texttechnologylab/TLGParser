package org.hucompute.tlgparser;

public class FuzzyJaccardSimilarityTask implements Runnable {

    private TLGGraph graph1;
    private TLGGraph graph2;
    private double[][] similarityMatrix;
    private int i;
    private int k;

    public FuzzyJaccardSimilarityTask(TLGGraph graph1, TLGGraph graph2, double[][] similarityMatrix, int i, int k) {
        this.graph1 = graph1;
        this.graph2 = graph2;
        this.similarityMatrix = similarityMatrix;
        this.i = i;
        this.k = k;
    }

    public void run() {
        similarityMatrix[i][k] = TLGGraph.getFuzzyJaccardSimilarity(graph1, graph2);
        similarityMatrix[k][i] = similarityMatrix[i][k];
    }
}