package org.keycloak.http.simple;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.keycloak.connections.httpclient.SafeInputStream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.Header;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;

public class SimpleHttpResponse implements AutoCloseable {

    private final HttpResponse response;
    private final long maxConsumedResponseSize;
    private final ObjectMapper objectMapper;
    private int statusCode = -1;
    private String responseString;
    private ContentType contentType;

    public SimpleHttpResponse(HttpResponse response, long maxConsumedResponseSize, ObjectMapper objectMapper) {
        this.response = response;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.objectMapper = objectMapper;
    }

    private void readResponse() throws IOException {
        if (statusCode == -1) {
            statusCode = response.getStatusLine().getStatusCode();

            InputStream is;
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                is = entity.getContent();
                contentType = ContentType.getOrDefault(entity);
                Charset charset = contentType.getCharset();
                try {
                    HeaderIterator it = response.headerIterator();
                    while (it.hasNext()) {
                        Header header = it.nextHeader();
                        if (header.getName().equals("Content-Encoding") && header.getValue().equals("gzip")) {
                            is = new GZIPInputStream(is);
                        }
                    }

                    is = new SafeInputStream(is, maxConsumedResponseSize);

                    try (InputStreamReader reader = charset == null ? new InputStreamReader(is, StandardCharsets.UTF_8) :
                            new InputStreamReader(is, charset)) {

                        StringWriter writer = new StringWriter();

                        char[] buffer = new char[1024 * 4];
                        for (int n = reader.read(buffer); n != -1; n = reader.read(buffer)) {
                            writer.write(buffer, 0, n);
                        }

                        responseString = writer.toString();
                    }
                } finally {
                    if (is != null) {
                        is.close();
                    }
                }
            }
        }
    }

    public int getStatus() throws IOException {
        readResponse();
        return response.getStatusLine().getStatusCode();
    }

    public JsonNode asJson() throws IOException {
        return objectMapper.readTree(asString());
    }

    public <T> T asJson(Class<T> type) throws IOException {
        return objectMapper.readValue(asString(), type);
    }

    public <T> T asJson(TypeReference<T> type) throws IOException {
        return objectMapper.readValue(asString(), type);
    }

    public String asString() throws IOException {
        readResponse();
        return responseString;
    }

    public String getFirstHeader(String name) throws IOException {
        readResponse();
        Header[] headers = response.getHeaders(name);

        if (headers != null && headers.length > 0) {
            return headers[0].getValue();
        }

        return null;
    }

    public List<String> getHeader(String name) throws IOException {
        readResponse();
        Header[] headers = response.getHeaders(name);

        if (headers != null && headers.length > 0) {
            return Stream.of(headers).map(Header::getValue).collect(Collectors.toList());
        }

        return null;
    }

    public Header[] getAllHeaders() throws IOException {
        readResponse();
        return response.getAllHeaders();
    }

    public ContentType getContentType() throws IOException {
        readResponse();
        return contentType;
    }

    public Charset getContentTypeCharset() throws IOException {
        readResponse();
        if (contentType != null) {
            Charset charset = contentType.getCharset();
            if (charset != null) {
                return charset;
            }
        }
        return StandardCharsets.UTF_8;
    }

    public void close() throws IOException {
        readResponse();
    }
}
