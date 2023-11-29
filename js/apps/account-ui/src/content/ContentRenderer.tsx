import { NavExpandable, Spinner } from "@patternfly/react-core";
import { TFunction } from "i18next";
import { Suspense, useState } from "react";
import { useTranslation } from "react-i18next";
import { useParams } from "react-router-dom";
import { CallOptions } from "../api/methods";
import { environment } from "../environment";
import { TFuncKey } from "../i18n";
import { NavLink } from "../root/PageNav";
import { ContentComponentParams } from "../routes";
import { joinPath } from "../utils/joinPath";
import { usePromise } from "../utils/usePromise";
import {
  ContentItem,
  Expansion,
  PageDef,
  isChildOf,
  isExpansion,
} from "./content";

export async function fetchContentJson(
  opts: CallOptions = {},
): Promise<ContentItem[]> {
  const response = await fetch(
    joinPath(environment.resourceUrl, "/content.json"),
    opts,
  );
  return await response.json();
}

function createNavItem(page: PageDef, activePage: string, t: TFunction) {
  return (
    <NavLink to={"content/" + page.path} isActive={activePage === page.path}>
      {t(page.label as TFuncKey)}
    </NavLink>
  );
}

function createExpandableNav(
  item: Expansion,
  t: TFunction,
  activePage: string,
  groupNum: number,
) {
  return (
    <NavExpandable
      key={item.id}
      title={t(item.label as TFuncKey)}
      isExpanded={isChildOf(item, activePage)}
    >
      {createNavItems(t, activePage, item.content, groupNum + 1)}
    </NavExpandable>
  );
}

function createNavItems(
  t: TFunction,
  activePage: string,
  contentParam: ContentItem[],
  groupNum: number,
) {
  return contentParam.map((item: ContentItem) => {
    if (isExpansion(item)) {
      return createExpandableNav(item, t, activePage!, groupNum);
    } else {
      const page: PageDef = item as PageDef;
      return createNavItem(page, activePage, t);
    }
  });
}

type MenuProps = {
  content: ContentItem[];
};

const Menu = ({ content }: MenuProps) => {
  const { t } = useTranslation();
  const { componentId: activePage } = useParams<ContentComponentParams>();

  const groupNum = 0;
  return content.map((item) => {
    if (isExpansion(item)) {
      return createExpandableNav(item, t, activePage!, groupNum);
    } else {
      const page: PageDef = item as PageDef;
      return createNavItem(page, activePage!, t);
    }
  });
};

export const ContentMenu = () => {
  const [content, setContent] = useState<ContentItem[]>();

  usePromise((signal) => fetchContentJson({ signal }), setContent);

  return (
    <Suspense fallback={<Spinner />}>
      {content && <Menu content={content} />}
    </Suspense>
  );
};
