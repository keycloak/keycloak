package org.keycloak.documentation.test.utils;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;

public class HttpUtils {

    private static final Logger logger = Logger.getLogger(HttpUtils.class);

    public boolean isValid(String url) {
        Response response = load(url, false, false);
        return response.isSuccess();
    }

    public Response load(String url, boolean readContent, boolean followRedirects) {
        int retryCount = Constants.HTTP_RETRY;

        while (true) {
            HttpURLConnection.setFollowRedirects(followRedirects);
            HttpURLConnection connection = null;
            Response response = new Response();
            try {
                connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setConnectTimeout(Constants.HTTP_CONNECTION_TIMEOUT);
                connection.setReadTimeout(Constants.HTTP_READ_TIMEOUT);
                int status = connection.getResponseCode();
                if (status == 200) {
                    if (readContent) {
                        StringWriter w = new StringWriter();
                        IOUtils.copy(connection.getInputStream(), w, "utf-8");
                        response.setContent(w.toString());
                    }
                    response.setSuccess(true);
                } else if (status == 301 || status == 302) {
                    String location = URLDecoder.decode(connection.getHeaderField("Location"), "utf-8");
                    response.setRedirectLocation(location);
                    response.setSuccess(false);
                } else {
                    response.setError("invalid status code " + status);
                    response.setSuccess(false);
                }

                return response;
            } catch (Exception e) {
                response.setError("exception " + e.getMessage());
                response.setSuccess(false);

                retryCount--;
                if (retryCount == 0) {
                    return response;
                }

                logger.info("Retrying " + url);
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
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
