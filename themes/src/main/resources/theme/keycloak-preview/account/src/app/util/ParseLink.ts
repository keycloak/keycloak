
export interface Links {
  prev?: string;
  next?: string;
}

function parse(linkHeader: string | undefined): Links {
  if (!linkHeader) return {};
  const links = linkHeader.split(/,\s*</);
  return links.reduce<Links>((acc: Links, link: string): Links => {
    const matcher = link.match(/<?([^>]*)>(.*)/);
    if (!matcher) return {};
    const linkUrl = matcher[1];
    const rel = matcher[2].match(/\s*(.+)\s*=\s*"?([^"]+)"?/);
    if (rel) {
        acc[rel[2]] = linkUrl;
    }
    return acc;
  }, {});
}

export default parse;