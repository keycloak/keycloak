package org.keycloak.client.registration;

import org.apache.http.StatusLine;

import java.io.IOException;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class HttpErrorException extends IOException {

    private StatusLine statusLine;

    public HttpErrorException(StatusLine statusLine) {
        this.statusLine = statusLine;
    }

    public StatusLine getStatusLine() {
        return statusLine;
    }

}
