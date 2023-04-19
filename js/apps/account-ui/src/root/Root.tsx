import { Page, Spinner } from "@patternfly/react-core";
import {
  KeycloakMasthead,
  Translations,
  TranslationsProvider,
} from "keycloak-masthead";
import { Suspense, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Outlet } from "react-router-dom";
import { AlertProvider } from "ui-shared";

import { environment } from "../environment";
import { keycloak } from "../keycloak";
import { joinPath } from "../utils/joinPath";
import { PageNav } from "./PageNav";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";

import style from "./Root.module.css";

const ReferrerLink = () => {
  const { t } = useTranslation();
  const searchParams = new URLSearchParams(location.search);

  return searchParams.has("referrer_uri") ? (
    <a
      href={searchParams.get("referrer_uri")!.replace("_hash_", "#")}
      className="pf-m-link pf-m-inline"
    >
      <ExternalLinkAltIcon />{" "}
      {t("backTo", { app: searchParams.get("referrer") })}
    </a>
  ) : null;
};

export const Root = () => {
  const { t } = useTranslation();

  const translations = useMemo<Translations>(
    () => ({
      avatar: t("avatar"),
      fullName: t("fullName"),
      manageAccount: t("manageAccount"),
      signOut: t("signOut"),
      unknownUser: t("unknownUser"),
    }),
    [t]
  );

  return (
    <Page
      header={
        <TranslationsProvider translations={translations}>
          <KeycloakMasthead
            features={{ hasManageAccount: false }}
            showNavToggle
            brand={{
              src: joinPath(environment.resourceUrl, "logo.svg"),
              alt: t("logo"),
              className: style.brand,
            }}
            toolbarItems={[<ReferrerLink key="link" />]}
            keycloak={keycloak}
          />
        </TranslationsProvider>
      }
      sidebar={<PageNav />}
      isManagedSidebar
    >
      <AlertProvider>
        <Suspense fallback={<Spinner />}>
          <Outlet />
        </Suspense>
      </AlertProvider>
    </Page>
  );
};
