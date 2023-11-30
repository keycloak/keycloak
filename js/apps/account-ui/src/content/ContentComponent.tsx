import { Spinner } from "@patternfly/react-core";
import { Suspense, lazy, useMemo, useState } from "react";
import { useParams } from "react-router-dom";
import { environment } from "../environment";
import { joinPath } from "../utils/joinPath";
import { usePromise } from "../utils/usePromise";
import { fetchContentJson } from "./ContentRenderer";
import { ContentItem, ModulePageDef, isExpansion } from "./content";
import { ContentComponentParams } from "../routes";

function findComponent(
  content: ContentItem[],
  componentId: string,
): string | undefined {
  for (const item of content) {
    if ("path" in item && item.path === componentId) {
      return (item as ModulePageDef).modulePath;
    }
    if (isExpansion(item)) {
      return findComponent(item.content, componentId);
    }
  }
  return undefined;
}

const ContentComponent = () => {
  const [content, setContent] = useState<ContentItem[]>();
  const { componentId } = useParams<ContentComponentParams>();

  usePromise((signal) => fetchContentJson({ signal }), setContent);
  const modulePath = useMemo(
    () => findComponent(content || [], componentId!),
    [content, componentId],
  );

  return (
    <Suspense fallback={<Spinner />}>
      {modulePath && <Component modulePath={modulePath} />}
    </Suspense>
  );
};

type ComponentProps = {
  modulePath: string;
};

const Component = ({ modulePath }: ComponentProps) => {
  const Element = lazy(
    () => import(joinPath(environment.resourceUrl, modulePath)),
  );
  return <Element />;
};

export default ContentComponent;
