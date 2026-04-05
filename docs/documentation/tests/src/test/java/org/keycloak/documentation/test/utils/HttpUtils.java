package org.keycloak.documentation.test.utils;

import org.apache.hc.client5.http.ClientProtocolException;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.DefaultHttpRequestRetryStrategy;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.TimeValue;

import java.io.IOException;

public class HttpUtils {

    private CloseableHttpClient client;

    public HttpUtils() {
        try {
            client = createClient();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    client.close();
                } catch (IOException e) {
                }
            }
        });
    }

    public Response load(String url) {
        return exec(new HttpGet(url));
    }

    public Response isValid(String url) {
        return exec(new HttpHead(url));
    }

    private Response exec(HttpUriRequestBase method) {
        Response response = new Response();

        HttpClientResponseHandler<String> responseHandler = new HttpClientResponseHandler<String>() {
            @Override
            public String handleResponse(ClassicHttpResponse r) throws IOException {
                int status = r.getCode();

                if (status == HttpStatus.SC_SUCCESS) {
                    response.setSuccess(true);

                    HttpEntity entity = r.getEntity();
                    try {
                        String c = entity != null ? EntityUtils.toString(entity) : "";
                        response.setContent(c);
                    } catch (ParseException e) {
                        throw new ClientProtocolException(e);
                    }
                } else if (status / 100 == 3) {
                    String location = r.getFirstHeader("Location").getValue();
                    response.setRedirectLocation(location);
                    response.setSuccess(false);
                } else {
                    response.setError("invalid status code " + status);
                    response.setSuccess(false);
                }
                return "";
            }
        };

        try {
            // add common headers that are needed by some pages
            method.addHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9");
            method.addHeader(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            method.addHeader(HttpHeaders.USER_AGENT, "Java/" + System.getProperty("java.version") + " (https://www.keycloak.org; keycloak-dev@googlegroups.com)");
            client.execute(method, responseHandler);
        } catch (Exception e) {
            response.setError("exception " + e.getMessage());
            response.setSuccess(false);
        }

        return response;
    }

    private static CloseableHttpClient createClient() throws Exception {
        return HttpClientBuilder.create()
            .setRetryStrategy(new DefaultHttpRequestRetryStrategy(
                Constants.HTTP_RETRY,
                TimeValue.ofSeconds(1L)
            ))
            .disableCookieManagement()
            .disableRedirectHandling()
            .setConnectionManager(
                PoolingHttpClientConnectionManagerBuilder.create()
                    .build()
            )
            .build();
    }

    public static class Response {

        private boolean success;
        private String content;
        private String redirectLocation;
        private String error;

        public boolean isSuccess() {
            return success;
        }

        private void setSuccess(boolean success) {
            this.success = success;
        }

        public String getContent() {
            return content;
        }

        private void setContent(String content) {
            this.content = content;
        }

        public String getRedirectLocation() {
            return redirectLocation;
        }

        private void setRedirectLocation(String redirectLocation) {
            this.redirectLocation = redirectLocation;
        }

        public String getError() {
            return error;
        }

        private void setError(String error) {
            this.error = error;
        }
    }

}
