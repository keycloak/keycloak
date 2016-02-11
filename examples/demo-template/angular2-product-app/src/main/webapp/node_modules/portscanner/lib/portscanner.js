var net    = require('net')
  , Socket = net.Socket
  , async  = require('async')

var portscanner = exports

/**
 * Finds the first port with a status of 'open', implying the port is in use and
 * there is likely a service listening on it.
 *
 * @param {Number} startPort  - Port to begin status check on (inclusive).
 * @param {Number} endPort    - Last port to check status on (inclusive).
 *                              Defaults to 65535.
 * @param {String} host       - Where to scan. Defaults to '127.0.0.1'.
 * @param {Function} callback - function (error, port) { ... }
 *   - {Object|null} error    - Any errors that occurred while port scanning.
 *   - {Number|Boolean} port  - The first open port found. Note, this is the
 *                              first port that returns status as 'open', not
 *                              necessarily the first open port checked. If no
 *                              open port is found, the value is false.
 */
portscanner.findAPortInUse = function(startPort, endPort, host, callback) {
  findAPortWithStatus('open', startPort, endPort, host, callback)
}

/**
 * Finds the first port with a status of 'closed', implying the port is not in
 * use.
 *
 * @param {Number} startPort  - Port to begin status check on (inclusive).
 * @param {Number} endPort    - Last port to check status on (inclusive).
 *                              Defaults to 65535.
 * @param {String} host       - Where to scan. Defaults to '127.0.0.1'.
 * @param {Function} callback - function (error, port) { ... }
 *   - {Object|null} error    - Any errors that occurred while port scanning.
 *   - {Number|Boolean} port  - The first closed port found. Note, this is the
 *                              first port that returns status as 'closed', not
 *                              necessarily the first closed port checked. If no
 *                              closed port is found, the value is false.
 */
portscanner.findAPortNotInUse = function(startPort, endPort, host, callback) {
  findAPortWithStatus('closed', startPort, endPort, host, callback)
}

/**
 * Checks the status of an individual port.
 *
 * @param {Number} port           - Port to check status on.
 * @param {String|Object} options - host or options
 *   - {String} host              - Host of where to scan. Defaults to '127.0.0.1'.
 *   - {Object} options
 *     - {String} host            - Host of where to scan. Defaults to '127.0.0.1'.
 *     - {Number} timeout         - Connection timeout. Defaults to 400ms.
 * @param {Function} callback     - function (error, port) { ... }
 *   - {Object|null} error        - Any errors that occurred while port scanning.
 *   - {String} status            - 'open' if the port is in use.
 *                                  'closed' if the port is available.
 */
portscanner.checkPortStatus = function(port, options, callback) {
  if (typeof options === 'string') {
    // Assume this param is the host option
    options = {host: options}
  }

  var host = options.host || '127.0.0.1'
  var timeout = options.timeout || 400
  var connectionRefused = false;

  var socket = new Socket()
    , status = null
    , error = null

  // Socket connection established, port is open
  socket.on('connect', function() {
    status = 'open'
    socket.destroy()
  })

  // If no response, assume port is not listening
  socket.setTimeout(timeout)
  socket.on('timeout', function() {
    status = 'closed'
    error = new Error('Timeout (' + timeout + 'ms) occurred waiting for ' + host + ':' + port + ' to be available')
    socket.destroy()
  })

  // Assuming the port is not open if an error. May need to refine based on
  // exception
  socket.on('error', function(exception) {
    if(exception.code !== "ECONNREFUSED") {
      error = exception
    }
    else
      connectionRefused = true;
    status = 'closed'
  })

  // Return after the socket has closed
  socket.on('close', function(exception) {
    if(exception && !connectionRefused) 
      error = exception;
    else
      error = null;
    callback(error, status)
  })

  socket.connect(port, host)
}

function findAPortWithStatus(status, startPort, endPort, host, callback) {
  endPort = endPort || 65535
  var foundPort = false
  var numberOfPortsChecked = 0
  var port = startPort

  // Returns true if a port with matching status has been found or if checked
  // the entire range of ports
  var hasFoundPort = function() {
    return foundPort || numberOfPortsChecked === (endPort - startPort + 1)
  }

  // Checks the status of the port
  var checkNextPort = function(callback) {
    portscanner.checkPortStatus(port, host, function(error, statusOfPort) {
      numberOfPortsChecked++
      if (statusOfPort === status) {
        foundPort = true
        callback(error)
      }
      else {
        port++
        callback(null)
      }
    })
  }

  // Check the status of each port until one with a matching status has been
  // found or the range of ports has been exhausted
  async.until(hasFoundPort, checkNextPort, function(error) {
    if (error) {
      callback(error, port)
    }
    else if (foundPort) {
      callback(null, port)
    }
    else {
      callback(null, false)
    }
  })
}

