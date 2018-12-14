package ctlab.mcmc;

import ctlab.bn.action.Multinomial;

import java.util.*;

public class Distribution {
    private List<GeneDistribution> distributions;

    public Distribution(int n, int nCachedStates) {
        distributions = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            distributions.add(new GeneDistribution(nCachedStates));
        }
    }

    public Multinomial request(int v, List<Integer> ps) {
        return distributions.get(v).request(ps);
    }

    private static class GeneDistribution {
        private Map<List<Integer>, Multinomial> cacheMap;

        public GeneDistribution(int nCachedStates) {
            cacheMap = new HashMap<>();
        }

        public Multinomial request(List<Integer> ps) {
            Multinomial mult = cacheMap.get(ps);
            if (mult == null) {
                mult = new Multinomial();
            }
        }
    }
}
