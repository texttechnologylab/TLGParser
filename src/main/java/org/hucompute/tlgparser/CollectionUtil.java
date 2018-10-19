package org.hucompute.tlgparser;

import java.util.HashSet;
import java.util.Set;

public class CollectionUtil {

    public static <G> Set<G> getIntersection(Set<G> g1, Set<G> g2) {
        Set<G> lResult = new HashSet<>();
        for (G g:g1) {
            if (g2.contains(g)) {
                lResult.add(g);
            }
        }
        return lResult;
    }

    public static <G> Set<G> getJoin(Set<G> g1, Set<G> g2) {
        Set<G> lResult = new HashSet<>(g1);
        lResult.addAll(g2);
        return lResult;
    }

}
