export type Links = {
  prev?: Record<string, string>;
  next?: Record<string, string>;
};

export function parseLinks(response: Response): Links {
  const linkHeader = response.headers.get("link");

  if (!linkHeader) {
    return {};
  }

  const links = linkHeader.split(/,\s*</);
  return links.reduce<Links>((acc: Links, link: string) => {
    const matcher = /<?([^>]*)>(.*)/.exec(link);
    if (!matcher) return {};
    const linkUrl = matcher[1];
    const rel = /\s*(.+)\s*=\s*"?([^"]+)"?/.exec(matcher[2]);
    if (rel) {
      const link: Record<string, string> = {};
      for (const [key, value] of new URL(linkUrl).searchParams.entries()) {
        link[key] = value;
      }
      acc[rel[2] as keyof Links] = link;
    }
    return acc;
  }, {});
}
