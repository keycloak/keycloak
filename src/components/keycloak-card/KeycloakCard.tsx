import React, { ReactElement, useState } from "react";
import {
  Card,
  CardHeader,
  CardActions,
  CardTitle,
  CardBody,
  CardFooter,
  Dropdown,
  KebabToggle,
  Label,
  Flex,
  FlexItem,
} from "@patternfly/react-core";
import "./keycloak-card.css";
import { useHistory, useRouteMatch } from "react-router-dom";

export type KeycloakCardProps = {
  id: string;
  title: string;
  dropdownItems?: ReactElement[];
  labelText?: string;
  labelColor?: any;
  footerText?: string;
  configEnabled?: boolean;
  providerId?: string;
};

export const KeycloakCard = ({
  id,
  title,
  dropdownItems,
  labelText,
  labelColor,
  footerText,
  providerId,
}: KeycloakCardProps) => {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);

  const history = useHistory();
  const { url } = useRouteMatch();

  const onDropdownToggle = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  const handleCardMenuClick = (e: any) => {
    e.stopPropagation();
  };

  const openSettings = () => {
    history.push(`${url}/${providerId}/${id}`);
  };

  return (
    <Card isSelectable onClick={openSettings}>
      <CardHeader>
        <CardActions>
          {dropdownItems && (
            <Dropdown
              isPlain
              position={"right"}
              toggle={<KebabToggle onToggle={onDropdownToggle} />}
              onClick={(e) => handleCardMenuClick(e)}
              isOpen={isDropdownOpen}
              dropdownItems={dropdownItems}
            />
          )}
        </CardActions>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardBody />
      <CardFooter>
        <Flex>
          <FlexItem className="keycloak--keycloak-card__footer">
            {footerText && footerText}
          </FlexItem>
          <FlexItem>
            {labelText && (
              <Label color={labelColor || "gray"}>{labelText}</Label>
            )}
          </FlexItem>
        </Flex>
      </CardFooter>
    </Card>
  );
};
