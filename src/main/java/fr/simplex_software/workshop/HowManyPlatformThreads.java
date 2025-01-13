package fr.simplex_software.workshop;

import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

public class HowManyPlatformThreads
{
  public static void main(String... args)
  {
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
    } catch (OutOfMemoryError ex)
    {
      System.out.println(">>> Max. number of possible OS threads: %d".formatted(osThreads.get()));
      System.exit(0);
    }
  }
}
