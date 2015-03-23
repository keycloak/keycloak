/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.social.stackoverflow;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;

import org.codehaus.jackson.JsonNode;
import org.jboss.logging.Logger;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.util.SimpleHttp;
import org.keycloak.broker.provider.FederatedIdentity;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.social.SocialIdentityProvider;

/**
 * Stackoverflow social provider. See https://developer.linkedin.com/docs/oauth2
 * 
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class StackoverflowIdentityProvider extends AbstractOAuth2IdentityProvider<StackOverflowIdentityProviderConfig>
		implements SocialIdentityProvider<StackOverflowIdentityProviderConfig> {

	private static final Logger log = Logger.getLogger(StackoverflowIdentityProvider.class);

	public static final String AUTH_URL = "https://stackexchange.com/oauth";
	public static final String TOKEN_URL = "https://stackexchange.com/oauth/access_token";
	public static final String PROFILE_URL = "https://api.stackexchange.com/2.2/me?order=desc&sort=name&site=stackoverflow";
	public static final String DEFAULT_SCOPE = "";

	public StackoverflowIdentityProvider(StackOverflowIdentityProviderConfig config) {
		super(config);
		config.setAuthorizationUrl(AUTH_URL);
		config.setTokenUrl(TOKEN_URL);
		config.setUserInfoUrl(PROFILE_URL);
	}

	@Override
	protected FederatedIdentity doGetFederatedIdentity(String accessToken) {
		log.debug("doGetFederatedIdentity()");
		try {

			String URL = PROFILE_URL + "&access_token=" + accessToken + "&key=" + getConfig().getKey();
			if (log.isDebugEnabled()) {
				log.debug("StackOverflow profile request to: " + URL);
			}
			JsonNode profile = SimpleHttp.doGet(URL).asJson().get("items").get(0);

			FederatedIdentity user = new FederatedIdentity(getJsonProperty(profile, "user_id"));

			user.setUsername(extractUsernameFromProfileURL(getJsonProperty(profile, "link")));
			// TODO username contains html encoding of national chracters sometimes
			user.setName(unescapeHtml3(getJsonProperty(profile, "display_name")));
			// email is not provided
			// user.setEmail(getJsonProperty(profile, "email"));

			return user;
		} catch (Exception e) {
			throw new IdentityBrokerException("Could not obtain user profile from Stackoverflow.", e);
		}
	}

	protected static String extractUsernameFromProfileURL(String profileURL) {
		if (isNotBlank(profileURL)) {

			try {
				log.debug("go to extract username from profile URL " + profileURL);
				URL u = new URL(profileURL);
				String path = u.getPath();
				if (isNotBlank(path) && path.length() > 1) {
					if (path.startsWith("/")) {
						path = path.substring(1);
					}
					String[] pe = path.split("/");
					if (pe.length >= 3) {
						return URLDecoder.decode(pe[2], "UTF-8");
					} else {
						log.warn("Stackoverflow profile URL path is without second part: " + profileURL);
					}
				} else {
					log.warn("Stackoverflow profile URL is without path part: " + profileURL);
				}
			} catch (MalformedURLException e) {
				log.warn("Stackoverflow profile URL is malformed: " + profileURL);
			} catch (Exception e) {
				log.warn("Stackoverflow profile URL " + profileURL + " username extraction failed due: " + e.getMessage());
			}
		}
		return null;
	}

	private static boolean isNotBlank(String s) {
		return s != null && s.trim().length() > 0;
	}

	@Override
	protected String getDefaultScopes() {
		return DEFAULT_SCOPE;
	}

	public static final String unescapeHtml3(final String input) {
		if (input == null)
			return null;
		StringWriter writer = null;
		int len = input.length();
		int i = 1;
		int st = 0;
		while (true) {
			// look for '&'
			while (i < len && input.charAt(i - 1) != '&')
				i++;
			if (i >= len)
				break;

			// found '&', look for ';'
			int j = i;
			while (j < len && j < i + MAX_ESCAPE + 1 && input.charAt(j) != ';')
				j++;
			if (j == len || j < i + MIN_ESCAPE || j == i + MAX_ESCAPE + 1) {
				i++;
				continue;
			}

			// found escape
			if (input.charAt(i) == '#') {
				// numeric escape
				int k = i + 1;
				int radix = 10;

				final char firstChar = input.charAt(k);
				if (firstChar == 'x' || firstChar == 'X') {
					k++;
					radix = 16;
				}

				try {
					int entityValue = Integer.parseInt(input.substring(k, j), radix);

					if (writer == null)
						writer = new StringWriter(input.length());
					writer.append(input.substring(st, i - 1));

					if (entityValue > 0xFFFF) {
						final char[] chrs = Character.toChars(entityValue);
						writer.write(chrs[0]);
						writer.write(chrs[1]);
					} else {
						writer.write(entityValue);
					}

				} catch (NumberFormatException ex) {
					i++;
					continue;
				}
			} else {
				// named escape
				CharSequence value = lookupMap.get(input.substring(i, j));
				if (value == null) {
					i++;
					continue;
				}

				if (writer == null)
					writer = new StringWriter(input.length());
				writer.append(input.substring(st, i - 1));

				writer.append(value);
			}

			// skip escape
			st = j + 1;
			i = st;
		}

		if (writer != null) {
			writer.append(input.substring(st, len));
			return writer.toString();
		}
		return input;
	}

	private static final String[][] ESCAPES = { { "\"", "quot" }, // " - double-quote
			{ "&", "amp" }, // & - ampersand
			{ "<", "lt" }, // < - less-than
			{ ">", "gt" }, // > - greater-than

			// Mapping to escape ISO-8859-1 characters to their named HTML 3.x equivalents.
			{ "\u00A0", "nbsp" }, // non-breaking space
			{ "\u00A1", "iexcl" }, // inverted exclamation mark
			{ "\u00A2", "cent" }, // cent sign
			{ "\u00A3", "pound" }, // pound sign
			{ "\u00A4", "curren" }, // currency sign
			{ "\u00A5", "yen" }, // yen sign = yuan sign
			{ "\u00A6", "brvbar" }, // broken bar = broken vertical bar
			{ "\u00A7", "sect" }, // section sign
			{ "\u00A8", "uml" }, // diaeresis = spacing diaeresis
			{ "\u00A9", "copy" }, // © - copyright sign
			{ "\u00AA", "ordf" }, // feminine ordinal indicator
			{ "\u00AB", "laquo" }, // left-pointing double angle quotation mark = left pointing guillemet
			{ "\u00AC", "not" }, // not sign
			{ "\u00AD", "shy" }, // soft hyphen = discretionary hyphen
			{ "\u00AE", "reg" }, // ® - registered trademark sign
			{ "\u00AF", "macr" }, // macron = spacing macron = overline = APL overbar
			{ "\u00B0", "deg" }, // degree sign
			{ "\u00B1", "plusmn" }, // plus-minus sign = plus-or-minus sign
			{ "\u00B2", "sup2" }, // superscript two = superscript digit two = squared
			{ "\u00B3", "sup3" }, // superscript three = superscript digit three = cubed
			{ "\u00B4", "acute" }, // acute accent = spacing acute
			{ "\u00B5", "micro" }, // micro sign
			{ "\u00B6", "para" }, // pilcrow sign = paragraph sign
			{ "\u00B7", "middot" }, // middle dot = Georgian comma = Greek middle dot
			{ "\u00B8", "cedil" }, // cedilla = spacing cedilla
			{ "\u00B9", "sup1" }, // superscript one = superscript digit one
			{ "\u00BA", "ordm" }, // masculine ordinal indicator
			{ "\u00BB", "raquo" }, // right-pointing double angle quotation mark = right pointing guillemet
			{ "\u00BC", "frac14" }, // vulgar fraction one quarter = fraction one quarter
			{ "\u00BD", "frac12" }, // vulgar fraction one half = fraction one half
			{ "\u00BE", "frac34" }, // vulgar fraction three quarters = fraction three quarters
			{ "\u00BF", "iquest" }, // inverted question mark = turned question mark
			{ "\u00C0", "Agrave" }, // А - uppercase A, grave accent
			{ "\u00C1", "Aacute" }, // Б - uppercase A, acute accent
			{ "\u00C2", "Acirc" }, // В - uppercase A, circumflex accent
			{ "\u00C3", "Atilde" }, // Г - uppercase A, tilde
			{ "\u00C4", "Auml" }, // Д - uppercase A, umlaut
			{ "\u00C5", "Aring" }, // Е - uppercase A, ring
			{ "\u00C6", "AElig" }, // Ж - uppercase AE
			{ "\u00C7", "Ccedil" }, // З - uppercase C, cedilla
			{ "\u00C8", "Egrave" }, // И - uppercase E, grave accent
			{ "\u00C9", "Eacute" }, // Й - uppercase E, acute accent
			{ "\u00CA", "Ecirc" }, // К - uppercase E, circumflex accent
			{ "\u00CB", "Euml" }, // Л - uppercase E, umlaut
			{ "\u00CC", "Igrave" }, // М - uppercase I, grave accent
			{ "\u00CD", "Iacute" }, // Н - uppercase I, acute accent
			{ "\u00CE", "Icirc" }, // О - uppercase I, circumflex accent
			{ "\u00CF", "Iuml" }, // П - uppercase I, umlaut
			{ "\u00D0", "ETH" }, // Р - uppercase Eth, Icelandic
			{ "\u00D1", "Ntilde" }, // С - uppercase N, tilde
			{ "\u00D2", "Ograve" }, // Т - uppercase O, grave accent
			{ "\u00D3", "Oacute" }, // У - uppercase O, acute accent
			{ "\u00D4", "Ocirc" }, // Ф - uppercase O, circumflex accent
			{ "\u00D5", "Otilde" }, // Х - uppercase O, tilde
			{ "\u00D6", "Ouml" }, // Ц - uppercase O, umlaut
			{ "\u00D7", "times" }, // multiplication sign
			{ "\u00D8", "Oslash" }, // Ш - uppercase O, slash
			{ "\u00D9", "Ugrave" }, // Щ - uppercase U, grave accent
			{ "\u00DA", "Uacute" }, // Ъ - uppercase U, acute accent
			{ "\u00DB", "Ucirc" }, // Ы - uppercase U, circumflex accent
			{ "\u00DC", "Uuml" }, // Ь - uppercase U, umlaut
			{ "\u00DD", "Yacute" }, // Э - uppercase Y, acute accent
			{ "\u00DE", "THORN" }, // Ю - uppercase THORN, Icelandic
			{ "\u00DF", "szlig" }, // Я - lowercase sharps, German
			{ "\u00E0", "agrave" }, // а - lowercase a, grave accent
			{ "\u00E1", "aacute" }, // б - lowercase a, acute accent
			{ "\u00E2", "acirc" }, // в - lowercase a, circumflex accent
			{ "\u00E3", "atilde" }, // г - lowercase a, tilde
			{ "\u00E4", "auml" }, // д - lowercase a, umlaut
			{ "\u00E5", "aring" }, // е - lowercase a, ring
			{ "\u00E6", "aelig" }, // ж - lowercase ae
			{ "\u00E7", "ccedil" }, // з - lowercase c, cedilla
			{ "\u00E8", "egrave" }, // и - lowercase e, grave accent
			{ "\u00E9", "eacute" }, // й - lowercase e, acute accent
			{ "\u00EA", "ecirc" }, // к - lowercase e, circumflex accent
			{ "\u00EB", "euml" }, // л - lowercase e, umlaut
			{ "\u00EC", "igrave" }, // м - lowercase i, grave accent
			{ "\u00ED", "iacute" }, // н - lowercase i, acute accent
			{ "\u00EE", "icirc" }, // о - lowercase i, circumflex accent
			{ "\u00EF", "iuml" }, // п - lowercase i, umlaut
			{ "\u00F0", "eth" }, // р - lowercase eth, Icelandic
			{ "\u00F1", "ntilde" }, // с - lowercase n, tilde
			{ "\u00F2", "ograve" }, // т - lowercase o, grave accent
			{ "\u00F3", "oacute" }, // у - lowercase o, acute accent
			{ "\u00F4", "ocirc" }, // ф - lowercase o, circumflex accent
			{ "\u00F5", "otilde" }, // х - lowercase o, tilde
			{ "\u00F6", "ouml" }, // ц - lowercase o, umlaut
			{ "\u00F7", "divide" }, // division sign
			{ "\u00F8", "oslash" }, // ш - lowercase o, slash
			{ "\u00F9", "ugrave" }, // щ - lowercase u, grave accent
			{ "\u00FA", "uacute" }, // ъ - lowercase u, acute accent
			{ "\u00FB", "ucirc" }, // ы - lowercase u, circumflex accent
			{ "\u00FC", "uuml" }, // ь - lowercase u, umlaut
			{ "\u00FD", "yacute" }, // э - lowercase y, acute accent
			{ "\u00FE", "thorn" }, // ю - lowercase thorn, Icelandic
			{ "\u00FF", "yuml" }, // я - lowercase y, umlaut
	};

	private static final int MIN_ESCAPE = 2;
	private static final int MAX_ESCAPE = 6;

	private static final HashMap<String, CharSequence> lookupMap;
	static {
		lookupMap = new HashMap<String, CharSequence>();
		for (final CharSequence[] seq : ESCAPES)
			lookupMap.put(seq[1].toString(), seq[0]);
	}
}
