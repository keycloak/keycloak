import {
  Grid,
  GridItem,
  GridProps,
  JumpLinks,
  JumpLinksItem,
  PageSection,
} from "@patternfly/react-core";
import {
  Fragment,
  ReactNode,
  useEffect,
  useMemo,
  useRef,
  useState,
} from "react";
import { FormPanel } from "./FormPanel";
import { ScrollPanel } from "./ScrollPanel";

import style from "./scroll-form.module.css";

export const mainPageContentId = "kc-main-content-page-container";
/** Distance in pixels from top of container at which a section becomes active */
const SECTION_ACTIVATION_OFFSET = 80;
/** Tolerance in pixels for detecting when scrolled to bottom */
const BOTTOM_SCROLL_TOLERANCE = 10;

type ScrollSection = {
  title: string;
  panel: ReactNode;
  isHidden?: boolean;
};

type ScrollFormProps = GridProps & {
  label: string;
  sections: ScrollSection[];
  borders?: boolean;
};

const spacesToHyphens = (string: string): string => {
  return string.replace(/\s+/g, "-");
};

export const ScrollForm = ({
  label,
  sections,
  borders = false,
  ...rest
}: ScrollFormProps) => {
  const [activeIndex, setActiveIndex] = useState(0);
  const isClickScrolling = useRef(false);

  const shownSections = useMemo(
    () => sections.filter(({ isHidden }) => !isHidden),
    [sections],
  );

  const sectionIds = useMemo(
    () =>
      shownSections.map(({ title }) => spacesToHyphens(title.toLowerCase())),
    [shownSections],
  );

  const handleClick = (scrollId: string, index: number) => {
    isClickScrolling.current = true;
    setActiveIndex(index);
    document.getElementById(scrollId)?.scrollIntoView({ behavior: "smooth" });

    const scrollContainer = document.getElementById(mainPageContentId);
    const onScrollEnd = () => {
      isClickScrolling.current = false;
      scrollContainer?.removeEventListener("scrollend", onScrollEnd);
    };
    scrollContainer?.addEventListener("scrollend", onScrollEnd);
  };

  // Sync activeIndex on scroll
  useEffect(() => {
    const scrollContainer = document.getElementById(mainPageContentId);
    if (!scrollContainer) return;

    const handleScroll = () => {
      if (isClickScrolling.current) return;

      const isAtBottom =
        scrollContainer.scrollHeight - scrollContainer.scrollTop <=
        scrollContainer.clientHeight + BOTTOM_SCROLL_TOLERANCE;
      if (isAtBottom) {
        setActiveIndex(sectionIds.length - 1);
        return;
      }

      const containerRect = scrollContainer.getBoundingClientRect();

      for (let i = sectionIds.length - 1; i >= 0; i--) {
        const element = document.getElementById(sectionIds[i]);
        if (element) {
          const elementRect = element.getBoundingClientRect();
          const relativeTop = elementRect.top - containerRect.top;

          if (relativeTop <= SECTION_ACTIVATION_OFFSET) {
            setActiveIndex(i);
            return;
          }
        }
      }
      setActiveIndex(0);
    };

    scrollContainer.addEventListener("scroll", handleScroll);
    handleScroll();
    return () => scrollContainer.removeEventListener("scroll", handleScroll);
  }, [sectionIds]);

  return (
    <Grid hasGutter {...rest}>
      <GridItem md={8} sm={12}>
        {shownSections.map(({ title, panel }) => {
          const scrollId = spacesToHyphens(title.toLowerCase());

          return (
            <Fragment key={title}>
              {borders ? (
                <FormPanel
                  scrollId={scrollId}
                  title={title}
                  className={style.panel}
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
      <GridItem md={4} sm={12} order={{ default: "-1", md: "1" }}>
        <PageSection className={style.sticky}>
          <JumpLinks isVertical label={label}>
            {sectionIds.map((scrollId, index) => (
              <JumpLinksItem
                key={scrollId}
                href={`#${scrollId}`}
                isActive={activeIndex === index}
                data-testid={`jump-link-${scrollId}`}
                onClick={(e) => {
                  e.stopPropagation();
                  e.preventDefault();
                  handleClick(scrollId, index);
                }}
              >
                {shownSections[index].title}
              </JumpLinksItem>
            ))}
          </JumpLinks>
        </PageSection>
      </GridItem>
    </Grid>
  );
};
