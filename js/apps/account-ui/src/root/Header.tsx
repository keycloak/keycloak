import { Button } from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import {
  KeycloakMasthead,
  KeycloakProvider,
  Translations,
  TranslationsProvider,
} from "keycloak-masthead";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { useHref } from "react-router-dom";
import { label } from "ui-shared";

import { environment } from "../environment";
import { joinPath } from "../utils/joinPath";
import { useEnvironment } from "./KeycloakContext";

import style from "./header.module.css";

const ReferrerLink = () => {
  const { t } = useTranslation();

  return environment.referrerUrl ? (
    <Button
      data-testid="referrer-link"
      component="a"
      href={environment.referrerUrl.replace("_hash_", "#")}
      variant="link"
      icon={<ExternalLinkSquareAltIcon />}
      iconPosition="right"
      isInline
    >
      {t("backTo", {
        app: label(t, environment.referrerName, environment.referrerUrl),
      })}
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
