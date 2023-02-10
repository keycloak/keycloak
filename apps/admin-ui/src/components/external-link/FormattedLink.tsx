import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import { AnchorHTMLAttributes } from "react";

export type FormattedLinkProps = AnchorHTMLAttributes<HTMLAnchorElement> & {
  isInline?: boolean;
};

export const FormattedLink = ({
  title,
  href,
  isInline,
  ...rest
}: FormattedLinkProps) => {
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer noopener"
      className={isInline ? "pf-m-link pf-m-inline" : ""}
      {...rest}
    >
      {title ? title : href}{" "}
      {href?.startsWith("http") && <ExternalLinkAltIcon />}
    </a>
  );
};
