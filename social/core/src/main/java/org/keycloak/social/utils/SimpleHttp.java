package org.keycloak.social.utils;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SimpleHttp {

    private static ObjectMapper mapper = new ObjectMapper();

    private String url;
    private String method;
    private Map<String, String> headers;
    private Map<String, String> params;

    private SimpleHttp(String url, String method) {
        this.url = url;
        this.method = method;
    }

    public static SimpleHttp doGet(String url) {
        return new SimpleHttp(url, "GET");
    }

    public static SimpleHttp doPost(String url) {
        return new SimpleHttp(url, "POST");
    }

    public SimpleHttp header(String name, String value) {
        if (headers == null) {
            headers = new HashMap<String, String>();
        }
        headers.put(name, value);
        return this;
    }

    public SimpleHttp param(String name, String value) {
        if (params == null) {
            params = new HashMap<String, String>();
        }
        params.put(name, value);
        return this;
    }

    public JsonNode asJson() throws IOException {
        return mapper.readTree(asString());
    }

    public String asString() throws IOException {
        boolean get = method.equals("GET");
        boolean post = method.equals("POST");

        StringBuilder sb = new StringBuilder();
        if (get) {
            sb.append(url);
        }

        if (params != null) {
            boolean f = true;
            for (Map.Entry<String, String> p : params.entrySet()) {
                if (f) {
                    f = false;
                    if (get) {
                        sb.append("?");
                    }
                } else {
                    sb.append("&");
                }
                sb.append(URLEncoder.encode(p.getKey(), "UTF-8"));
                sb.append("=");
                sb.append(URLEncoder.encode(p.getValue(), "UTF-8"));
            }
        }

        if (get) {
            url = sb.toString();
        }

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        OutputStream os = null;
        InputStream is = null;

        try {
            connection.setRequestMethod(method);

            if (headers != null) {
                for (Map.Entry<String, String> h : headers.entrySet()) {
                    connection.setRequestProperty(h.getKey(), h.getValue());
                }
            }

            if (post) {
                String data = sb.toString();

                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                connection.setRequestProperty("Content-Length", String.valueOf(data.length()));

                os = connection.getOutputStream();
                os.write(data.getBytes());
            } else {
                connection.setDoOutput(false);
            }

            is = connection.getInputStream();
            return toString(is);
        } finally {
            if (os != null) {
                os.close();
            }

            if (is != null) {
                is.close();
            }
        }
    }

    private String toString(InputStream is) throws IOException {
        InputStreamReader reader = new InputStreamReader(is);

        StringWriter writer = new StringWriter();

        char[] buffer = new char[1024 * 4];
        for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
            writer.write(buffer, 0, n);
        }

        return writer.toString();
    }

}
