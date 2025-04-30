package org.keycloak.it.utils;

import java.util.List;

public interface OutputConsumer {

    void onStdOut(String line);
    void onErrOut(String line);
    void reset();

    List<String> getStdOut();
    List<String> getErrOut();

}
