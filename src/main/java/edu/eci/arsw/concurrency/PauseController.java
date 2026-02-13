package edu.eci.arsw.concurrency;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public final class PauseController {
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition unpaused = lock.newCondition();
  private final Condition allPaused = lock.newCondition();
  private volatile boolean paused = false;
  private int pausedThreads = 0;
  private int totalThreads = 0;

  public void setTotalThreads(int total) {
    lock.lock();
    try {
      this.totalThreads = total;
    } finally {
      lock.unlock();
    }
  }

  public void threadFinished() {
    lock.lock();
    try {
      totalThreads--;
      if (paused && pausedThreads >= totalThreads) {
        allPaused.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  public void pause() { 
    lock.lock(); 
    try { 
      paused = true; 
    } finally { 
      lock.unlock(); 
    } 
  }

  public void resume() { 
    lock.lock(); 
    try { 
      paused = false; 
      pausedThreads = 0;
      unpaused.signalAll(); 
    } finally { 
      lock.unlock(); 
    } 
  }

  public boolean paused() { return paused; }

  public void awaitIfPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try { 
      if (paused) {
        pausedThreads++;
        if (pausedThreads == totalThreads) {
          allPaused.signalAll();
        }
        while (paused) {
          unpaused.await();
        }
        // pausedThreads se resetea en resume()
      }
    } finally { 
      lock.unlock(); 
    }
  }

  public void waitUntilAllPaused() throws InterruptedException {
    lock.lockInterruptibly();
    try {
      while (paused && pausedThreads < totalThreads) {
        allPaused.await();
      }
    } finally {
      lock.unlock();
    }
  }
}

