package com.domain.elephant.disruptor;

import com.lmax.disruptor.EventHandler;

/**
 * TODO describe the file
 *
 * @author lijing
 * @version 1.0.0
 * @since 2017/11/24
 */
abstract class AbstractLongEventHandler implements EventHandler<LongEvent> {

    public void onEvent(LongEvent event, long sequence, boolean endOfBatch) throws Exception {
        System.out.println(System.currentTimeMillis() + "--" + this.getClass().getSimpleName() + "--value=" + event.get());
        handle();
    }

    abstract void handle() throws InterruptedException;
}
