package org.keycloak.adapters.undertow;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.IOException;
import java.io.InputStream;

public class UndertowHttpServletRequest {

    public static HttpServletRequestWrapper setupServletInputStream(ServletRequest servletRequest, InputStream inputStream) {
        return new HttpServletRequestWrapper((HttpServletRequest) servletRequest) {
            @Override
            public ServletInputStream getInputStream() {
                inputStream.mark(0);
                return new ServletInputStream() {
                    @Override
                    public boolean isFinished() {
                        return false;
                    }

                    @Override
                    public boolean isReady() {
                        return false;
                    }

                    @Override
                    public void setReadListener(ReadListener readListener) {

                    }

                    @Override
                    public int read() throws IOException {
                        return inputStream.read();
                    }
                };
            }
        };
    }
}
