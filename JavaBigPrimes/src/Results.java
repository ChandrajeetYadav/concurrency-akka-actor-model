import java.math.BigInteger;
import java.util.SortedSet;
import java.util.TreeSet;

public class Results {
    private SortedSet<BigInteger> primes;

    public Results() {
        primes = new TreeSet<>();
    }

    public int getSize() {
        synchronized (this) {
            return primes.size();
        }
    }

    public void addPrime(BigInteger bigInteger) {
        synchronized (this) {
            primes.add(bigInteger);
        }
    }

    public void print() {
        synchronized (this) {
            primes.forEach(System.out::println);
        }
    }
}
