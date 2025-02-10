package fr.simplex_software.workshop.tests;

import org.junit.jupiter.api.*;

import java.time.*;
import java.util.concurrent.*;
import java.util.logging.*;
import java.util.stream.*;

public class TestForkJoinPool
{
  private static final Logger LOG = Logger.getLogger(TestForkJoinPool.class.getName());
  private static final int CORES = Runtime.getRuntime().availableProcessors();
  private static final double WAITING_TIME = 100;
  private static final double SERVICE_TIME = 20;
  private static final int OPTIMAL_PARALLELISM = (int) (CORES * (1 + WAITING_TIME / SERVICE_TIME));

  @Test
  public void testForkJoinPool() throws Exception
  {
    LOG.info(">>> Setting parallelism to %s for %d available CPU cores (waiting/service ratio: %.1f)"
      .formatted(OPTIMAL_PARALLELISM, CORES, WAITING_TIME / SERVICE_TIME));
    Instant start = Instant.now();
    try (ForkJoinPool pool = new ForkJoinPool(OPTIMAL_PARALLELISM))
    {
      pool.submit(() -> run()).get();
    }
    Duration duration = Duration.between(start, Instant.now());
    LOG.info("Threads: %d, Duration: %d ms"
      .formatted(OPTIMAL_PARALLELISM, duration.toMillis()));
  }

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
      LOG.info(">>> Setting parallelism to %s for %d available CPU cores (waiting/service ratio: %.1f)"
        .formatted(threadCount, CORES, WAITING_TIME / SERVICE_TIME));
      Instant start = Instant.now();
      try (ForkJoinPool pool = new ForkJoinPool(threadCount))
      {
        pool.submit(() -> run()).get();
      }
      Duration duration = Duration.between(start, Instant.now());
      LOG.info("Threads: %d, Duration: %d ms"
        .formatted(threadCount, duration.toMillis()));
    }
  }

  private static void run()
  {
    long count = Stream.generate(() ->
      {
        try
        {
          TimeUnit.MILLISECONDS.sleep(100);
          simulateCpuWork(20);
          return 1;
        } catch (InterruptedException e)
        {
          throw new RuntimeException(e);
        }
      })
      .parallel()
      .limit(10)
      .count();
  }

  private static void simulateCpuWork(long milliseconds)
  {
    long startTime = System.nanoTime();
    double result = 0;
    while (System.nanoTime() - startTime < milliseconds * 1_000_000)
      result += Math.sin(result) + Math.cos(result);
  }
}