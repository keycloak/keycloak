package org.keycloak.testsuite.arquillian.undertow;

import java.util.Random;

import org.jboss.arquillian.container.spi.ConfigurationException;
import org.jboss.arquillian.container.spi.client.container.ContainerConfiguration;

public class UndertowContainerConfiguration implements ContainerConfiguration {

	private static final int RANDOM_PORT_FLAG = -1;
	private static final int MAX_PORT = 48128;
	private static final int MIN_PORT = 1024;

	private String bindAddress = "localhost";

	private int bindHttpPort = 8080;

	public void validate() throws ConfigurationException {
		if(bindHttpPort == RANDOM_PORT_FLAG) {
			bindHttpPort = generateRandomPort();
		}
	}

	public String getBindAddress() {
		return bindAddress;
	}

	public void setBindAddress(String bindAddress) {
		this.bindAddress = bindAddress;
	}

	public int getBindHttpPort() {
		return bindHttpPort;
	}

	public void setBindHttpPort(int bindHttpPort) {
		this.bindHttpPort = bindHttpPort;
	}

	private int generateRandomPort() {
		Random random = new Random();
		int nextPortZeroBased = random.nextInt(MAX_PORT);
		return nextPortZeroBased + MIN_PORT;
	}
	
}
