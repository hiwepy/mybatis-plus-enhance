package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;

import java.util.Objects;

/**
 * Thread-local context for controlling table-signature verification.
 *
 * <p>Intended only for historical data re-signing and deferred re-sign flows that
 * read rows whose signatures have not yet been refreshed. Normal business queries
 * must not open this scope, ensuring that signature mismatches always fail closed.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public final class SignatureVerificationContext {

    private static final TransmittableThreadLocal<Integer> IGNORE_DEPTH = new TransmittableThreadLocal<>();

    private SignatureVerificationContext() {
    }

    /**
     * Opens a maintenance scope that temporarily ignores signature verification.
     *
     * @return a scope handle that must be closed
     */
    public static Scope openIgnored() {
        Integer previous = IGNORE_DEPTH.get();
        IGNORE_DEPTH.set(Objects.isNull(previous) ? 1 : previous + 1);
        return new Scope(previous);
    }

    /**
     * Returns whether the current thread is within a maintenance re-sign scope.
     *
     * @return {@code true} if signature verification is temporarily ignored
     */
    public static boolean isIgnored() {
        Integer depth = IGNORE_DEPTH.get();
        return Objects.nonNull(depth) && depth > 0;
    }

    /**
     * Clears the current thread's state. Primarily for request boundaries and tests.
     */
    public static void clear() {
        IGNORE_DEPTH.remove();
    }

    /**
     * Auto-restoring scope handle for signature verification bypass.
     */
    public static final class Scope implements AutoCloseable {

        private final Integer previous;
        private boolean closed;

        private Scope(Integer previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Objects.isNull(previous)) {
                IGNORE_DEPTH.remove();
            } else {
                IGNORE_DEPTH.set(previous);
            }
            closed = true;
        }
    }
}
