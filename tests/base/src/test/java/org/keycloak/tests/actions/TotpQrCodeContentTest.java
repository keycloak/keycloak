/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.tests.actions;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import javax.imageio.ImageIO;

import org.keycloak.models.UserModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.tests.utils.PasswordGenerateUtil;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class TotpQrCodeContentTest {

    private static final String PASSWORD = PasswordGenerateUtil.generatePassword();

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectUser(ref = "totpUser", config = TotpUserConfig.class)
    ManagedUser totpUser;

    @InjectUser(ref = "emailUsernameMismatchUser", config = EmailUsernameMismatchUserConfig.class)
    ManagedUser emailUsernameMismatchUser;

    @InjectUser(ref = "noEmailUser", config = NoEmailUserConfig.class)
    ManagedUser noEmailUser;

    @InjectPage
    LoginPage loginPage;

    @InjectPage
    LoginConfigTotpPage totpPage;

    @Test
    public void qrCodeIssuerUsesDisplayNameShortWhenSet() {
        realm.updateWithCleanup(r -> r.displayName("Full Display Name").displayNameShort("Short Name"));

        KeyUri keyUri = loginAndDecodeQrCode(totpUser);

        assertEquals("Short Name", keyUri.issuer());
        assertEquals(totpUser.getUsername(), keyUri.accountName());
    }

    @Test
    public void qrCodeIssuerFallsBackToDisplayNameWhenNoDisplayNameShort() {
        realm.updateWithCleanup(r -> r.displayName("Full Display Name"));

        KeyUri keyUri = loginAndDecodeQrCode(totpUser);

        assertEquals("Full Display Name", keyUri.issuer());
    }

    @Test
    public void qrCodeAccountNameUsesEmailWhenRegistrationEmailAsUsernameEnabled() {
        realm.updateWithCleanup(r -> r.registrationEmailAsUsername(true));

        KeyUri keyUri = loginAndDecodeQrCode(emailUsernameMismatchUser);

        assertEquals(emailUsernameMismatchUser.getEmail(), keyUri.accountName());
    }

    @Test
    public void qrCodeAccountNameFallsBackToUsernameWhenEmailMissingAndRegistrationEmailAsUsernameEnabled() {
        realm.updateWithCleanup(r -> r.registrationEmailAsUsername(true));

        KeyUri keyUri = loginAndDecodeQrCode(noEmailUser);

        assertEquals(noEmailUser.getUsername(), keyUri.accountName());
    }

    @Test
    public void qrCodeIssuerResolvesLocalizationKeyInDisplayNameShort() {
        String key = "qrCodeTestIssuerKey";
        String localizedIssuer = "Localized Issuer Name";

        realm.updateWithCleanup(r -> r.internationalizationEnabled(true).supportedLocales("en").defaultLocale("en")
                .displayNameShort("${" + key + "}"));
        realm.cleanup().add(r -> r.localization().deleteRealmLocalizationText("en", key));
        realm.admin().localization().saveRealmLocalizationText("en", key, localizedIssuer);

        KeyUri keyUri = loginAndDecodeQrCode(totpUser);

        assertEquals(localizedIssuer, keyUri.issuer());
    }

    private KeyUri loginAndDecodeQrCode(ManagedUser user) {
        oauth.openLoginForm();
        loginPage.fillLogin(user.getUsername(), user.getPassword());
        loginPage.submit();

        totpPage.assertCurrent();

        String content = decodeQrCode(totpPage.getTotpQrCodeSrc());
        return KeyUri.parse(content);
    }

    private static String decodeQrCode(String dataUri) {
        String base64Content = dataUri.substring(dataUri.indexOf(',') + 1).trim();
        byte[] imageBytes = Base64.getDecoder().decode(base64Content);

        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
            LuminanceSource source = new BufferedImageLuminanceSource(image);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            Result result = new QRCodeReader().decode(bitmap);
            return result.getText();
        } catch (IOException | ReaderException e) {
            throw new RuntimeException("Failed to decode QR code", e);
        }
    }

    private record KeyUri(String issuer, String accountName) {

        static KeyUri parse(String keyUri) {
            URI uri = URI.create(keyUri);

            String label = uri.getPath().substring(1);
            String issuer = label.substring(0, label.indexOf(':'));
            String accountName = label.substring(label.indexOf(':') + 1);

            return new KeyUri(issuer, accountName);
        }
    }

    private static class TotpUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("qr-totp-user")
                    .email("qr-totp-user@localhost")
                    .name("Qr", "Totp")
                    .password(PASSWORD)
                    .emailVerified(true)
                    .requiredActions(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        }
    }

    private static class EmailUsernameMismatchUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("qr-totp-diff-user")
                    .email("qr-totp-diff-user@example.com")
                    .name("Qr", "Totp")
                    .password(PASSWORD)
                    .emailVerified(true)
                    .requiredActions(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        }
    }

    private static class NoEmailUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("qr-totp-no-email-user")
                    .name("Qr", "Totp")
                    .password(PASSWORD)
                    .requiredActions(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        }
    }

}
