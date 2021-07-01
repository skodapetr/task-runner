const path = require("path");
const express = require("express");
const logger = require("./logging");
const request = require("request");

(function initialize() {
  const app = express();
  initializeProxy(app);
  initializeStatic(app);
  start(app);
})();

function initializeProxy(app) {
  app.use('/api', function(req, res) {
    const url = "http://localhost:8020/api" + req.url;
    console.log("proxy", url);
    req.pipe(request({ "qs":req.query, "uri": url })).pipe(res);
  });
}

function initializeStatic(app) {
  const assetsPath = path.join(__dirname, "../client/");
  app.use("/", express.static(assetsPath));
}

function start(app) {
  const port = 8021;
  app.listen(port, function onStart(error) {
    if (error) {
      logger.error("Can't start server.", {"error": error});
    }
    logger.info("Server has been started.", {"port": port});
  });
}