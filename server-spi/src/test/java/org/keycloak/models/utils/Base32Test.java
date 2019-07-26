package org.keycloak.models.utils;

import org.junit.Assert;
import org.junit.Test;

public class Base32Test {
	
	@Test
	public void decode() {
		Assert.assertArrayEquals(new byte[0], Base32.decode(""));
		Assert.assertArrayEquals(new byte[0], Base32.decode("!"));
		Assert.assertArrayEquals(new byte[] {122}, Base32.decode(" pJ"));
		Assert.assertArrayEquals(new byte[] {-72}, Base32.decode("x{|"));
		Assert.assertArrayEquals(new byte[] {60, 122}, Base32.decode("hR\u00015"));
		Assert.assertArrayEquals(new byte[] {-8, 0, 0, 0}, Base32.decode("\\\u20487`\u0010]1"));
		Assert.assertArrayEquals(new byte[] {-56, -84, -37, -49, 19}, Base32.decode("Zcwn\\xtyt"));
		Assert.assertArrayEquals(new byte[] {83, -18, -10, -29, -43, 0, 0, 0, 0},
			Base32.decode("\u0000\u8008\u4037`K\u0000\u8033P\u00b8XpNy6V"));
	}

	@Test
	public void encode() {
		Assert.assertEquals("", Base32.encode(new byte[0]));
		Assert.assertEquals("AA", Base32.encode(new byte[] {0}));
		Assert.assertEquals("WLALBMA",	Base32.encode(new byte[] {-78, -64, -80, -80}));
		Assert.assertEquals("WNALBAA",	Base32.encode(new byte[] {-77, 64, -80, -128}));
		Assert.assertEquals("WNALBAE7", Base32.encode(new byte[] {-77, 64, -80, -128, -97}));
	}
}
