# Concurrency and Parallelism in Java

Concurrency and parallelism are two notions that are often confusing Java developers.
They might be considered quite similar because both of them execute several tasks 
as their main unit of work, but the way that these tasks are handled id very 
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
  - **Resource allocation (CPU time, I/O, etc.)**: controlled by tasks in the case of the parallelism but not in the case of the concurrency, where threads cannot control resources allocation, but compete each other in order to gain it.
  - **Modus operandi**: In parallelism, threads operate on CPU cores in such a way that every core is busy. In concurrency, threads operate on tasks in such a way that, ideally, each thread executes a separate task.


