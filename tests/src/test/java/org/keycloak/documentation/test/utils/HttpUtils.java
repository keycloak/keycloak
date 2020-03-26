package org.keycloak.documentation.test.utils;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.cookie.CookieSpec;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class HttpUtils {

    public Response load(String url) {
        CloseableHttpClient client = createClient();
        Response response = new Response();

        try {
            HttpGet h = new HttpGet(url);
            CloseableHttpResponse r = client.execute(h);
            int status = r.getStatusLine().getStatusCode();

            if (status == 200) {
                response.setSuccess(true);

                HttpEntity entity = r.getEntity();
                String c = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8);

                response.setContent(c);
            } else if (status == 301 || status == 302) {
                String location = r.getFirstHeader("Location").getValue();
                response.setRedirectLocation(location);
                response.setSuccess(false);
            } else {
                response.setError("invalid status code " + status);
                response.setSuccess(false);
            }
        } catch (Exception e) {
            response.setError("exception " + e.getMessage());
            response.setSuccess(false);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
            }
        }

        return response;
    }

    private CloseableHttpClient createClient() {
        return HttpClientBuilder.create()
                    .setRetryHandler(new DefaultHttpRequestRetryHandler(Constants.HTTP_RETRY, true))
                    .setDefaultRequestConfig(
                            RequestConfig.custom().setCookieSpec(CookieSpecs.IGNORE_COOKIES).build()
                    )
                    .build();
    }

    public Response isValid(String url) {
        CloseableHttpClient client = createClient();
        Response response = new Response();

        try {
            HttpHead h = new HttpHead(url);
            CloseableHttpResponse r = client.execute(h);
            int status = r.getStatusLine().getStatusCode();

            if (status == 200) {
                response.setSuccess(true);
            } else if (status == 301 || status == 302) {
                String location = r.getFirstHeader("Location").getValue();
                response.setRedirectLocation(location);
                response.setSuccess(false);
            } else {
                response.setError("invalid status code " + status);
                response.setSuccess(false);
            }
        } catch (Exception e) {
            response.setError("exception " + e.getMessage());
            response.setSuccess(false);
        } finally {
            try {
                client.close();
            } catch (IOException e) {
            }
        }

        return response;
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
