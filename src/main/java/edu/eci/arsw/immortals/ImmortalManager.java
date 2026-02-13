package edu.eci.arsw.immortals;

import edu.eci.arsw.concurrency.PauseController;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class ImmortalManager implements AutoCloseable {
  private final List<Immortal> population = new ArrayList<>();
  private final List<Future<?>> futures = new ArrayList<>();
  private final PauseController controller = new PauseController();
  private final ScoreBoard scoreBoard = new ScoreBoard();
  private ExecutorService exec;

  private final String fightMode;
  private final int initialHealth;
  private final int damage;

  public ImmortalManager(int n, String fightMode) {
    this(n, fightMode, Integer.getInteger("health", 100), Integer.getInteger("damage", 10));
  }

  public ImmortalManager(int n, String fightMode, int initialHealth, int damage) {
    this.fightMode = fightMode;
    this.initialHealth = initialHealth;
    this.damage = damage;
    for (int i=0;i<n;i++) {
      population.add(new Immortal("Immortal-"+i, initialHealth, damage, population, scoreBoard, controller));
    }
  }

  public synchronized void start() {
    if (exec != null) stop();
    controller.setTotalThreads(population.size());
    exec = Executors.newVirtualThreadPerTaskExecutor();
    for (Immortal im : population) {
      futures.add(exec.submit(im));
    }
  }

  public void pause() { controller.pause(); }
  public void resume() { controller.resume(); }
  
  public void stop() {
    // 1. Señalizar a todos los inmortales que deben detenerse
    for (Immortal im : population) {
      im.stop();
    }

    // 2. Reanudar si están pausados para que puedan terminar
    controller.resume();
    
    // 3. Cerrar executor y esperar terminación ordenada
    if (exec != null) {
      exec.shutdown();
      try {
        if (!exec.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
          exec.shutdownNow();
        }
      } catch (InterruptedException e) {
        exec.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }

  public int aliveCount() {
    int c = 0;
    for (Immortal im : population) if (im.isAlive()) c++;
    return c;
  }

  public long totalHealth() {
    long sum = 0;
    for (Immortal im : population) sum += im.getHealth();
    return sum;
  }

  public long expectedHealth() {
    return (long) population.size() * initialHealth - (scoreBoard.totalFights() * (damage / 2));
  }

  public boolean checkInvariant() {
    return totalHealth() == expectedHealth();
  }

  public List<Immortal> populationSnapshot() {
    return Collections.unmodifiableList(new ArrayList<>(population));
  }

  public ScoreBoard scoreBoard() { return scoreBoard; }
  public PauseController controller() { return controller; }

  @Override public void close() { stop(); }
}
