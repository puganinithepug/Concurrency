import java.util.concurrent.*;
import java.util.Arrays;
import java.util.List;

public class q2 {
    private static int n, t;
    
    public static ExecutorService threadPool;

    public static void main(String[] args) throws Exception {
        // get input arguments
        n = Integer.parseInt(args[0]); // string length
        t = Integer.parseInt(args[1]); // number of threads
        int seed = (args.length > 2) ? Integer.parseInt(args[2]) : (int) System.currentTimeMillis();

        // generate input bracket sequence
        char[] brackets = Bracket.construct(n, seed);
        
        threadPool = Executors.newFixedThreadPool(t);
        
        long startTime = System.nanoTime();
        
        // compute
        int[] result;
        try {
            Future<int[]> futureResult = threadPool.submit(new BracketTask(brackets, 0, n));
            result = futureResult.get(); // wait for the task to finish
        } finally {
            threadPool.shutdown(); // proper shutdown
        }

        // stop timing
        long endTime = System.nanoTime();

        // output
        System.out.println((endTime - startTime) / 1_000_000 + " ms"); // execution time
        System.out.println((result[0] == 1) + " " + Bracket.verify()); // correctness check
    }

    public static class BracketTask implements Callable<int[]> {
        private final char[] str;
        private final int start, end;

        public BracketTask(char[] str, int start, int end) {
            this.str = str;
            this.start = start;
            this.end = end;
        }

        @Override
        public int[] call() {
            // Skip threading if only one thread is used
            if (t==1 || (end - start) <= Math.max(n / t, 1000)) {
                return computeDirectly();}
            
                int mid = (start + end) / 2;
	try {
	    Future<int[]> leftFuture = threadPool.submit(new BracketTask(str, start, mid));
	    int[] left = leftFuture.get();  // left result
	    Future<int[]> rightFuture = threadPool.submit(new BracketTask(str, mid, end));


	    int[] right = rightFuture.get(); // right result

	    return mergeResults(left, right);
	} catch (Exception e) {
	    e.printStackTrace();
	    return new int[]{0, 0, 0}; // error case
	}
     
       }

        private int[] computeDirectly() {
            int f = 0, m = 0;
            for (int i = start; i < end; i++) {
                if (str[i] == '[') f++;
                else if (str[i] == ']') f--;
                m = Math.min(m, f);
            }
            return new int[]{(f == 0 && m >= 0) ? 1 : 0, f, m};
        }

       private int[] mergeResults(int[] left, int[] right) {
	    int b1 = left[0], f1 = left[1], m1 = left[2];
	    int b2 = right[0], f2 = right[1], m2 = right[2];

	    int f = f1 + f2;
	    int m = Math.min(m1, f1 + m2);

	    int b = (b1 != 0 && b2 != 0) || ((f == 0) && (m1 >= 0) && (f1 + m2 >= 0)) ? 1 : 0;

	    return new int[]{b, f, m};
	}

    }
}
