package ch.softappeal.yass.core.remote.session;

import ch.softappeal.yass.util.Check;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Closes a session if it isn't healthy.
 */
public final class SessionWatcher {

    private SessionWatcher() {
        // disable
    }

    public interface Checker {
        /**
         * Must execute without an exception within timeout if session is ok.
         */
        void check() throws Exception;
    }

    /**
     * @param executor used twice, must interrupt it's threads to terminate checks (use {@link ExecutorService#shutdownNow()})
     */
    public static void watchSession(
        final Executor executor, final Session session,
        final long delaySeconds, final long intervalSeconds, final long timeoutSeconds,
        final Checker checker
    ) {
        Check.notNull(session);
        Check.notNull(checker);
        executor.execute(() -> {
            try {
                TimeUnit.SECONDS.sleep(delaySeconds);
            } catch (final InterruptedException ignore) {
                return;
            }
            while (!session.isClosed() && !Thread.interrupted()) {
                try {
                    TimeUnit.SECONDS.sleep(intervalSeconds);
                } catch (final InterruptedException ignore) {
                    return;
                }
                final CountDownLatch latch = new CountDownLatch(1);
                executor.execute(() -> {
                    try {
                        if (!latch.await(timeoutSeconds, TimeUnit.SECONDS)) {
                            Session.close(session, new Exception("check timeout"));
                        }
                    } catch (final InterruptedException e) {
                        Session.close(session, e);
                    }
                });
                try {
                    checker.check();
                } catch (final Exception e) {
                    Session.close(session, e);
                    return;
                }
                latch.countDown();
            }
        });
    }

    public static void watchSession(
        final Executor executor, final Session session,
        final long intervalSeconds, final long timeoutSeconds,
        final Checker checker
    ) {
        watchSession(executor, session, 0L, intervalSeconds, timeoutSeconds, checker);
    }

}
