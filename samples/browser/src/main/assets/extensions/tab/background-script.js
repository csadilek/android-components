browser.runtime.onMessage.addListener(notify);

function notify(message) {
  if (message.action == "showTab") {
    // This works:
    //browser.tabs.update({url: "http://example.com"});

    // This fails:
    let newUrl = browser.extension.getURL("/tab.html");
    console.log(`Updating tab to ${newUrl}`);
    browser.tabs.update({url: newUrl});
  }
}