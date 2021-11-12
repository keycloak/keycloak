describe("bootstrap-treeview test suite", function () {

  beforeEach(function () {
    globals.readFixture(globals.fixturePath + 'bootstrap-treeview.html');
  });

  it('should render a tree view with three parents and one child node', function (done) {

    var defaultData = [
      {
        text: 'Parent 1',
        href: '#parent1',
        tags: ['1'],
        nodes: [
          {
            text: 'Child 1',
            href: '#child1',
            icon: 'fa fa-file-o',
            tags: ['0']
          }
        ]
      },
      {
        text: 'Parent 2',
        href: '#parent2',
        tags: ['0']
      },
      {
        text: 'Parent 3',
        href: '#parent3',
        tags: ['0']
      }
    ];

    $('#treeview1').treeview({
      collapseIcon: "fa fa-angle-down",
      data: defaultData,
      expandIcon: "fa fa-angle-right",
      nodeIcon: "fa fa-folder",
      showBorder: false
    });

    setTimeout(function () {
      var nodes = $('#treeview1 ul li');
      var indent = nodes.find('.indent');

      expect(nodes).toHaveLength(4);
      expect(indent).toHaveLength(1);
      done();
    }, globals.wait);
  });

});
