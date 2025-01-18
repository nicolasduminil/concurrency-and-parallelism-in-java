package fr.simplex_software.workshop.tests;

import org.junit.jupiter.api.*;

import java.lang.management.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;
import java.util.logging.*;

public class TestThreads
{
  private static final Logger LOG = Logger.getLogger(TestThreads.class.getName());
  private static final long MAX_THREADS = 100_000;

  @Test
  public void howManyPlatformThreads()
  {
    AtomicLong osThreads = new AtomicLong();
    List<Thread> threads = new ArrayList<>();
    Runtime runtime = Runtime.getRuntime();
    try
    {
      while (osThreads.get() < MAX_THREADS)
      {
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        if (usedMemory > (maxMemory * 0.8))
        {
          LOG.warning("### Memory threshold reached at %d bytes".formatted(usedMemory));
          break;
        }

        // Add CPU load check
        double systemCpuLoad = getSystemCpuLoad();
        if (systemCpuLoad > 0.8)
        {
          LOG.warning("### CPU threshold reached at %f".formatted(systemCpuLoad));
          break;
        }

        Thread thread = Thread.ofPlatform().unstarted(() ->
        {
          osThreads.incrementAndGet();
          LockSupport.park();
        });
        threads.add(thread);
        thread.start();

        Thread.sleep(1);
      }
    }
    catch (InterruptedException e)
    {
      Thread.currentThread().interrupt();
      LOG.warning("### Thread creation interrupted");
    }
    finally
    {
      cleanupThreads(threads);
    }

    LOG.info(">>> Max number of OS threads created: %d"
      .formatted(osThreads.get()));
  }

  @Test
  public void howManyVirtualThreads()
  {
    AtomicLong vThreads = new AtomicLong();
    List<Thread> threads = new ArrayList<>();
    Runtime runtime = Runtime.getRuntime();

    try
    {
      while (vThreads.get() < MAX_THREADS)
      {
        long usedMemory = runtime.totalMemory() - runtime.freeMemory();
        long maxMemory = runtime.maxMemory();

        if (usedMemory > (maxMemory * 0.8))
        {
          LOG.warning("### Memory threshold reached at %d bytes"
            .formatted(usedMemory));
          break;
        }

        // Add CPU load check
        double systemCpuLoad = getSystemCpuLoad();
        if (systemCpuLoad > 0.8)
        {
          LOG.warning("### CPU threshold reached %f".formatted(systemCpuLoad));
          break;
        }

        Thread thread = Thread.ofVirtual().unstarted(() ->
        {
          vThreads.incrementAndGet();
          LockSupport.park();
        });
        threads.add(thread);
        thread.start();

        Thread.sleep(1);
      }
    }
    catch (InterruptedException e)
    {
      Thread.currentThread().interrupt();
      LOG.warning("### Thread creation interrupted");
    }
    finally
    {
      cleanupThreads(threads);
    }

    LOG.info(">>> Max number of virtual threads created: %d"
      .formatted(vThreads.get()));
  }

  private void cleanupThreads(List<Thread> threads)
  {
    threads.forEach(LockSupport::unpark);
    threads.forEach(Thread::interrupt);

    for (Thread thread : threads)
    {
      try
      {
        thread.join(100);
      } catch (InterruptedException e)
      {
        Thread.currentThread().interrupt();
        LOG.warning("### Cleanup interrupted");
        break;
      }
    }
  }

  private double getSystemCpuLoad()
  {
    return ((com.sun.management.OperatingSystemMXBean) ManagementFactory
      .getOperatingSystemMXBean()).getCpuLoad();
  }
}
