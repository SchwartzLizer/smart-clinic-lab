import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";
import vm from "node:vm";

function node(tagName) {
  return {
    tagName,
    children: [],
    append(...children) { this.children.push(...children); },
    replaceChildren(...children) { this.children = children; },
    addEventListener() {},
  };
}

const headerSource = await readFile(
  new URL("../../main/resources/static/js/components/header.js", import.meta.url),
  "utf8",
);

function navigationButtonLabels(activeRole) {
  const headerTarget = node("div");
  const context = {
    document: {
      readyState: "loading",
      addEventListener() {},
      createElement: node,
      getElementById(id) { return id === "header" ? headerTarget : null; },
    },
    sessionStorage: {
      getItem(key) { return key === "userRole" ? activeRole : null; },
      clear() {},
    },
    location: { href: "" },
  };
  vm.runInNewContext(
    headerSource.replace("function renderHeader(){", "globalThis.renderHeader = function renderHeader(){"),
    context,
  );
  context.renderHeader();
  const header = headerTarget.children[0];
  const nav = header.children[1];
  return nav.children.slice(1).map((button) => button.textContent);
}

test("guest patient portal renders Log in without contradictory Log out", () => {
  assert.deepEqual(navigationButtonLabels("patient"), ["Log in"]);
});

test("authenticated roles render Log out without guest Log in", () => {
  for (const authenticatedRole of ["admin", "doctor", "loggedPatient"]) {
    assert.deepEqual(navigationButtonLabels(authenticatedRole), ["Log out"]);
  }
});
