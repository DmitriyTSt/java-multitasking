import com.aparapi.Kernel;

import java.util.*;
import java.util.stream.IntStream;

public class GraphViewFromNewtonFunction {

    public static void main(String[] args) {
        new GraphViewFromNewtonFunction().run();
    }

    private void run() {
        int n = 3;
        int lasString = 1;
        for (int i = 0; i < n; i++) {
            lasString *= 2;
        }
        ArrayList<Integer> from = new ArrayList<>();
        ArrayList<Integer> to = new ArrayList<>();
        for (int i = 0; i < lasString; i++) {
            ArrayList<Integer> binary = toBinary(i, n);
            from.add(getCode(binary));
            to.add(getCode(newton(binary)));
        }
        ArrayList<Integer> graph = getGraph(from, to);

        processGraph(graph);
    }

    private static void gpu() {
        final int size = 100000;
        final int[] a = IntStream.range(2, size + 2).toArray();
        final boolean[] primeNumbers = new boolean[size];

        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int gid = getGlobalId();
                int num = a[gid];
                boolean prime = true;
                for (int i = 2; i < num; i++) {
                    if (num % i == 0) {
                        prime = false;
                        //break is not supported
                    }
                }
                primeNumbers[gid] = prime;
            }
        };
        long startTime = System.currentTimeMillis();
        kernel.execute(size);
        System.out.printf("time taken: %s ms%n", System.currentTimeMillis() - startTime);
        System.out.println(Arrays.toString(Arrays.copyOf(primeNumbers, 20)));//just print a sub array
        kernel.dispose();
    }

    private ArrayList<Integer> newton(ArrayList<Integer> a) {
        ArrayList<Integer> ans = new ArrayList<>();
        for (int i = 0; i < a.size() - 1; i++) {
            ans.add((a.get(i + 1) + a.get(i)) % 2);
        }
        ans.add((a.get(0) + a.get(a.size() - 1)) % 2);
        return ans;
    }

    private ArrayList<Integer> toBinary(int x, int n) {
        ArrayList<Integer> ans = new ArrayList<>();
        while (x > 1) {
            ans.add(x % 2);
            x /= 2;
        }
        ans.add(x);
        while (ans.size() < n) {
            ans.add(0);
        }
        Collections.reverse(ans);
        return ans;
    }

    int getCode(ArrayList<Integer> a) {
        int ans = 0;
        int st = 1;
        for (int i = a.size() - 1; i >= 0; i--) {
            ans += st * a.get(i);
            st *= 2;
        }
        return ans;
    }

    ArrayList<Integer> getGraph(ArrayList<Integer> a, ArrayList<Integer> t) {
        int n = a.size();
        ArrayList<Integer> ans1 = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ans1.add(0);
        }
        for (int i = 0; i < n; i++) {
            ans1.set(a.get(i), t.get(i));
        }
        return ans1;
    }

    void processGraph(ArrayList<Integer> graph) {
        final int n = graph.size();
        final int[] a = IntStream.range(0, n).toArray();
        final ArrayList<Integer>[] o = new ArrayList[n];
        long startTime = System.currentTimeMillis();
        Kernel kernel = new Kernel() {
            @Override
            public void run() {
                int gid = getGlobalId();
                int i = a[gid];
                boolean[] used = new boolean[n];
                for (int j = 0; j < n; j++) {
                    used[j] = false;
                }
                ArrayList<Integer> p = new ArrayList<>();
                int current = i;
                used[current] = true;
                while (!used[graph.get(current)]) {
                    p.add(current);
                    current = graph.get(current);
                    used[current] = true;
                }
                ArrayList<Integer> circle = new ArrayList<>();
                if (current == i) {
                    p.add(i);
                    o[i] = circle;
                } else {
                    circle.add(current);
                    int cSearch = graph.get(current);
                    int pit = p.size() - 1;
                    while (p.get(pit) != cSearch) {
                        current = p.get(pit);
                        pit--;
                        circle.add(current);
                    }
                    current = p.get(pit);
                    circle.add(current);
                    o[i] = circle;
                }
            }
        };
        kernel.execute(n);
        kernel.dispose();
        long circlesFoundTime = System.currentTimeMillis();
        System.out.printf("time circle found: %s ms%n", circlesFoundTime - startTime);
        ArrayList<ArrayList<Integer>> circles = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            if (!circles.contains(o[i])) {
                circles.add(o[i]);
            }
        }
        long circlesFormatTime = System.currentTimeMillis();
        System.out.printf("time circle formated: %s ms%n", circlesFormatTime - circlesFoundTime);
        final int[] vCircleIdx = new int[n];
        for (int i = 0; i < n; i++) {
            vCircleIdx[i] = -1;
        }
        kernel = new Kernel() {
            @Override
            public void run() {
                int gid = getGlobalId();
                int i = a[gid];
                if (getCircleIdx(circles, i) == -1) {
                    int current = i;
                    while (getCircleIdx(circles, graph.get(current)) == -1) {
                        current = graph.get(current);
                    }
                    vCircleIdx[i] = getCircleIdx(circles, graph.get(current));
                }
            }
        };
        kernel.execute(n);
        kernel.dispose();
        long vertexCircleIdFoundTime = System.currentTimeMillis();
        System.out.printf("time vertex circle ids: %s ms%n", vertexCircleIdFoundTime - circlesFormatTime);
        for (int i = 0; i < circles.size(); i++) {
            if (i > 0) {
                System.out.print(" + ");
            }
            int vertexes = 0;
            for (int j = 0; j < vCircleIdx.length; j++) {
                if (vCircleIdx[j] == i) {
                    vertexes++;
                }
            }
            System.out.print(String.format(Locale.getDefault(), "(O%d * T%d)", circles.get(i).size(), (vertexes / circles.get(i).size()) + 1));
        }
        long allTime = System.currentTimeMillis();
        System.out.printf("all time spent: %s ms%n", allTime - startTime);
    }

    int getCircleIdx(ArrayList<ArrayList<Integer>> o, int v) {
        for (int i = 0; i < o.size(); i++) {
            for (Integer vc : o.get(i)) {
                if (v == vc) {
                    return i;
                }
            }
        }
        return -1;
    }


}
