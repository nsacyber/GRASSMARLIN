package grassmarlin.ui.common.tasks;

import javafx.application.Platform;

import java.util.concurrent.CountDownLatch;

import static javafx.application.Platform.isFxApplicationThread;

public class SynchronousPlatform {
    /**
     * Run a task in the FX thread and wait for it to complete.
     * This should be used sparingly from worker threads, generally only from the loading code.
     *
     * This code is effectively taken from PlatformImpl.runAndWait, which is not exposed through Platform.  I don't know why it isn't exposed, other than the generally poor approach to error handling (which is not a condemnation of the JavaFX code, as ours has similarly poor error handling in parts)
     * @param runnable
     */
    public static void runNow(final Runnable runnable) {
        if (isFxApplicationThread()) {
            runnable.run();
        } else {
            final CountDownLatch doneLatch = new CountDownLatch(1);
            Platform.runLater(() -> {
                try {
                    runnable.run();
                } finally {
                    doneLatch.countDown();
                }
            });

            try {
                doneLatch.await();
            } catch (InterruptedException ex) {
                //If interrupted just resume.
            }
        }
    }
}
