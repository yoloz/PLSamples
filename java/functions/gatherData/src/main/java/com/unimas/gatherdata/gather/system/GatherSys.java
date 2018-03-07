package com.unimas.gatherdata.gather.system;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.util.Properties;

public class GatherSys {

    private Logger logger = LoggerFactory.getLogger(GatherSys.class);

    /**
     * configuration definition
     */
    public enum CONFIG {
        INTERVAL("gather.system.interval.sec");
        private String value;

        CONFIG(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

//    private final int interval;

    public GatherSys(Properties config) {

    }

}
