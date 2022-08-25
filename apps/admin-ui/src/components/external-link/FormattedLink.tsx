import { AnchorHTMLAttributes } from "react";
import { ExternalLinkAltIcon } from "@patternfly/react-icons";
import type { IFormatter, IFormatterValueType } from "@patternfly/react-table";

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

export const formattedLinkTableCell =
  (): IFormatter => (data?: IFormatterValueType) => {
    return (
      data ? <FormattedLink href={data.toString()} /> : undefined
    ) as object;
  };
