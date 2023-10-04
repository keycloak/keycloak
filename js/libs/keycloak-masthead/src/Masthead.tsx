import {
  Avatar,
  AvatarProps,
  Brand,
  BrandProps,
  DropdownItem,
  PageHeader,
  PageHeaderProps,
  PageHeaderTools,
  PageHeaderToolsGroup,
  PageHeaderToolsItem,
} from "@patternfly/react-core";
import { ReactNode } from "react";

import { KeycloakDropdown } from "./KeycloakDropdown";
import { useTranslation } from "./translation/useTranslation";
import { loggedInUserName } from "./util";
import { DefaultAvatar } from "./DefaultAvatar";
import { useKeycloak } from "./KeycloakContext";

type BrandLogo = BrandProps & {
  href: string;
};

type KeycloakMastheadProps = PageHeaderProps & {
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
  const { keycloak } = useKeycloak()!;
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

  const picture = keycloak.tokenParsed?.picture;
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
                    ? loggedInUserName(keycloak.tokenParsed, t)
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
