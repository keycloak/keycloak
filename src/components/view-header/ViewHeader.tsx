import {
  Badge,
  Divider,
  Dropdown,
  DropdownPosition,
  DropdownToggle,
  Level,
  LevelItem,
  PageSection,
  Switch,
  Text,
  TextContent,
  Toolbar,
  ToolbarContent,
  ToolbarItem,
} from "@patternfly/react-core";
import React, { ReactElement, ReactNode, useState } from "react";
import { useTranslation } from "react-i18next";
import {
  FormattedLink,
  FormattedLinkProps,
} from "../external-link/FormattedLink";
import { useHelp } from "../help-enabler/HelpHeader";
import { HelpItem } from "../help-enabler/HelpItem";

export type ViewHeaderProps = {
  titleKey: string;
  badge?: string;
  badgeId?: string;
  badgeIsRead?: boolean;
  subKey?: string | ReactNode;
  actionsDropdownId?: string;
  subKeyLinkProps?: FormattedLinkProps;
  dropdownItems?: ReactElement[];
  lowerDropdownItems?: any;
  lowerDropdownMenuTitle?: any;
  isEnabled?: boolean;
  onToggle?: (value: boolean) => void;
  divider?: boolean;
  helpTextKey?: string;
};

export const ViewHeader = ({
  actionsDropdownId,
  titleKey,
  badge,
  badgeIsRead,
  subKey,
  subKeyLinkProps,
  dropdownItems,
  lowerDropdownMenuTitle,
  lowerDropdownItems,
  isEnabled = true,
  onToggle,
  divider = true,
  helpTextKey,
}: ViewHeaderProps) => {
  const { t } = useTranslation();
  const { enabled } = useHelp();
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
                  <Badge
                    data-testid="composite-role-badge"
                    isRead={badgeIsRead}
                  >
                    {badge}
                  </Badge>
                </LevelItem>
              )}
            </Level>
          </LevelItem>
          <LevelItem></LevelItem>
          <LevelItem>
            <Toolbar className="pf-u-p-0">
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
                    {helpTextKey && (
                      <HelpItem
                        helpText={t(helpTextKey)}
                        forLabel={t("common:enabled")}
                        forID={`${titleKey}-switch`}
                      />
                    )}
                  </ToolbarItem>
                )}
                {dropdownItems && (
                  <ToolbarItem>
                    <Dropdown
                      position={DropdownPosition.right}
                      toggle={
                        <DropdownToggle
                          id={actionsDropdownId}
                          onToggle={onDropdownToggle}
                        >
                          {t("common:action")}
                        </DropdownToggle>
                      }
                      isOpen={isDropdownOpen}
                      dropdownItems={dropdownItems}
                      data-testid="action-dropdown"
                    />
                  </ToolbarItem>
                )}
              </ToolbarContent>
            </Toolbar>
          </LevelItem>
        </Level>
        {enabled && (
          <TextContent id="view-header-subkey">
            <Text>
              {React.isValidElement(subKey)
                ? subKey
                : subKey
                ? t(subKey as string)
                : ""}
              {subKeyLinkProps && (
                <FormattedLink
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
      {divider && <Divider component="div" />}
    </>
  );
};
