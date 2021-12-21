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
import React, {
  ReactElement,
  ReactNode,
  useState,
  isValidElement,
  Fragment,
} from "react";
import { useTranslation } from "react-i18next";
import { FormattedLink } from "../external-link/FormattedLink";
import { useHelp } from "../help-enabler/HelpHeader";
import { HelpItem } from "../help-enabler/HelpItem";
import "../../help-urls";

export type ViewHeaderProps = {
  titleKey: string;
  className?: string;
  badges?: ViewHeaderBadge[];
  isDropdownDisabled?: boolean;
  subKey?: string | ReactNode;
  actionsDropdownId?: string;
  helpUrl?: string | undefined;
  dropdownItems?: ReactElement[];
  lowerDropdownItems?: any;
  lowerDropdownMenuTitle?: any;
  isEnabled?: boolean;
  onToggle?: (value: boolean) => void;
  divider?: boolean;
  helpTextKey?: string;
};

export type ViewHeaderBadge = {
  id?: string;
  text?: string | ReactNode;
  readonly?: boolean;
};

export const ViewHeader = ({
  actionsDropdownId,
  className,
  titleKey,
  badges,
  isDropdownDisabled,
  subKey,
  helpUrl,
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
                  <Text className={className} component="h1">
                    {t(titleKey)}
                  </Text>
                </TextContent>
              </LevelItem>
              {badges && (
                <LevelItem>
                  {badges.map((badge, index) => (
                    <Fragment key={index}>
                      {!isValidElement(badge.text) && (
                        <Fragment key={badge.text as string}>
                          <Badge data-testid={badge.id} isRead={badge.readonly}>
                            {badge.text}
                          </Badge>{" "}
                        </Fragment>
                      )}
                      {isValidElement(badge.text) && badge.text}{" "}
                    </Fragment>
                  ))}
                </LevelItem>
              )}
            </Level>
          </LevelItem>
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
                        onToggle(value);
                      }}
                    />
                    {helpTextKey && (
                      <HelpItem
                        helpText={t(helpTextKey)}
                        fieldLabelId={`${titleKey}-switch`}
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
                          isDisabled={isDropdownDisabled}
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
              {helpUrl && (
                <FormattedLink
                  title={t("common:learnMore")}
                  href={helpUrl}
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
