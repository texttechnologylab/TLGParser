package org.hucompute.tlgparser;

public class MathUtil {

    public static long factorial(long n) {
        long lResult = 1;
        for (long m=1; m<=n; m++) {
            lResult *= m;
        }
        return lResult;
    }

    public static double binomialCoefficient(long n, long k) {
        if (n >= k) {
            return (double) (factorial(n)) / (factorial(k) * factorial(n - k));
        }
        else {
            double lResult = 1;
            for (long j=1; j<=k; j++) {
                lResult *= (n+1-j)/(double)j;
            }
            return lResult;
        }
    }

}
