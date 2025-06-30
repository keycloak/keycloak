import {
  Avatar,
  AvatarProps,
  Brand,
  BrandProps,
  DropdownItem,
} from "@patternfly/react-core";
import {
  PageHeader,
  PageHeaderProps,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem,
} from "@patternfly/react-core/deprecated";
import { TFunction } from "i18next";
import Keycloak, { type KeycloakTokenParsed } from "keycloak-js";
import { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { DefaultAvatar } from "./DefaultAvatar";
import { KeycloakDropdown } from "./KeycloakDropdown";

function loggedInUserName(
  token: KeycloakTokenParsed | undefined,
  t: TFunction,
) {
  if (!token) {
    return t("unknownUser");
  }

  const givenName = token.given_name;
  const familyName = token.family_name;
  const preferredUsername = token.preferred_username;

  if (givenName && familyName) {
    return t("fullName", { givenName, familyName });
  }

  return givenName || familyName || preferredUsername || t("unknownUser");
}

type BrandLogo = BrandProps & {
  href: string;
};

type KeycloakMastheadProps = PageHeaderProps & {
  keycloak: Keycloak;
  brand: BrandLogo;
  avatar?: AvatarProps;
  features?: {
    hasLogout?: boolean;
    hasManageAccount?: boolean;
    hasUsername?: boolean;
  };
  kebabDropdownItems?: ReactNode[];
  dropdownItems?: ReactNode[];
  toolbarItems?: ReactNode[];
};

const KeycloakMasthead = ({
  keycloak,
  brand: { href: brandHref, ...brandProps },
  avatar,
  features: {
    hasLogout = true,
    hasManageAccount = true,
    hasUsername = true,
  } = {},
  kebabDropdownItems,
  dropdownItems = [],
  toolbarItems,
  ...rest
}: KeycloakMastheadProps) => {
  const { t } = useTranslation();
  const extraItems = [];
  if (hasManageAccount) {
    extraItems.push(
      <DropdownItem
        key="manageAccount"
        onClick={() => keycloak.accountManagement()}
      >
        {t("manageAccount")}
      </DropdownItem>,
    );
  }
  if (hasLogout) {
    extraItems.push(
      <DropdownItem key="signOut" onClick={() => keycloak.logout()}>
        {t("signOut")}
      </DropdownItem>,
    );
  }

  const picture = keycloak.idTokenParsed?.picture;
  return (
    <PageHeader
      {...rest}
      logo={<Brand {...brandProps} />}
      logoProps={{ href: brandHref }}
      headerTools={
        <PageHeaderTools>
          <PageHeaderToolsGroup>
            <PageHeaderToolsItem
              visibility={{
                md: "hidden",
              }}
            >
              <KeycloakDropdown
                data-testid="options-kebab"
                isKebab
                dropDownItems={[
                  ...(kebabDropdownItems || dropdownItems),
                  extraItems,
                ]}
              />
            </PageHeaderToolsItem>
            <PageHeaderToolsItem>{toolbarItems}</PageHeaderToolsItem>
            <PageHeaderToolsItem
              visibility={{
                default: "hidden",
                md: "visible",
              }}
            >
              <KeycloakDropdown
                data-testid="options"
                dropDownItems={[...dropdownItems, extraItems]}
                title={
                  hasUsername
                    ? loggedInUserName(keycloak.idTokenParsed, t)
                    : undefined
                }
              />
            </PageHeaderToolsItem>
          </PageHeaderToolsGroup>
          {picture || avatar?.src ? (
            <Avatar {...{ src: picture, alt: t("avatar"), ...avatar }} />
          ) : (
            <DefaultAvatar {...avatar} />
          )}
        </PageHeaderTools>
      }
    />
  );
};

export default KeycloakMasthead;
