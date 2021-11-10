//test globals
window.globals = {

  //karma test timeout
  timeout: 10000,

  //fixture base path
  base: './',

  //fixture path
  fixturePath: 'base/dist/tests/',

  //visual waits in between tests
  wait: 500,

  //global helper for reading our test fixtures
  readFixture: function(path){
    var fixture = readFixtures(path);

    //NOTE: we are reusing "test" page fixtures from standalone pages,
    //so it is necessary to remove page scripts and ensure we do not redefine jquery,
    //and instead use our karma loaded jquery/scripts
    var strip = fixture.replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, " ");

    setFixtures(strip);
  }
};

//set global jasmine variables
jasmine.DEFAULT_TIMEOUT_INTERVAL = globals.timeout;
jasmine.getFixtures().fixturesPath = globals.base;

