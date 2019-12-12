import com.aparapi.Kernel
import java.util.Arrays
import java.util.stream.IntStream
import com.aparapi.device.Device
import com.aparapi.internal.kernel.KernelManager
import com.aparapi.internal.kernel.KernelPreferences



fun main() {
    gpu()
}

fun deviceInfo() {
    val preferences = KernelManager.instance().defaultPreferences
    println("-- Devices in preferred order --")
    for (device in preferences.getPreferredDevices(null)) {
        println("----------")
        println(device)
    }
}

fun gpu() {
    val size = 100000
    val a = IntStream.range(2, size + 2).toArray()
    val primeNumbers = BooleanArray(size)

    val kernel = object : Kernel() {
        override fun run() {
            val gid = globalId
            val num = a[gid]
            var prime = true
            for (i in 2 until num) {
                if (num % i == 0) {
                    prime = false
                    //break is not supported
                }
            }
            primeNumbers[gid] = prime
        }
    }
    val startTime = System.currentTimeMillis()
    kernel.execute(size)
    System.out.printf("time taken: %s ms%n", System.currentTimeMillis() - startTime)
    println(Arrays.toString(primeNumbers.copyOf(20)))//just print a sub array
    kernel.dispose()
}

fun cpuSingle() {
    val size = 100000
    val a = IntStream.range(2, size + 2).toArray()
    val primeNumbers = BooleanArray(size)

    val startTime = System.currentTimeMillis()
    for (n in 0 until size) {
        val num = a[n]
        var prime = true
        for (i in 2 until num) {
            if (num % i == 0) {
                prime = false
                //not using break for a fair comparision
            }
        }
        primeNumbers[n] = prime
    }
    System.out.printf("time taken: %s ms%n", System.currentTimeMillis() - startTime)
    println(Arrays.toString(primeNumbers.copyOf(20)))//just print a sub array
}

fun cpuParallel() {
    val size = 100000
    val a = IntStream.range(2, size + 2).toArray()

    val startTime = System.currentTimeMillis()
    val primeNumbers = Arrays.stream(a)
        .parallel()
        .mapToObj { isPrime(it) }
        .toArray()
    System.out.printf("time taken: %s ms%n", System.currentTimeMillis() - startTime)
    println(Arrays.toString(primeNumbers.copyOf<Any>(20)))//just print a sub array
}

fun isPrime(num: Int): Boolean {
    var prime = true
    for (i in 2 until num) {
        if (num % i == 0) {
            prime = false
            //not using break for a fair comparision
        }
    }
    return prime
}