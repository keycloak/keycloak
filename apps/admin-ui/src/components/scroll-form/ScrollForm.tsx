import { Fragment, FunctionComponent, ReactNode, useMemo } from "react";
import { useTranslation } from "react-i18next";
import {
  Grid,
  GridItem,
  GridProps,
  JumpLinks,
  JumpLinksItem,
  PageSection,
} from "@patternfly/react-core";

import { mainPageContentId } from "../../App";
import { ScrollPanel } from "./ScrollPanel";
import { FormPanel } from "./FormPanel";

import "./scroll-form.css";

type ScrollSection = {
  title: string;
  panel: ReactNode;
  isHidden?: boolean;
};

type ScrollFormProps = GridProps & {
  sections: ScrollSection[];
  borders?: boolean;
};

const spacesToHyphens = (string: string): string => {
  return string.replace(/\s+/g, "-");
};

export const ScrollForm: FunctionComponent<ScrollFormProps> = ({
  sections,
  borders = false,
  ...rest
}) => {
  const { t } = useTranslation("common");
  const shownSections = useMemo(
    () => sections.filter(({ isHidden }) => !isHidden),
    [sections]
  );

  return (
    <Grid hasGutter {...rest}>
      <GridItem span={8}>
        {shownSections.map(({ title, panel }) => {
          const scrollId = spacesToHyphens(title.toLowerCase());

          return (
            <Fragment key={title}>
              {borders ? (
                <FormPanel
                  scrollId={scrollId}
                  title={title}
                  className="kc-form-panel__panel"
                >
                  {panel}
                </FormPanel>
              ) : (
                <ScrollPanel scrollId={scrollId} title={title}>
                  {panel}
                </ScrollPanel>
              )}
            </Fragment>
          );
        })}
      </GridItem>
      <GridItem span={4}>
        <PageSection className="kc-scroll-form--sticky">
          <JumpLinks
            isVertical
            // scrollableSelector has to point to the id of the element whose scrollTop changes
            // to scroll the entire main section, it has to be the pf-c-page__main
            scrollableSelector={`#${mainPageContentId}`}
            label={t("jumpToSection")}
            offset={100}
          >
            {shownSections.map(({ title }) => {
              const scrollId = spacesToHyphens(title.toLowerCase());

              return (
                // note that JumpLinks currently does not work with spaces in the href
                <JumpLinksItem
                  key={title}
                  href={`#${scrollId}`}
                  data-testid={`jump-link-${scrollId}`}
                >
                  {title}
                </JumpLinksItem>
              );
            })}
          </JumpLinks>
        </PageSection>
      </GridItem>
    </Grid>
  );
};
