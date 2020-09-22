import React from "react";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { Button } from "@patternfly/react-core";

export const ExternalLink = ({ title, href, ...rest }: ButtonProps) => {
  return (
    <Button
      variant="link"
      icon={href?.startsWith("http") && <ExternalLinkAltIcon />}
      iconPosition="right"
      component="a"
      href={href}
      {...rest}
    >
      {title ? title : href}
    </Button>
  );
};
