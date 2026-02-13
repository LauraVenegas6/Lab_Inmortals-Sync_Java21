package edu.eci.arsw.highlandersim;

import edu.eci.arsw.immortals.Immortal;
import edu.eci.arsw.immortals.ImmortalManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public final class ControlFrame extends JFrame {

  private ImmortalManager manager;
  private final JTextArea output = new JTextArea(14, 40);
  private final JButton startBtn = new JButton("Start");
  private final JButton pauseAndCheckBtn = new JButton("Pause & Check");
  private final JButton resumeBtn = new JButton("Resume");
  private final JButton stopBtn = new JButton("Stop");

  private final JSpinner countSpinner = new JSpinner(new SpinnerNumberModel(8, 2, 5000, 1));
  private final JSpinner healthSpinner = new JSpinner(new SpinnerNumberModel(100, 10, 10000, 10));
  private final JSpinner damageSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 1000, 1));
  private final JComboBox<String> fightMode = new JComboBox<>(new String[]{"ordered", "naive"});

  public ControlFrame(int count, String fight) {
    setTitle("Highlander Simulator — ARSW");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setLayout(new BorderLayout(8,8));

    JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
    top.add(new JLabel("Count:"));
    countSpinner.setValue(count);
    top.add(countSpinner);
    top.add(new JLabel("Health:"));
    top.add(healthSpinner);
    top.add(new JLabel("Damage:"));
    top.add(damageSpinner);
    top.add(new JLabel("Fight:"));
    fightMode.setSelectedItem(fight);
    top.add(fightMode);
    add(top, BorderLayout.NORTH);

    output.setEditable(false);
    output.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    add(new JScrollPane(output), BorderLayout.CENTER);

    JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER));
    bottom.add(startBtn);
    bottom.add(pauseAndCheckBtn);
    bottom.add(resumeBtn);
    bottom.add(stopBtn);
    add(bottom, BorderLayout.SOUTH);

    startBtn.addActionListener(this::onStart);
    pauseAndCheckBtn.addActionListener(this::onPauseAndCheck);
    resumeBtn.addActionListener(this::onResume);
    stopBtn.addActionListener(this::onStop);

    pack();
    setLocationByPlatform(true);
    setVisible(true);
  }

  private void onStart(ActionEvent e) {
    safeStop();
    int n = (Integer) countSpinner.getValue();
    int health = (Integer) healthSpinner.getValue();
    int damage = (Integer) damageSpinner.getValue();
    String fight = (String) fightMode.getSelectedItem();
    manager = new ImmortalManager(n, fight, health, damage);
    manager.start();
    output.setText("Simulation started with %d immortals (health=%d, damage=%d, fight=%s)%n"
      .formatted(n, health, damage, fight));
  }

  private void onPauseAndCheck(ActionEvent e) {
    if (manager == null) return;
    
    manager.pause();
    try {
      manager.controller().waitUntilAllPaused();
      Thread.sleep(50);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      output.setText("⚠️ Pausa interrumpida\n");
      return;
    }
    
    List<Immortal> pop = manager.populationSnapshot();
    long actualTotal = 0;
    StringBuilder sb = new StringBuilder();
    
    sb.append("════════════════════════════════\n");
    sb.append("   ESTADO DE LOS INMORTALES\n");
    sb.append("════════════════════════════════\n\n");
    
    for (Immortal im : pop) {
      int h = im.getHealth();
      actualTotal += h;
      String status = im.isAlive() ? "🟢" : "💀";
      sb.append(String.format("%s %-14s : %5d\n", status, im.name(), h));
    }
    
    long fights = manager.scoreBoard().totalFights();
    long expectedTotal = manager.expectedHealth();
    long difference = actualTotal - expectedTotal;
    boolean invariantOK = manager.checkInvariant();
    
    sb.append("\n================================\n");
    sb.append("   VALIDACIÓN DEL INVARIANTE\n");
    sb.append("================================\n");
    sb.append(String.format("Salud Total Actual:   %d\n", actualTotal));
    sb.append(String.format("Salud Total Esperada: %d\n", expectedTotal));
    sb.append(String.format("Peleas Registradas:   %d\n", fights));
    sb.append(String.format("Inmortales Vivos:     %d / %d\n", manager.aliveCount(), pop.size()));
    sb.append("--------------------------------\n");
    sb.append(String.format("Invariante: %s %s\n", 
        invariantOK ? "✓" : "✗", 
        invariantOK ? "SE CUMPLE" : "ROTO"));
    
    if (!invariantOK) {
      sb.append(String.format("Diferencia: %d\n", difference));
      sb.append("\n El invariante NO se cumple.\n");
      sb.append("Posibles causas:\n");
      sb.append("- Race conditions en peleas\n");
      sb.append("- Lecturas durante actualización\n");
      sb.append("- Orden de locks inconsistente\n");
    } else {
      sb.append("\n✓ El sistema es consistente.\n");
    }
    
    output.setText(sb.toString());
  }

  private void onResume(ActionEvent e) {
    if (manager == null) return;
    manager.resume();
  }

  private void onStop(ActionEvent e) { 
    safeStop();
    output.setText("✓ Simulación detenida correctamente.\n");
  }

  private void safeStop() {
    if (manager != null) {
      manager.stop();
      manager = null;
    }
  }

  public static void main(String[] args) {
    int count = Integer.getInteger("count", 8);
    String fight = System.getProperty("fight", "ordered");
    SwingUtilities.invokeLater(() -> new ControlFrame(count, fight));
  }
}
