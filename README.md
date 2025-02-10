# Concurrency and Parallelism in Java

Concurrency and parallelism are two notions that are often confusing Java developers.
They might be considered quite similar because both of them execute several tasks 
as their main unit of work, but the way that these tasks are handled is very 
different.

Concurrency uses multi-threading in order to start several execution units, 
called *threads*, that compete each other for the hardware resources, to run 
multiple tasks in a time-slicing manner. These tasks, performed through 
multi-threading, complete faster. Parallelism, on the other hand, consists in 
splitting-up tasks into sub-tasks and allocating them to threads running in 
parallel, across multiple CPU cores. Accordingly, the difference between concurrency
and parallelism consists essentially in the fact that, while with concurrency a
single thread might be active at a given time, parallelism means having at least
two threads running in parallel.

But beyond this most essential difference, there others very important ones, 
for example:

  - **Efficiency**: measured in latency (the amount of time required to complete a task) for parallelism and in throughput (the number of completed tasks) for concurrency.
  - **Resource allocation (CPU time, I/O, etc.)**: controlled by tasks in the case of the parallelism but not in the case of the concurrency, where threads cannot control resources allocation, but compete each other in order to gain them.
  - **Modus operandi**: In parallelism, threads operate on CPU cores in such a way that every core is busy. In concurrency, threads operate on tasks in such a way that, ideally, each thread executes a separate task.

In order to understand the differences between concurrency and parallelism, let's
look at a couple of examples. Let's suppose that we have 100 millions numbers 
that we need to filter such that to extract from them the list of odd numbers.

