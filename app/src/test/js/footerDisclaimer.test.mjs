import test from "node:test";
import assert from "node:assert/strict";

test("renders the portfolio-only disclaimer in the shared footer", async () => {
  const previousDocument = globalThis.document;
  const target = {
    replaceChildren(...children) {
      this.children = children;
    }
  };

  globalThis.document = {
    readyState: "complete",
    getElementById(id) {
      return id === "footer" ? target : null;
    },
    createElement(tagName) {
      return { tagName, className: "", textContent: "" };
    },
    addEventListener() {
      throw new Error("DOMContentLoaded listener should not be required for a ready document");
    }
  };

  try {
    const moduleUrl = new URL(
      `../../main/resources/static/js/components/footer.js?qa=${Date.now()}`,
      import.meta.url
    );
    await import(moduleUrl);

    assert.equal(target.children.length, 1);
    assert.equal(target.children[0].tagName, "footer");
    assert.equal(
      target.children[0].textContent,
      "Portfolio demo — synthetic data only; not for clinical use; no SLA."
    );
  } finally {
    if (previousDocument === undefined) delete globalThis.document;
    else globalThis.document = previousDocument;
  }
});
