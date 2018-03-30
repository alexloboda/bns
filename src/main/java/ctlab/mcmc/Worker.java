package ctlab.mcmc;

public class Worker extends Thread {
    private Model m;
    private int warmup;
    private int steps;

    public Worker(Model m, int steps, int warmup) {
        this.m = m;
        this.steps = steps;
        this.warmup = warmup;
    }

    public void run() {
        for (int i = 0; i < warmup; i++) {
            m.step(true);
        }

        for (int i = 0; i < steps; i++) {
            if (Thread.interrupted()) {
                m.finish();
                return;
            }
            m.step(false);
        }
        m.finish();
    }
}
