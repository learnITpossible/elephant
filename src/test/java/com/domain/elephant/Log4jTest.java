package com.domain.elephant;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * com.domain.elephant
 * @author Mark Li
 * @version 1.0.0
 * @since 2017/11/27
 */
public class Log4jTest {

    private static final Logger logger = LoggerFactory.getLogger(Log4jTest.class);

    @Test
    public void testException() throws Exception {
        logger.debug("test exception start...");
        logger.debug(throwException());
    }

    String throwException() throws Exception {
        throw new Exception("error");
    }
}
