module.exports = [
    {
        active:  false,
        title:   "DSL (2Mbs, 5ms RTT)",
        id:      "dsl",
        speed:   200,
        latency: 5,
        urls:    [],
        order:   1
    },
    {
        active:  false,
        title:   "4G (4Mbs, 20ms RTT)",
        id:     "4g",
        speed:   400,
        latency: 10,
        urls:    [],
        order:   2

    },
    {
        active:  false,
        title:   "3G (750kbs, 100ms RTT)",
        id:     "3g",
        speed:   75,
        latency: 50,
        urls:    [],
        order:   2

    },
    {
        active:  false,
        id:      "edge",
        title:   "Regular 2G (250kbs, 300ms RTT)",
        speed:   25,
        latency: 150,
        urls:    [],
        order:   3
    },
    {
        active:  false,
        id:      "edge+",
        title:   "Good 2G (450kbs, 150ms RTT)",
        speed:   45,
        latency: 75,
        urls:    [],
        order:   3
    },
    {
        active:  false,
        id:      "gprs",
        title:   "GPRS (50kbs, 500ms RTT)",
        speed:   5,
        latency: 250,
        urls:    [],
        order:   4
    },
    //{
    //    active:  false,
    //    id:    "none",
    //    title:   "None",
    //    speed:   0,
    //    latency: 0,
    //    urls:    [],
    //    order:   5
    //}
];