package org.jboss.arquillian.warp.ftest.async;

import java.io.Serializable;

public class ReverseRequest implements Serializable {

    private long serial;
    private String payload;

    public ReverseRequest(long serial, String payload) {
        super();
        this.serial = serial;
        this.payload = payload;
    }

    public long getSerial() {
        return serial;
    }

    public String getPayload() {
        return payload;
    }
}
