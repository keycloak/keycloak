package org.keycloak.gatling.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * @author <a href="mailto:mstrukel@redhat.com">Marko Strukelj</a>
 */
class LogReader {

    private BufferedReader reader;

    LogReader(File file) throws FileNotFoundException {
        reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), Charset.forName("utf-8")));
    }

    LogLine readLine() throws IOException {
        String line = reader.readLine();
        return line != null ? new LogLine(line) : null;
    }

    void close() throws IOException {
        reader.close();
    }
}
