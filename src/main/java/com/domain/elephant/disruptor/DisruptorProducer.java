package com.domain.elephant.disruptor;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;

/**
 * TODO describe the file
 *
 * @author lijing
 * @version 1.0.0
 * @since 2017/11/24
 */
public class DisruptorProducer {

    private Disruptor<LongEvent> disruptor;

    public DisruptorProducer(RingBuffer<LongEvent> ringBuffer) {
        this.ringBuffer = ringBuffer;
    }

    private RingBuffer<LongEvent> ringBuffer;

    public DisruptorProducer(Disruptor<LongEvent> disruptor) {
        this.disruptor = disruptor;
        this.ringBuffer = disruptor.getRingBuffer();
    }

    public void process01(long value) {

        long sequence = ringBuffer.next();
        try {
            ringBuffer.get(sequence).set(value);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public void process02(long value) {

        RingBuffer<LongEvent> ringBuffer = disruptor.getRingBuffer();
        long sequence = ringBuffer.next();
        try {
            ringBuffer.get(sequence).set(value);
        } finally {
            ringBuffer.publish(sequence);
        }
    }

    public static void main(String[] args) throws InterruptedException {

        DisruptorFacade disruptorFacade01 = new DisruptorFacade();
        DisruptorProducer test01 = new DisruptorProducer(disruptorFacade01.startDisruptorR());
        for (int i = 0; i < 10; i++) {
            test01.process01(i);
            Thread.sleep(1000);
        }
        disruptorFacade01.shutdownDisruptor();

        /*DisruptorFacade disruptorFacade02 = new DisruptorFacade();
        DisruptorProducer test02 = new DisruptorProducer(disruptorFacade02.startDisruptor());
        for (int i = 10; i < 20; i++) {
            test02.process02(i);
        }
        disruptorFacade02.shutdownDisruptor();*/
    }
}
