import {
  Brand,
  Masthead,
  MastheadBrand,
  MastheadMain,
  MastheadToggle,
  PageToggleButton,
} from "@patternfly/react-core";
import { BarsIcon } from "@patternfly/react-icons";
import { useTranslation } from "react-i18next";
import { useHref, useLinkClickHandler } from "react-router-dom";

import { environment } from "../environment";
import classes from "./PageHeader.module.css";

export const PageHeader = () => {
  const { t } = useTranslation();
  const href = useHref("/");
  const handleClick = useLinkClickHandler("/");

  return (
    <Masthead>
      <MastheadToggle>
        <PageToggleButton variant="plain" aria-label={t("globalNavigation")}>
          <BarsIcon />
        </PageToggleButton>
      </MastheadToggle>
      <MastheadMain>
        <MastheadBrand href={href} onClick={handleClick}>
          <Brand
            className={classes.brand}
            src={environment.resourceUrl + "/logo.svg"}
            alt={t("logo")}
          />
        </MastheadBrand>
      </MastheadMain>
    </Masthead>
  );
};
