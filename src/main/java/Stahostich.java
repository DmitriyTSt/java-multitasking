import com.aparapi.Kernel;

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.IntStream;

public class Stahostich {

    public static void main (String[] args) {
        new Stahostich().run();
    }

    int gcd (int a, int b) {
        return b != 0 ? gcd (b, a % b) : a;
    }

    private void run() {
        int n = 4000;
        final int[][] a = new int[n][n];
        final int[][] o = new int[n][n];
        final int[][] xstep = new int[n][n];
        final int[][] xx = new int[n][n];
        final double[] s = new double[n];

        final int[] i = IntStream.range(0, n).toArray();
        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int gid = getGlobalId();
                int num = i[gid];
                int aidx = 0;
                int oidx = 0;
                int xstepIdx = 0;
                int xxIdx = 0;
                if (num % 2 == 1) {
                    for (int j = 1; j < num; j++) {
                        if (gcd(j, num) == 1) {
                            a[gid][aidx++] = j;
                        }
                    }
                    int m = a[gid].length;
                    int x = a[gid][0];
                    do {
                        o[gid][oidx++] = x;
                        x = (2 * x) % num;
                    } while (x != a[gid][0]);
                    int t = o[gid].length;
                    int originOidx = 0;
                    for (int step = 0; step < num && originOidx < t; step = (step + 1) % num) {
                        if (o[gid][originOidx] == a[gid][step]) {
                            xstep[gid][originOidx++] = step;
                        }
                    }
                    Arrays.sort(xstep[gid]);
                    for (int j = 0; j < t; j++) {
                        xx[gid][xxIdx++] = (xstep[gid][j] - xstep[gid][(j-1 + 100 * t) % t] + 100 * m) % m;
                    }
                    int R = 0;
                    for (int j = 0; j < t; j++) {
                        R += xx[gid][j] * xx[gid][j];
                    }
                    double r = R * 1.0 / m / m;
                    s[gid] = r * t;
                }
            }
        };
        long startTime = System.currentTimeMillis();
        kernel.execute(n);
        kernel.dispose();
        System.out.printf("time taken: %s ms%n", System.currentTimeMillis() - startTime);
        for (int j = 0; j < n; j++) {
            if (Math.abs(2.0 - s[j]) < 0.01) {
                System.out.println(s[j]);
            }
        }
    }
}
