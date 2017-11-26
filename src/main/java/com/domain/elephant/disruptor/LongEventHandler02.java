package com.domain.elephant.disruptor;

import com.lmax.disruptor.EventHandler;

/**
 * TODO describe the file
 *
 * @author lijing
 * @version 1.0.0
 * @since 2017/11/24
 */
class LongEventHandler02 extends AbstractLongEventHandler {

    @Override
    void handle() throws InterruptedException {

        Thread.sleep(5000);
    }
}
