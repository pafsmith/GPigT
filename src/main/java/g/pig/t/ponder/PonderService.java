package g.pig.t.ponder;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * The asynchronous "pondering" backend, mocked for now. A ponder call runs on a
 * dedicated single worker thread (never the shared common pool) and completes
 * with the placeholder response after {@link #MOCK_DELAY}. Wiring the real
 * backend later only replaces the delayed placeholder with the HTTP call — the
 * future-based structure stays.
 *
 * <p>The returned {@link CompletableFuture} is the thread-safe hand-off: game
 * logic polls it from the server-thread tick (e.g. {@code getNow(null)}).
 * Nothing in this class touches world or entity state, and callers must not
 * mutate game state from future callbacks — poll instead.
 */
public final class PonderService {
    /** Mock ponder duration; removed when the real backend is wired in. */
    private static final Duration MOCK_DELAY = Duration.ofSeconds(3);
    private static final String MOCK_RESPONSE = "??????";

    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "GPigT-Ponder");
        thread.setDaemon(true);
        return thread;
    });

    private PonderService() {
    }

    public static void initialize() {
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EXECUTOR.shutdownNow());
    }

    /**
     * Ponders a question off the main server thread. Completes with the
     * response after {@link #MOCK_DELAY}; never blocks the caller.
     */
    public static CompletableFuture<String> ponder(String question) {
        Objects.requireNonNull(question, "question");
        return CompletableFuture.supplyAsync(
                () -> MOCK_RESPONSE,
                CompletableFuture.delayedExecutor(MOCK_DELAY.toMillis(), TimeUnit.MILLISECONDS, EXECUTOR)
        );
    }
}
