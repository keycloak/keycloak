package org.keycloak.protocol.oidc.utils;

import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

public class UrlStringTest {

    @Test
    public void shouldExtractProtocolWhenPresent() {
        UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getProtocol(), equalTo("http"));

        reader = new UrlString("https://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getProtocol(), equalTo("https"));

        reader = new UrlString("ftp://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getProtocol(), equalTo("ftp"));
    }

    @Test
    public void shouldWildcardProtocolWhenWildcard() {
        UrlString reader = new UrlString("*://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getProtocol(), equalTo("*"));

        reader = new UrlString("http*://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getProtocol(), equalTo("http*"));

        reader = new UrlString("*tp*://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getProtocol(), equalTo("*tp*"));
    }

    @Test
    public void shouldExtractHostnameWhenPresent() {
        UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("www.example.com"));

        reader = new UrlString("http://localhost:8080/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("localhost"));

        reader = new UrlString("http://localhost.localdomain:8080/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("localhost.localdomain"));
    }

    @Test
    public void shouldWildcardHostnameWhenWildcard() {
        UrlString reader = new UrlString("http://*/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("*"));

        reader = new UrlString("http://*.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("*.example.com"));

        reader = new UrlString("http://sandbox.*.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("sandbox.*.example.com"));

        reader = new UrlString("http://sandbox.*.dev.*.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("sandbox.*.dev.*.example.com"));

        reader = new UrlString("http://sandbox.*.dev.*.example.com:8080/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("sandbox.*.dev.*.example.com"));

        reader = new UrlString("http://*:8443/path/to/content/page.html?lang=en");
        assertThat(reader.getHost(), equalTo("*"));
    }

    @Test
    public void shouldExtractPortWhenPresent() {
        UrlString reader = new UrlString("http://www.example.com:80/path/to/content/page.html?lang=en");
        assertThat(reader.getPort(), equalTo(Optional.of("80")));

        reader = new UrlString("http://www.example.com:*/path/to/content/page.html?lang=en");
        assertThat(reader.getPort(), equalTo(Optional.of("*")));

        reader = new UrlString("http://www.example.com:*443/path/to/content/page.html?lang=en");
        assertThat(reader.getPort(), equalTo(Optional.of("*443")));
    }

    @Test
    public void shouldExtractEmptyWhenPortAbsent() {
        final UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getPort(), equalTo(Optional.empty()));
    }

    @Test
    public void shouldExtractPathWhenPresent() {
        final UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getPath(), equalTo(Optional.of("/path/to/content/page.html")));
    }

    @Test
    public void shouldExtractEmptyWhenPathAbsent() {
        final UrlString reader = new UrlString("http://www.example.com?lang=en");
        assertThat(reader.getPath(), equalTo(Optional.empty()));
    }

    @Test
    public void shouldWildcardPathWhenWildcard() {
        UrlString reader = new UrlString("http://www.example.com/*");
        assertThat(reader.getPath(), equalTo(Optional.of("/*")));

        reader = new UrlString("http://www.example.com/user1/*");
        assertThat(reader.getPath(), equalTo(Optional.of("/user1/*")));

        reader = new UrlString("http://www.example.com/user1/*?region=us");
        assertThat(reader.getPath(), equalTo(Optional.of("/user1/*")));
    }

    @Test
    public void shouldExtractQueryArgumentsWhenPresent() {
        UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=en");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("lang=en")));

        reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=en&region=us");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("lang=en&region=us")));

        reader = new UrlString("http://www.example.com/path/to/content/page.html?lang&region=us");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("lang&region=us")));

        reader = new UrlString("http://www.example.com/path/to/content/page.html?lang&region");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("lang&region")));
    }

    @Test
    public void shouldExtractEmptyWhenQueryArgumentsAbsent() {
        final UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html");
        assertThat(reader.getQueryParams(), equalTo(Optional.empty()));
    }

    @Test
    public void shouldExtractWildcardWhenQueryArgumentsWildcard() {
        UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=*");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("lang=*")));

        reader = new UrlString("http://www.example.com/path/to/content/page.html?lang=*&region=us");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("lang=*&region=us")));

        reader = new UrlString("http://www.example.com/path/to/content/page.html?*region=us*");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("*region=us*")));

        reader = new UrlString("http://www.example.com/path/to/content/page.html?locale=en_*");
        assertThat(reader.getQueryParams(), equalTo(Optional.of("locale=en_*")));
    }

    @Test
    public void shouldExtractFragmentWhenPresent() {
        final UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html#section5");
        assertThat(reader.getFragment(), equalTo(Optional.of("section5")));
    }

    @Test
    public void shouldExtractEmptyWhenFragmentAbsent() {
        final UrlString reader = new UrlString("http://www.example.com/path/to/content/page.html");
        assertThat(reader.getFragment(), equalTo(Optional.empty()));
    }
}
