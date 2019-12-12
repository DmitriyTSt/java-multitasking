import com.aparapi.Kernel;
import com.aparapi.device.Device;
import com.aparapi.internal.kernel.KernelManager;
import com.aparapi.internal.kernel.KernelPreferences;

import java.util.Arrays;
import java.util.stream.IntStream;

public class Main {

    public static void main(String[] args) {
        gpu(500000000);
    }

    private static void deviceInfo() {
        KernelPreferences preferences = KernelManager.instance().getDefaultPreferences();
        System.out.println("-- Devices in preferred order --");
        for (Device device : preferences.getPreferredDevices(null)) {
            System.out.println("----------");
            System.out.println(device);
        }
    }

    private static void gpu(int size) {
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

    public static void parallelCpu(int size) {
        final int[] a = IntStream.range(2, size + 2).toArray();

        long startTime = System.currentTimeMillis();
        Object[] primeNumbers = Arrays.stream(a)
                .parallel()
                .mapToObj(Main::isPrime)
                .toArray();
        System.out.printf("time taken: %s ms%n", System.currentTimeMillis() - startTime);
        System.out.println(Arrays.toString(Arrays.copyOf(primeNumbers, 20)));//just print a sub array
    }

    private static boolean isPrime(int num) {
        boolean prime = true;
        for (int i = 2; i < num; i++) {
            if (num % i == 0) {
                prime = false;
                //not using break for a fair comparision
            }
        }
        return prime;
    }
}
