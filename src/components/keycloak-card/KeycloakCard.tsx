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
  dropdownItems,
  title,
  labelText,
  labelColor,
  footerText,
}: KeycloakCardProps) => {
  const [isDropdownOpen, setIsDropdownOpen] = useState(false);
  const onDropdownToggle = () => {
    setIsDropdownOpen(!isDropdownOpen);
  };

  return (
    <Card>
      <CardHeader>
        <CardActions>
          {dropdownItems && (
            <Dropdown
              isPlain
              position={"right"}
              toggle={<KebabToggle onToggle={onDropdownToggle} />}
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
