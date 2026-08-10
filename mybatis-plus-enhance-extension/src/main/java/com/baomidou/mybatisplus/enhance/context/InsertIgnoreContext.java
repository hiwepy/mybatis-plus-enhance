package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

/**
 * Thread-local context for {@code INSERT IGNORE} mode.
 *
 * <p>Creates a scope via {@link #open()} for use with try-with-resources, ensuring that the
 * state prior to entering the scope is restored even in nested calls and exception paths.
 * Uses {@link TransmittableThreadLocal} to propagate context to async tasks when paired
 * with TTL thread-pool wrappers.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public final class InsertIgnoreContext {

    /**
     * Nesting depth of the current execution chain within the {@code INSERT IGNORE} scope.
     */
    private static final TransmittableThreadLocal<Integer> DEPTH = new TransmittableThreadLocal<>();

    /**
     * Utility class; prevents instantiation.
     */
    private InsertIgnoreContext() {
    }

    /**
     * Opens an {@code INSERT IGNORE} scope for the current thread.
     *
     * <p>Scopes support nesting; closing an inner scope restores the outer depth.
     * Callers should prefer try-with-resources to avoid leaving stale context on
     * exception paths.</p>
     *
     * @return a scope handle that must be closed
     */
    public static Scope open() {
        Integer previousDepth = DEPTH.get();
        DEPTH.set(Objects.isNull(previousDepth) ? 1 : previousDepth + 1);
        return new Scope(previousDepth);
    }

    /**
     * Returns whether the current thread has {@code INSERT IGNORE} rewriting enabled.
     *
     * @return {@code true} if the scope depth is greater than zero
     */
    public static boolean isEnabled() {
        Integer depth = DEPTH.get();
        return Objects.nonNull(depth) && depth > 0;
    }

    /**
     * Forcefully clears the scope state for the current thread.
     *
     * <p>Intended for request boundaries, thread reuse, and test cleanup.
     * Normal business code should close the {@link Scope} to correctly
     * restore nested state.</p>
     */
    public static void clear() {
        DEPTH.remove();
    }

    /**
     * Auto-restoring scope handle for {@code INSERT IGNORE}.
     *
     * <p>Repeated calls to {@link #close()} are safe.</p>
     */
    public static final class Scope implements AutoCloseable {

        /**
         * Nesting depth before this scope was opened; {@code null} if not previously enabled.
         */
        private final Integer previousDepth;

        /**
         * Whether restoration has already been performed, preventing double-close from
         * corrupting an outer scope.
         */
        private boolean closed;

        /**
         * Creates a scope restoration handle.
         *
         * @param previousDepth the nesting depth before this scope was opened
         */
        private Scope(Integer previousDepth) {
            this.previousDepth = previousDepth;
        }

        /**
         * Closes this scope and restores the nesting depth that was active before opening.
         */
        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Objects.isNull(previousDepth)) {
                DEPTH.remove();
            } else {
                DEPTH.set(previousDepth);
            }
            closed = true;
        }
    }
}
