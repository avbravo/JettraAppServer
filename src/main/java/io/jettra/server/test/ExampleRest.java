package io.jettra.server.test;

import java.io.Serializable;
import io.jettra.server.config.JettraConfigProperty;

public class ExampleRest implements Serializable {

    @JettraConfigProperty(name = "title")
    private String title;

    @JettraConfigProperty(name = "system.version")
    private String systemVersion;

    public void draw() {
        IO.println(title + " " + systemVersion);
    }
}
