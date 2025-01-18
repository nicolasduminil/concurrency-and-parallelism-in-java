package fr.simplex_software.workshop.tests;

import java.time.*;
import java.util.*;
import java.util.logging.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestFilters
{
  private static final Logger LOG = Logger.getLogger(TestFilters.class.getName());

  @Test
  @Order(10)
  public void testSimpleFilter()
  {
    List<Integer> ints = IntStream.range(0, 100_000_000).boxed().toList();
    Instant start = Instant.now();
    List<Integer> result = ints.stream().filter(n -> n % 2 != 0).toList();
    Instant end = Instant.now();
    long duration = Duration.between(start, end).toMillis();
    LOG.info(">>> We've filtered %d numbers in %dms"
      .formatted(ints.size(), duration));
  }

  @Test
  @Order(20)
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
    LOG.info(">>> We've filtered %d numbers in %dms"
      .formatted(rslt.size(), duration));
  }

  @Test
  public void testParallelFilter()
  {
    List<Integer> ints = IntStream.range(0, 100_000_000).boxed().toList();
    Instant start = Instant.now();
    List<Integer> result = ints.parallelStream().filter(n -> n % 2 != 0).toList();
    Instant end = Instant.now();
    long duration = Duration.between(start, end).toMillis();
    LOG.info(">>> We've filtered %d numbers in %dms"
      .formatted(ints.size(), duration));
  }
}
