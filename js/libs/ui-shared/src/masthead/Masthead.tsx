import {
  Avatar,
  AvatarProps,
  DropdownItem,
  Masthead,
  MastheadBrand,
  MastheadBrandProps,
  MastheadContent,
  MastheadLogo,
  MastheadMain,
  MastheadMainProps,
  MastheadToggle,
  PageToggleButton,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";

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

type BrandLogo = MastheadBrandProps;

type KeycloakMastheadProps = MastheadMainProps & {
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
  toolbar?: ReactNode;
};

const KeycloakMasthead = ({
  keycloak,
  brand: { src, alt, className },
  avatar,
  features: {
    hasLogout = true,
    hasManageAccount = true,
    hasUsername = true,
  } = {},
  kebabDropdownItems,
  dropdownItems = [],
  toolbarItems,
  toolbar,
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
    <Masthead {...rest}>
      <MastheadMain>
        <MastheadToggle>
          <PageToggleButton
            isHamburgerButton
            variant="plain"
            aria-label={t("navigation")}
          ></PageToggleButton>
        </MastheadToggle>
        <MastheadBrand data-codemods>
          <MastheadLogo data-codemods>
            <img src={src} alt={alt} className={className} />
          </MastheadLogo>
        </MastheadBrand>
      </MastheadMain>
      <MastheadContent>
        {toolbar}
        <Toolbar>
          <ToolbarContent>
            {toolbarItems?.map((item, index) => (
              <ToolbarItem key={index} align={{ default: "alignEnd" }}>
                {item}
              </ToolbarItem>
            ))}
            <ToolbarItem
              visibility={{
                default: "hidden",
                md: "visible",
              }} /** this user dropdown is hidden on mobile sizes */
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
            </ToolbarItem>
            <ToolbarItem
              align={{ default: "alignStart" }}
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
            </ToolbarItem>
            <ToolbarItem
              align={{ default: "alignEnd" }}
              className="pf-v6-u-m-0-on-lg"
            >
              {picture || avatar?.src ? (
                <Avatar {...{ src: picture, alt: t("avatar"), ...avatar }} />
              ) : (
                <DefaultAvatar {...avatar} />
              )}
            </ToolbarItem>
          </ToolbarContent>
        </Toolbar>
      </MastheadContent>
    </Masthead>
  );
};

export default KeycloakMasthead;
