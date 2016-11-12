package com.liveperson.tutorial.ws.bot.base;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author elyran
 * @since 11/2/16.
 */
public class Conversation {
    private AtomicInteger sequence;

    public Conversation() {
        sequence = new AtomicInteger(0);
    }

    public boolean compareAndSetSequence(int sequence) {
        final int current = this.sequence.get();
        if (current < sequence) {
            final boolean success = this.sequence.compareAndSet(current, sequence);
            return success || compareAndSetSequence(sequence);
        } else {
            return false;
        }
    }

    public int getSequence() {
        return sequence.get();
    }
}
