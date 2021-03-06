/**
 * This is a slightly modified version of https://github.com/dharmafly/connect-ratelimit
 */

module.exports = function (options) {
  var clients   = {},
      whitelist,
      blacklist,
      end       = false,
      fail      = {
        message: 'Rate limit exceded.',
        type: ''
      },
      config    = {
        whitelist: {
          totalRequests: 5000,
          every:         60 * 60 * 1000
        },
        blacklist: {
          totalRequests: 0,
          every:         0 
        },
        normal: {
          totalRequests: 500,
          every:         60 * 60 * 1000
        },
      };

  var categories;

  if (!options){
    options = {};
  }

  whitelist   = options.whitelist || [];
  blacklist   = options.blacklist || [];
  end         = options.end       || end;

  deepExtend(fail, options.fail);

  categories = options.categories || options.catagories; 
  if (categories){
    deepExtend(config, categories);
  }

  function middleware (req, res, next) {
    var name   = req.headers['x-forwarded-for'] || req.connection.remoteAddress,
        type   = getClientType(name),
        client = clients[name];

    res.ratelimit = {
      clients: clients,
      exceeded: false
    };

    if (req.url === '/favicon.ico') {
      next();
      return;
    };

    if (!client) {
      clients[name] = client = new Client(name, type, config[type].every);
    }

    res.setHeader('X-RateLimit-Limit', config[type].totalRequests);
    res.setHeader('X-RateLimit-Remaining', config[type].totalRequests - client.ticks);

    res.ratelimit.exceeded = !ok(client);
    res.ratelimit.client   = client;

    if (ok(client)) {
      client.ticks++;
      next();
    } 
    else if (end === false) {
      next();
    }
    else {
      res.statusCode = 429;
      if (fail.type !== '') {
        res.setHeader('content-type', fail.type);
      }
      res.end(fail.message);
    }
  }

  function Client (name, type, resetIn) {
    var name   = name;

    this.name  = name;
    this.type  = type;
    this.ticks = 1;

    setTimeout(function () {
      delete clients[name];
    }, resetIn);
  }

  function ok (client) {
    if (client.type === 'whitelist') {
      return client.ticks <= config.whitelist.totalRequests;
    }
    if (client.type === 'blacklist') {
      return client.ticks <= config.blacklist.totalRequests;
    }
    if (client.type === 'normal') {
      return client.ticks <= config.normal.totalRequests;
    }
  }

  function getClientType (name) {
    if (whitelist.indexOf(name) > -1) {
      return 'whitelist';
    }
    if (blacklist.indexOf(name) > -1) {
      return 'blacklist';
    }
    return 'normal';
  }

  return middleware;
};

function deepExtend (destination, source) {
  var property;
  
  for (property in source) {
    if (source[property] && source[property].constructor &&
    source[property].constructor === Object) {
      destination[property] = destination[property] || {};
      deepExtend(destination[property], source[property]);
    } else {
      destination[property] = source[property];
    }
  }
  return destination;
}