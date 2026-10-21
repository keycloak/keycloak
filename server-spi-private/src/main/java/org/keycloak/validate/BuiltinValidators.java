/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.keycloak.validate;

import org.keycloak.validate.validators.DoubleValidator;
import org.keycloak.validate.validators.EmailValidator;
import org.keycloak.validate.validators.IntegerValidator;
import org.keycloak.validate.validators.IsoDateValidator;
import org.keycloak.validate.validators.LengthValidator;
import org.keycloak.validate.validators.LocalDateValidator;
import org.keycloak.validate.validators.NotBlankValidator;
import org.keycloak.validate.validators.NotEmptyValidator;
import org.keycloak.validate.validators.OptionsValidator;
import org.keycloak.validate.validators.PatternValidator;
import org.keycloak.validate.validators.UriValidator;
import org.keycloak.validate.validators.ValidatorConfigValidator;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class BuiltinValidators {

    public static NotBlankValidator notBlankValidator() {
        return NotBlankValidator.INSTANCE;
    }

    public static NotEmptyValidator notEmptyValidator() {
        return NotEmptyValidator.INSTANCE;
    }

    public static LengthValidator lengthValidator() {
        return LengthValidator.INSTANCE;
    }

    public static UriValidator uriValidator() {
        return UriValidator.INSTANCE;
    }

    public static EmailValidator emailValidator() {
        return EmailValidator.INSTANCE;
    }

    public static PatternValidator patternValidator() {
        return PatternValidator.INSTANCE;
    }

    public static DoubleValidator doubleValidator() {
        return DoubleValidator.INSTANCE;
    }

    public static IntegerValidator integerValidator() {
        return IntegerValidator.INSTANCE;
    }

    public static LocalDateValidator dateValidator() {
        return LocalDateValidator.INSTANCE;
    }

    public static IsoDateValidator isoDateValidator() {
        return IsoDateValidator.INSTANCE;
    }

    public static OptionsValidator optionsValidator() {
        return OptionsValidator.INSTANCE;
    }

    public static ValidatorConfigValidator validatorConfigValidator() {
        return ValidatorConfigValidator.INSTANCE;
    }
}
