import {
  KeycloakMasthead,
  KeycloakProvider,
  Translations,
  TranslationsProvider,
} from "keycloak-masthead";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useHref } from "react-router-dom";
import { useEnvironment } from "./KeycloakContext";
import { joinPath } from "../utils/joinPath";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import { Button } from "@patternfly/react-core";

import style from "./header.module.css";
import { environment } from "../environment";

const ReferrerLink = () => {
  const { t } = useTranslation();

  return environment.referrer_uri ? (
    <Button
      data-testid="referrer-link"
      component="a"
      href={environment.referrer_uri!.replace("_hash_", "#")}
      variant="link"
      icon={<ExternalLinkSquareAltIcon />}
      iconPosition="right"
      isInline
    >
      {t("backTo", { app: environment.referrer })}
    </Button>
  ) : null;
};

export const Header = () => {
  const { environment, keycloak } = useEnvironment();
  const { t } = useTranslation();

  const brandImage = environment.logo || "logo.svg";
  const logoUrl = environment.logoUrl ? environment.logoUrl : "/";
  const internalLogoHref = useHref(logoUrl);

  // User can indicate that he wants an internal URL by starting it with "/"
  const indexHref = logoUrl.startsWith("/") ? internalLogoHref : logoUrl;
  const translations = useMemo<Translations>(
    () => ({
      avatar: t("avatar"),
      fullName: t("fullName"),
      manageAccount: t("manageAccount"),
      signOut: t("signOut"),
      unknownUser: t("unknownUser"),
    }),
    [t],
  );

  return (
    <TranslationsProvider translations={translations}>
      <KeycloakProvider keycloak={keycloak}>
        <KeycloakMasthead
          features={{ hasManageAccount: false }}
          showNavToggle
          brand={{
            href: indexHref,
            src: joinPath(environment.resourceUrl, brandImage),
            alt: t("logo"),
            className: style.brand,
          }}
          toolbarItems={[<ReferrerLink key="link" />]}
        />
      </KeycloakProvider>
    </TranslationsProvider>
  );
};
