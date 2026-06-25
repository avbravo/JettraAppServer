package com.jettra.server.core;

import com.jettra.server.JettraServer;

public class AutocloneManager {

    private final JettraServer mainServer;
    private final int sessionThreshold;
    private Thread monitorThread;
    private volatile boolean running;
    private boolean hasCloned = false;

    public AutocloneManager(JettraServer mainServer, int sessionThreshold) {
        this.mainServer = mainServer;
        this.sessionThreshold = sessionThreshold;
    }

    public void start() {
        if (running) return;
        running = true;
        monitorThread = new Thread(() -> {
            System.out.println("[AutocloneManager] Iniciado. Monitoreando sesiones (Límite: " + sessionThreshold + ")...");
            while (running) {
                try {
                    Thread.sleep(5000); // Check every 5 seconds
                    int count = JettraContext.getActiveSessionCount();
                    if (count > sessionThreshold && !hasCloned) {
                        System.out.println("[AutocloneManager] Alerta de carga: " + count + " sesiones superan el límite de " + sessionThreshold);
                        hasCloned = true;
                        mainServer.autoclone();
                    } else if (count <= sessionThreshold) {
                        // Reset if load drops significantly (optional logic)
                        hasCloned = false;
                    }
                } catch (InterruptedException e) {
                    running = false;
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    System.err.println("[AutocloneManager] Error en el monitoreo: " + e.getMessage());
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    public void stop() {
        running = false;
        if (monitorThread != null) {
            monitorThread.interrupt();
        }
    }
}
