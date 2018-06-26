# Generates a copy of site.pages as site.weighted_pages
# with pages sorted by weight attribute. Pages with no
# weight specified are placed after the pages with specified weight.

module Jekyll
  
  class WeightedPagesGenerator < Generator
    safe true

    def generate(site)
      site.config['weighted_pages'] = site.pages.sort_by { |a| 
        a.data['weight'] ? a.data['weight'] : site.pages.length }
    end

  end

end