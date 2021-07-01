const initialHtml = document.getElementById("initial");
const missingHtml = document.getElementById("missing");
const queuedHtml = document.getElementById("queued");
const runningHtml = document.getElementById("running");
const successfulHtml = document.getElementById("successful");
const failedHtml = document.getElementById("failed");
const fetchFailedHtml = document.getElementById("fetchFailed");

(async function initialize() {
  const query = parseUrlQuery();
  if (query.task === null) {
    onMissingTask();
    return
  }
  await checkStatus(query);
  const timer = setInterval(async function checkTask() {
    const shouldCheckAgain = checkStatus(query);
    if (!shouldCheckAgain) {
      clearInterval(timer);
    }
  }, 1000)
})();

function parseUrlQuery() {
  const url = new URL(window.location);
  return {
    "task": url.searchParams.get("task"),
    "redirect": url.searchParams.get("redirect"),
    "redirectFailed": url.searchParams.get("redirect-failed"),
  }
}

async function checkStatus(query) {
  let content;
  try {
    content = await fetchTask(query.task);
  } catch {
    onFetchFailed();
    return true;
  }
  switch (content.status) {
    case "queued":
      onTaskQueued(content)
      return true;
    case "running":
      onTaskRunning(content);
      return true;
    case "successful":
      onTaskSuccessful(content, query);
      return false;
    case "failed":
      onTaskFailed(content, query);
      return false;
    default:
      onUnknownState(content);
      return true;
  }
}

function onMissingTask() {
  setVisible([missingHtml]);
  setInVisible([
    initialHtml, queuedHtml,
    runningHtml, successfulHtml, failedHtml, fetchFailedHtml,
  ]);
}

function setVisible(elements) {
  elements.forEach(element => element.classList.remove("invisible"));
}

function setInVisible(elements) {
  elements.forEach(element => element.classList.add("invisible"));
}

async function fetchTask(url) {
  return new Promise((accept, reject) => {
    fetch(url)
      .then(response => response.json())
      .then(accept)
      .catch(reject);
  });
}

function onFetchFailed() {
  // Leave all as it is just show the error message.
  setVisible([fetchFailedHtml]);
}

function onTaskQueued(content) {
  setVisible([queuedHtml]);
  setInVisible([
    initialHtml, missingHtml,
    runningHtml, successfulHtml, failedHtml, fetchFailedHtml,
  ]);
}

function onTaskRunning(content) {
  setVisible([runningHtml]);
  setInVisible([
    initialHtml, missingHtml, queuedHtml,
    successfulHtml, failedHtml, fetchFailedHtml,
  ]);
}

function onTaskSuccessful(content, args) {
  if (args.redirect) {
    window.location.replace(args.redirect);
  }
  setVisible([successfulHtml]);
  setInVisible([
    initialHtml, missingHtml, queuedHtml,
    runningHtml, failedHtml, fetchFailedHtml,
  ]);
}

function onTaskFailed(content, args) {
  if (args.redirectFailed) {
    window.location.replace(args.redirectFailed);
  }
  setVisible([failedHtml]);
  setInVisible([
    initialHtml, missingHtml, queuedHtml,
    runningHtml, successfulHtml, fetchFailedHtml,
  ]);
}

function onUnknownState(content) {
  // Just save log information and do nothing.
  console.warn("Unknown state", content.status);
}
