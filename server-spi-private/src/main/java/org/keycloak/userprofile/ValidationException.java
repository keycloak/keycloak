/*
 *
 *  * Copyright 2021  Red Hat, Inc. and/or its affiliates
 *  * and other contributors as indicated by the @author tags.
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.keycloak.userprofile;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.theme.Theme;
import org.keycloak.validate.ValidationError;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public final class ValidationException extends RuntimeException implements Consumer<ValidationError> {

	private final Map<String, List<Error>> errors = new HashMap<>();
	private final BiFunction<String, Object[], String> messageFormatter;

	public ValidationException(KeycloakSession session, UserModel user) {
		this.messageFormatter = new MessageFormatter(session, user);
	}

	public List<Error> getErrors() {
		return errors.values().stream().reduce(new ArrayList<>(), (l, r) -> {
			l.addAll(r);
			return l;
		}, (l, r) -> l);
	}

	public boolean hasError(String... types) {
		if (types.length == 0) {
			return !errors.isEmpty();
		}

		for (String type : types) {
			if (errors.containsKey(type)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Checks if there are validation errors related to the attribute with the given {@code name}.
	 *
	 * @param name
	 * @return
	 */
	public boolean isAttributeOnError(String... name) {
		if (name.length == 0) {
			return !errors.isEmpty();
		}

		List<String> names = Arrays.asList(name);

		return errors.values().stream().flatMap(Collection::stream).anyMatch(error -> names.contains(error.getAttribute()));
	}

	@Override
	public void accept(ValidationError error) {
		addError(error);
	}

	void addError(ValidationError error) {
		List<Error> errors = this.errors.computeIfAbsent(error.getMessage(), (k) -> new ArrayList<>());
		errors.add(new Error(error, messageFormatter));
	}

	@Override
	public String toString() {
		return "ValidationException [errors=" + errors + "]";
	}

	@Override
	public String getMessage() {
		return toString();
	}

	public Response.Status getStatusCode() {
		for (Map.Entry<String, List<Error>> entry : errors.entrySet()) {
			for (Error error : entry.getValue()) {
				if (!Response.Status.BAD_REQUEST.equals(error.getStatusCode())) {
					return error.getStatusCode();
				}
			}
		}
		return Response.Status.BAD_REQUEST;
	}

	public static class Error implements Serializable {

		private final ValidationError error;
		private final BiFunction<String, Object[], String> messageFormatter;

		public Error(ValidationError error, BiFunction<String, Object[], String> messageFormatter) {
			this.error = error;
			this.messageFormatter = messageFormatter;
		}

		public String getAttribute() {
			return error.getInputHint();
		}

		public String getMessage() {
			return error.getMessage();
		}
		
		public Object[] getMessageParameters() {
			return error.getInputHintWithMessageParameters();
		}

		@Override
		public String toString() {
			return "Error [error=" + error + "]";
		}

		public String getFormattedMessage() {
			return messageFormatter.apply(getMessage(), getMessageParameters());
		}

		public Response.Status getStatusCode() {
			return error.getStatusCode();
		}
	}

    private final class MessageFormatter implements BiFunction<String, Object[], String> {

		private final Locale locale;
		private final Properties messages;

		public MessageFormatter(KeycloakSession session, UserModel user) {
			try {
				KeycloakContext context = session.getContext();
				locale = context.resolveLocale(user);
				messages = getTheme(session).getMessages(locale);
				RealmModel realm = context.getRealm();
				Map<String, String> localizationTexts = realm.getRealmLocalizationTextsByLocale(locale.toLanguageTag());
				messages.putAll(localizationTexts);
			} catch (IOException cause) {
				throw new RuntimeException("Failed to configure error messages", cause);
			}
		}

		private Theme getTheme(KeycloakSession session) throws IOException {
			return session.theme().getTheme(Theme.Type.ADMIN);
		}

		@Override
		public String apply(String s, Object[] objects) {
			return new MessageFormat(messages.getProperty(s, s), locale).format(objects);
		}
	}
}
