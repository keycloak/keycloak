type Edge = { from: string; to: string };

export default class FlowDiagram {
  exists() {
    cy.get(".react-flow").should("exist");
  }

  edgesExist(edges: Edge[]) {
    edges.forEach((edge) => {
      this.#labelToId(edge.from).then((fromId) => {
        this.#labelToId(edge.to).then((toId) => {
          const label = `Edge from ${fromId} to ${toId}`;
          cy.get(`[aria-label="${label}"]`);
        });
      });
    });
    return this;
  }

  #labelToId(label: string) {
    return cy.findByText(label).then((node) => {
      const id = node.attr("data-id");
      if (id) {
        return cy.wrap(id);
      }
      // if data-id does not exist, we're looking at a subflow, which has the data-id
      // on the grandparent
      return cy
        .wrap(node)
        .parent()
        .parent()
        .then((n) => cy.wrap(n.attr("data-id")));
    });
  }
}