A very simple solution to this problem is implemented by the `SimpleFilter` class
that you may find in the [GitHub project](https://github.com/nicolasduminil/concurrency-and-parallelism-in-java):

    public void testSimpleFilter()
    {
      List<Integer> ints = IntStream.range(0, 100_000_000).boxed().toList();
      Instant start = Instant.now();
      List<Integer> result = ints.stream().filter(n -> n % 2 != 0).toList();
      Instant end = Instant.now();
      long duration = Duration.between(start, end).toMillis();
      System.out.println(">>> TestFilters.testSimpleFilter(): We've filtered %d numbers in %dms"
        .formatted(ints.size(), duration));
    }

This is a simple test that collects in a list all the odd numbers between 0 and
one hundred millions. Running this test I got the following output:

    Jan 18, 2025 5:04:37 PM fr.simplex_software.workshop.tests.TestFilters testSimpleFilter
    INFO: >>> We've filtered 100000000 numbers in 938ms

Depending on your hardware resources, you might see a different result.

Let's see now if we could perform this operation faster by using concurrency.

    @Test
    public void testConcurrentFilter()
    {
      List<Integer> ints = IntStream.range(0, 100_000_000).boxed().toList();
      Instant start = Instant.now();

      // Number of threads to use
      int nThreads = Runtime.getRuntime().availableProcessors();
      Thread[] threads = new Thread[nThreads];
      List<List<Integer>> results = new ArrayList<>(nThreads);

      // Initialize the results list with empty lists
      for (int i = 0; i < nThreads; i++)
        results.add(new ArrayList<>());

      // Split the list and create threads
      int chunkSize = ints.size() / nThreads;
      for (int i = 0; i < nThreads; i++)
      {
        final int threadIndex = i;
        final int fromIndex = i * chunkSize;
        final int toIndex = (i == nThreads - 1) ? ints.size() : (i + 1) * chunkSize;
        threads[i] = new Thread(() ->
        {
          List<Integer> threadResult = ints.subList(fromIndex, toIndex)
            .stream()
            .filter(n -> n % 2 != 0)
            .toList();
          results.set(threadIndex, threadResult);
        });
        threads[i].start();
      }

      // Wait for all threads to complete
      try
      {
        for (Thread thread : threads)
          thread.join();
      }
      catch (InterruptedException e)
      {
        Thread.currentThread().interrupt();
          throw new RuntimeException("Thread interrupted", e);
      }

      // Combine results
      List<Integer> rslt = new ArrayList<>();
      for (List<Integer> result : results)
        rslt.addAll(result);

      long duration = Duration.between(start, Instant.now()).toMillis();
      System.out.println(">>> ConcurrentFilter.main(): We've filtered %d numbers in %dms"
        .formatted(rslt.size(), duration));
    }

The code above is much more complex and it implements a different algorithm, as follows:

  - Splits the range of numbers in as many chunk as CPU cores.
  - Creates and starts a thread for each chunk.
  - Creates an array to hold the threads.
  - Creates a list to store partial results from each thread.
  - Waits for all threads to complete using join()
  - Combines the results from all threads

Running this test we'll get:

    Jan 18, 2025 5:04:37 PM fr.simplex_software.workshop.tests.TestFilters testSimpleFilter
    INFO: >>> We've filtered 50000000 numbers in 784ms

As you can see, we have already improved quite considerably our perfs. But we 
could do better by using parallel processing. Look at the following code:

    @Test
    public void testParallelFilter()
    {
      List<Integer> ints = IntStream.range(0, 100_000_000).boxed().toList();
      Instant start = Instant.now();
      List<Integer> result = ints.parallelStream().filter(n -> n % 2 != 0).toList();
      Instant end = Instant.now();
      long duration = Duration.between(start, end).toMillis();
      System.out.println(">>> TestFilters.testParallelFilter(): We've filtered %d numbers in %dms"
        .formatted(ints.size(), duration));
    }

It's almost identical to `testSimpleFilter` that we just have seen previously, 
except that it is using parallel streams. Running it I got:

    an 18, 2025 5:04:41 PM fr.simplex_software.workshop.tests.TestFilters testParallelFilter
    INFO: >>> We've filtered 100000000 numbers in 290ms

Wow, this is remarkable. So, I improved my perfs by reducing the execution time 
from 938ms initially to 290ms, i.e. almost 70%. And if you compare the concurrent 
implementation, which is quite complex, requiring more than 60 lines of code, with
the simplicity of the parallel one, then you understand quickly its advantages.

Now, talking about concurrency and threads, you certainly know that Java, since
its 21st release, supports two kind of threads:

  - Platform threads. These are thin wrappers around OS threads. They are expansive in terms of memory footprint and execution time. Moreover, the OS scheduler is responsible for managing them, which is a costly operation named *context switching*. Platform threads are available since Java 1.1.
  - Virtual threads.  These are lightweight entities having their own stack memory with a small footprint. They are cheap to create, block, and destroy. For example, creating a virtual thread is around 1,000 times cheaper than creating a platform thread. They are meant to sustain a massive throughput as applications might handle millions of them. They are available since Java 19, as a preview, and since Java 21 as a permanent feature.

This comparison between platform and virtual threads should make obvious the fact
that the number of the platform threads that an application might create and handle
is limited. 

Let’s talk about numbers. Knowing that a single Java thread needs 
20 MB, a machine, like my laptop, with 16 GB of memory has room for
around 800 Java threads. Let’s assume that these threads perform RESTful 
endpoints calls and that each one takes around 100 ms to complete, while the 
requests/responses preparation and processing takes around 0.001 ms. So a thread
works for 0.001 ms and then waits for 100 ms, meaning that it is idle during 
99.99% of the time. This also means that the 800 threads will use 0.8% of CPU.

Based on the numbers above, we can quickly conclude that platform threads represent
a major bottleneck in throughput that doesn’t allow us to take advantage of the
hardware full capacity. But talking about the threads number, how many of them 
could we create ? The simple code snippet of code below will help us to find out:

    AtomicLong osThreads = new AtomicLong();
    try
    {
      while (true)
      {
        Thread.ofPlatform().start(() ->
        {
          osThreads.incrementAndGet();
          LockSupport.park();
        });
      }
    } 
    catch (OutOfMemoryError ex)
    {
      System.out.println(">>> Max. number of possible OS threads: %d".formatted(osThreads.get()));
    }

Beware that running the code snippet above will crash your JVM despite catching
the `OutOfMemoryError`. For example, running it on my laptop I get this:

    >>> Max. number of possible OS threads: 37830

But it doesn't mean that you really would be able to handle such a huge number
of threads as, attending it, you're hitting `OutOfMemory`. Accordingly, a more 
realistic implementation will take into account the used memory and will stop 
creating threads once that it becomes superior to 80% of the total physical 
memory. Another condition to check is the CPU load and stop the process as soon
as it reaches 80%. Last but not least, instead of the dangerous `while (true)`,
we can imagine that, whatever happens, we could never manage more tha 1 million 
platform threads.

Checkout the `TestThreads` class in the GitHub project. Now, running it I got 
a much more reasonable number:

    WARNING: ### CPU threshold reached 1.000000
    Jan 18, 2025 3:13:04 PM fr.simplex_software.workshop.tests.TestThreads howManyPlatformThreads
    INFO: >>> Max number of OS threads created: 973

So I'd be able to manage only 973 threads without kneeling down my machine.

Okay, so far we have discussed the platform threads which, as we have seen, might
be a bottleneck preventing us to fully use the hardware resources. What about 
virtual threads ?

Virtual threads were introduced in JDK 19 as a preview (JEP 425), and they became
a final feature in JDK 21 (JEP 444). They run on top of platform threads, called *carrier* threads, in a 
one-to-many relationship, while the *carrier* threads run on top of OS threads in
a one-to-one relationship. They are stored in the JVM heap, instead of the OS stack,
so they take advantage of the Garbage Collector. They are scheduled by the JVM 
via the *work-stealing* `ForkJoinPool` scheduler which orchestrates them to run,
only one at a time, on *carrier* threads.

So, how many virtual threads can we create ? It's easy to adapt our previous test
by replacing:

    Thread thread = Thread.ofPlatform().unstarted(() -> ...

by:

    Thread thread = Thread.ofVirtual().unstarted(() -> ...

such that to create virtual threads instead of plarform ones. This time the 
output will show a much higher number:

    Jan 18, 2025 4:38:27 PM fr.simplex_software.workshop.tests.TestThreads howManyVirtualThreads
    WARNING: ### CPU threshold reached 1.000000
    Jan 18, 2025 4:38:27 PM fr.simplex_software.workshop.tests.TestThreads howManyVirtualThreads
    INFO: >>> Max number of virtual threads created: 49976

So, I've been able to create nearly 50 000 virtual threads without exhausting neither
the RAM, nor the CPU. In reality, depending on your hardware, the number of virtual threads that you could 
manage in you applications is much higher and it could rise to millions, if you 
can afford to relax the constraints that we applied or if you use thread pools and executors.

# Concurrency and Parallelism in Java (Part 2)

In a [previous post](http://www.simplex-software.fr/posts-archive/concurrency-and-parallelism/), we've been looking at a couple of interesting
aspects concerning the concurrency and parallelism in Java. We've seen that, with
parallel processing, the maximum number of parallel tasks that can be executed
at any given moment,
i.e. the maximum number of the simultaneously running platforms threads,
is equal to the number of the available CPU cores. If the number of the currently
active threads is superior to the one of the available CPU cores, then the number
of threads waiting for resources will be equal to the difference between the total
number of the active threads and the number of the available CPU cores.

In conclusion, more the difference between the number of the currently active 
threads and the one of the available CPU cores is important, more important will
be the number of the blocked threads, waiting for the CPU.

The Java common `ForkJoinPool`, which is responsible for threads allocation,
is configurable. Its parallelism level can be set via the system property 
`java.util.concurrent.ForkJoinPool.common.parallelism`. For example:

    System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "16");

sets the Java common `ForkJoinPool` maximum number of parallel threads to 16.

Now, an interesting question would be to know what's the relationship between 
this parallelism level and the one of the platform's available CPU cores ? The 
typical recommendation
would be to set the parallelism level to the number of cores, or slightly higher.
For example, for CPU-intensive tasks, set the parallelism level to the number
of cores, while for I/O intensive tasks, set it according to the formula below:

    parallelism = number of cores * waiting time / service time

Here `waiting time` is the time spent waiting for I/O operations (like network
calls, disk operations, etc.) and `service time ` is the actual CPU processing
time. For example, let's say that our task makes a database query which takes
100 ms, after which it processes the results during 20 ms. Then:

    paralallism = 8 * (1 + 100/20) = 48

The reasoning behind this formula is:

- During I/O operations, CPU cores are idle
- While one thread is waiting for I/O, other threads can use the CPU
- The ratio (waiting time / service time) helps determine how many additional threads can effectively use the CPU during I/O waits
- A higher waiting-to-service time ratio justifies more threads since cores would otherwise be idle during I/O waits

Let's look at an implementation trying to simulate such a computing model:

    public class TestForkJoinPool
    {
      private static final Logger LOG = Logger.getLogger(TestForkJoinPool.class.getName());

      @Test
      public void testForkJoinPool() throws Exception
      {
        final int CORES = Runtime.getRuntime().availableProcessors();
        final double WAITING_TIME = 100;
        final double SERVICE_TIME = 20;
        final OPTIMAL_PARALLELISM = (int) (CORES * (1 + WAITING_TIME / SERVICE_TIME));

        LOG.info(">>> Have set parallelism to %s for %d available CPU cores (waiting/service ratio: %.1f)"
         .formatted(OPTIMAL_PARALLELISM, CORES, WAITING_TIME / SERVICE_TIME));
        try (var pool = new ForkJoinPool(OPTIMAL_PARALLELISM))
        {
          pool.submit(() -> run()).get();
        }
      }

      private static void run()
      {
        Instant start = Instant.now();
        long count = Stream.generate(() ->
        {
          try
          {
            TimeUnit.MILLISECONDS.sleep(100);
            simulateCpuWork(20);
            return 1;
          } 
          catch (InterruptedException e)
          {
            throw new RuntimeException(e);
          }
        })
        .parallel()
        .limit(10)
        .count();
        LOG.info(">>> count: %d, duration: %d ms".formatted(count,
          Duration.between(start, Instant.now()).toMillis()));
      }

      private static void simulateCpuWork(long milliseconds)
      {
        long startTime = System.nanoTime();
        double result = 0;
        while (System.nanoTime() - startTime < milliseconds * 1_000_000)
        {
          result += Math.sin(result) + Math.cos(result);
        }
      }
    }

> **_NOTE:_**  In this example we've used a custom `ForkJoinPool` but the test
> result would have been the same for the common `ForkJoinPool`.

In the code above we're simulating 10 operations consisting each in a I/O 
intensive processing, taking 100 ms, and a CPU intensive one, taking 20 ms.

Running this test on my laptop having 8 available CPU cores, I get this:

    Feb 10, 2025 12:34:14 AM fr.simplex_software.workshop.tests.TestForkJoinPool testForkJoinPool
    INFO: >>> Have set parallelism to 48 for 8 available CPU cores (waiting/service ratio: 5.0)
    Feb 10, 2025 12:34:14 AM fr.simplex_software.workshop.tests.TestForkJoinPool run
    INFO: >>> Have performed 10 operations, duration: 255 ms

As you can see, it takes 255 ms to perform the 10 operations. In order to check 
the validity of the mentioned formula, I instantiated the common `ForkJoinPool` with a non-optimal
thread numbers, for example:

    ...
    try (var pool = new ForkJoinPool(1))
    ...

This time the total duration was of 1 445 ms, i.e. more than 5 times slower. 
Meaning that the optimal setting completes the work much faster. But what happens
if I set the parallelism level at a number of thread higher than the optimal one ?
For example, doing:

    ...
    try (var pool = new ForkJoinPool(64))
    ...

I was expecting to see degraded performances but, surprisingly, the test performs
almost as fast as when using the optimal value, or, in the worst case, just a little 
bit slower. 

In order to illustrate this let's modify the test as follows:

    @Test
    public void compareThreadCounts() throws Exception
    {
      int[] threadCounts = {
        1,
        CORES,
        OPTIMAL_PARALLELISM,
        OPTIMAL_PARALLELISM * 2,
        OPTIMAL_PARALLELISM * 4
      };

      for (int threadCount : threadCounts)
      {
        LOG.info(">>> Have set parallelism to %s for %d available CPU cores (waiting/service ratio: %.1f)"
          .formatted(threadCount, CORES, WAITING_TIME / SERVICE_TIME));
        Instant start = Instant.now();
        try (var pool = new ForkJoinPool(threadCount))
        {
          pool.submit(() -> run()).get();
        }
        Duration duration = Duration.between(start, Instant.now());
        LOG.info("Threads: %d, Duration: %d ms"
          .formatted(threadCount, duration.toMillis()));
      }
    }
    ...

Running this test on my machine I'm getting the following results:

| Nb. of threads | Duration |
|----------------|----------|
| 1              | 1443 ms  |
| 8              | 361 ms   |
| 48             | 253 ms   |
| 96             | 264 ms   |
| 192            | 279 ms   |

As you can see, the best result is obtained for the optimal parallelism level. 
When setting it at inferior values the test is significantly slower but when 
setting it at higher values, the test is just a bit slower.

In conclusion, this formula works but, before using it, you need to take into 
account the fact that it isn't a panacea. It's based on idealized assumptions 
about workload distribution
and real-world performance can vary due to many factors.

Accordingly, the following best practices have to be observed when applying it:

  1. Use the formula as a starting point, not a fixed rule.
  2. Benchmark with your specific workload.
  3. Monitor system resources (CPU, memory, etc.).
  4. Consider implementing adaptive thread pool sizing.
  5. Watch for signs of thread contention.

The optimal parallelism level is more of a minimum threshold for good performance
rather than a strict maximum. As long as you're not seeing degraded performance
or resource exhaustion, having more threads than the calculated optimal can be
perfectly fine.

If you're interested in these topics then you might like [![50 Shades of Java Executors](/assets/images/executors.jpg)](https://shorturl.at/ohTjM)

