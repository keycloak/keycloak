import React, { ReactElement, useContext, useState } from "react";
import {
  Text,
  PageSection,
  TextContent,
  Divider,
  Level,
  LevelItem,
  Switch,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
  Badge,
  ButtonProps,
  Dropdown,
  DropdownToggle,
  DropdownPosition,
} from "@patternfly/react-core";
import { HelpContext } from "../help-enabler/HelpHeader";
import { useTranslation } from "react-i18next";
import { ExternalLink } from "../external-link/ExternalLink";

export type ViewHeaderProps = {
  titleKey: string;
  badge?: string;
  subKey: string;
  subKeyLinkProps?: ButtonProps;
  dropdownItems?: ReactElement[];
  lowerDropdownItems?: any;
  lowerDropdownMenuTitle?: any;
  isEnabled?: boolean;
  onToggle?: (value: boolean) => void;
};

export const ViewHeader = ({
  titleKey,
  badge,
  subKey,
  subKeyLinkProps,
  dropdownItems,
  lowerDropdownMenuTitle,
  lowerDropdownItems,
  isEnabled = true,
  onToggle,
}: ViewHeaderProps) => {
  const { t } = useTranslation();
  const { enabled } = useContext(HelpContext);
  const [isDropdownOpen, setDropdownOpen] = useState(false);
  const [isLowerDropdownOpen, setIsLowerDropdownOpen] = useState(false);

  const onDropdownToggle = () => {
    setDropdownOpen(!isDropdownOpen);
  };

  const onLowerDropdownToggle = () => {
    setIsLowerDropdownOpen(!isLowerDropdownOpen);
  };

  return (
    <>
      <PageSection variant="light">
        <Level hasGutter>
          <LevelItem>
            <Level>
              <LevelItem>
                <TextContent className="pf-u-mr-sm">
                  <Text component="h1">{t(titleKey)}</Text>
                </TextContent>
              </LevelItem>
              {badge && (
                <LevelItem>
                  <Badge>{badge}</Badge>
                </LevelItem>
              )}
            </Level>
          </LevelItem>
          <LevelItem></LevelItem>
          <LevelItem>
            <Toolbar>
              <ToolbarContent>
                {onToggle && (
                  <ToolbarItem>
                    <Switch
                      id={`${titleKey}-switch`}
                      label={t("common:enabled")}
                      labelOff={t("common:disabled")}
                      className="pf-u-mr-lg"
                      isChecked={isEnabled}
                      onChange={(value) => {
                        if (onToggle) {
                          onToggle(value);
                        }
                      }}
                    />
                  </ToolbarItem>
                )}
                {dropdownItems && (
                  <ToolbarItem>
                    <Dropdown
                      position={DropdownPosition.right}
                      toggle={
                        <DropdownToggle onToggle={onDropdownToggle}>
                          {t("common:action")}
                        </DropdownToggle>
                      }
                      isOpen={isDropdownOpen}
                      dropdownItems={dropdownItems}
                    />
                  </ToolbarItem>
                )}
              </ToolbarContent>
            </Toolbar>
          </LevelItem>
        </Level>
        {enabled && (
          <TextContent>
            <Text>
              {t(subKey)}
              {subKeyLinkProps && (
                <ExternalLink
                  {...subKeyLinkProps}
                  isInline
                  className="pf-u-ml-md"
                />
              )}
            </Text>
          </TextContent>
        )}
        {lowerDropdownItems && (
          <Dropdown
            className="keycloak__user-federation__dropdown"
            toggle={
              <DropdownToggle
                onToggle={() => onLowerDropdownToggle()}
                isPrimary
                id="ufToggleId"
              >
                {t(lowerDropdownMenuTitle)}
              </DropdownToggle>
            }
            isOpen={isLowerDropdownOpen}
            dropdownItems={lowerDropdownItems}
          />
        )}
      </PageSection>
      <Divider />
    </>
  );
};
