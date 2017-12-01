package com.domain.elephant.disruptor;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * TODO describe the file
 *
 * @author lijing
 * @version 1.0.0
 * @since 2017/11/24
 */
public class DisruptorFacade {

    private Disruptor<LongEvent> disruptor;

    private static final int ringBufferSize = 4096;

    public Disruptor<LongEvent> startDisruptor() {

        disruptor = new Disruptor<>(new LongEventFactory(), ringBufferSize, new LongThreadFactory(), ProducerType.SINGLE, new BlockingWaitStrategy());
        EventHandlerGroup<LongEvent> group = disruptor.handleEventsWith(new LongEventHandler01());
        group.then(new LongEventHandler02());
        disruptor.start();

        return disruptor;
    }

    public RingBuffer<LongEvent> startDisruptorR() {

        disruptor = new Disruptor<>(new LongEventFactory(), ringBufferSize, Executors.defaultThreadFactory(), ProducerType.SINGLE, new YieldingWaitStrategy());
        disruptor.handleEventsWith(new LongEventHandler01()).then(new LongEventHandler02());
        return disruptor.start();
    }

    public void shutdownDisruptor() {

        System.out.println("shutdown disruptor");
        if (disruptor != null) {
            disruptor.shutdown();
        }
    }

    private class LongEventFactory implements EventFactory<LongEvent> {

        public LongEvent newInstance() {
            return new LongEvent();
        }
    }

    private class LongThreadFactory implements ThreadFactory {

        public Thread newThread(Runnable r) {
            return new Thread(r);
        }
    }
}
