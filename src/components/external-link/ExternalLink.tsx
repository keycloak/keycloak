import React from "react";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { Button, ButtonProps } from "@patternfly/react-core";

export const ExternalLink = ({ title, href, ...rest }: ButtonProps) => {
  return (
    <Button
      variant="link"
      icon={href?.startsWith("http") && <ExternalLinkAltIcon />}
      iconPosition="right"
      component="a"
      href={href}
      target="_blank"
      {...rest}
    >
      {title ? title : href}
    </Button>
  );
};
