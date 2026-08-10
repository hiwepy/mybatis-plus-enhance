package com.baomidou.mybatisplus.enhance.context;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.baomidou.mybatisplus.enhance.crypto.enums.SignatureUpdateStrategy;

import java.util.Objects;

/**
 * Thread-local context for table-signature update strategy.
 *
 * <p>Scopes bridge the gap between Service transactions and interceptors, conveying
 * explicit update semantics. Callers must use try-with-resources to close the scope;
 * nesting restores the previous strategy on close.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 */
public final class SignatureUpdateContext {

    private static final TransmittableThreadLocal<SignatureUpdateStrategy> STRATEGY =
            new TransmittableThreadLocal<>();

    private SignatureUpdateContext() {
    }

    /**
     * Opens a scope with the specified signature update strategy.
     *
     * @param strategy the update strategy for this scope; must not be {@code null}
     * @return a scope handle that must be closed
     */
    public static Scope open(SignatureUpdateStrategy strategy) {
        SignatureUpdateStrategy previous = STRATEGY.get();
        STRATEGY.set(Objects.requireNonNull(strategy, "strategy must not be null"));
        return new Scope(previous);
    }

    /**
     * Returns the current strategy. When no scope is open, defaults to the safe
     * {@link SignatureUpdateStrategy#REJECT_PARTIAL}.
     *
     * @return the current signature update strategy
     */
    public static SignatureUpdateStrategy current() {
        SignatureUpdateStrategy strategy = STRATEGY.get();
        return Objects.isNull(strategy) ? SignatureUpdateStrategy.REJECT_PARTIAL : strategy;
    }

    /**
     * Clears the current thread's strategy. Primarily for request boundaries and tests.
     */
    public static void clear() {
        STRATEGY.remove();
    }

    /**
     * Auto-restoring scope handle for signature update strategy.
     */
    public static final class Scope implements AutoCloseable {

        private final SignatureUpdateStrategy previous;
        private boolean closed;

        private Scope(SignatureUpdateStrategy previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            if (Objects.isNull(previous)) {
                STRATEGY.remove();
            } else {
                STRATEGY.set(previous);
            }
            closed = true;
        }
    }
}
