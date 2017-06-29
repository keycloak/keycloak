package org.keycloak.testsuite.docker;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Docker doesn't provide a static/reliable way to grab the host machine's IP.
 * <p>
 * this currently just returns the first address for the bridge adapter starting with 'docker'.  Not the most elegant solution,
 * but I'm open to suggestions.
 *
 * @see https://github.com/moby/moby/issues/1143 and related issues referenced therein.
 */
public class DockerHostIpSupplier implements Supplier<Optional<String>> {

    @Override
    public Optional<String> get() {
        final Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            return Optional.empty();
        }

        return Collections.list(networkInterfaces).stream()
                .filter(networkInterface -> networkInterface.getDisplayName().startsWith("docker"))
                .flatMap(networkInterface -> Collections.list(networkInterface.getInetAddresses()).stream())
                .map(InetAddress::getHostAddress)
                .filter(DockerHostIpSupplier::looksLikeIpv4Address)
                .findFirst();
    }

    public static boolean looksLikeIpv4Address(final String ip) {
        return IPv4RegexPattern.matcher(ip).matches();
    }

    private static final Pattern IPv4RegexPattern = Pattern.compile("^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$");

}
