import { Button, Page, Spinner } from "@patternfly/react-core";
import { ExternalLinkSquareAltIcon } from "@patternfly/react-icons";
import {
  KeycloakMasthead,
  KeycloakProvider,
  Translations,
  TranslationsProvider,
} from "keycloak-masthead";
import { Suspense, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Outlet, useHref } from "react-router-dom";
import { AlertProvider, Help } from "ui-shared";
import { environment } from "../environment";
import { keycloak } from "../keycloak";
import { joinPath } from "../utils/joinPath";
import { PageNav } from "./PageNav";

import style from "./Root.module.css";

const ReferrerLink = () => {
  const { t } = useTranslation();
  const searchParams = new URLSearchParams(location.search);

  return searchParams.has("referrer_uri") ? (
    <Button
      component="a"
      href={searchParams.get("referrer_uri")!.replace("_hash_", "#")}
      variant="link"
      icon={<ExternalLinkSquareAltIcon />}
      iconPosition="right"
      isInline
    >
      {t("backTo", { app: searchParams.get("referrer") })}
    </Button>
  ) : null;
};

export const Root = () => {
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
    <KeycloakProvider keycloak={keycloak}>
      <Page
        header={
          <TranslationsProvider translations={translations}>
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
          </TranslationsProvider>
        }
        sidebar={<PageNav />}
        isManagedSidebar
      >
        <AlertProvider>
          <Help>
            <Suspense fallback={<Spinner />}>
              <Outlet />
            </Suspense>
          </Help>
        </AlertProvider>
      </Page>
    </KeycloakProvider>
  );
};
