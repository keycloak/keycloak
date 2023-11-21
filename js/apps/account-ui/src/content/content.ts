export interface ContentItem {
  id?: string;
  label: string;
  labelParams?: string[];
  hidden?: string;
}

export interface Expansion extends ContentItem {
  content: ContentItem[];
}

export interface PageDef extends ContentItem {
  path: string;
}

export interface ComponentPageDef extends PageDef {
  component: React.ComponentType;
}

export interface ModulePageDef extends PageDef {
  modulePath: string;
  componentName: string;
}

export function isExpansion(
  contentItem: ContentItem,
): contentItem is Expansion {
  return "content" in contentItem;
}

export function isChildOf(parent: Expansion, child: string): boolean {
  for (const item of parent.content) {
    if (isExpansion(item) && isChildOf(item, child)) return true;
    if ("path" in item && item.path === child) return true;
  }

  return false;
}
