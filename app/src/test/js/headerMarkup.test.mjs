import test from "node:test";
import assert from "node:assert/strict";
import { readFile } from "node:fs/promises";

const headerSource = await readFile(
  new URL("../../main/resources/static/js/components/header.js", import.meta.url),
  "utf8",
);

const patientDashboardSource = await readFile(
  new URL("../../main/resources/static/pages/patientDashboard.html", import.meta.url),
  "utf8",
);
const loggedPatientDashboardSource = await readFile(
  new URL("../../main/resources/static/pages/loggedPatientDashboard.html", import.meta.url),
  "utf8",
);

test("header markup applies the bounded logo image and title styles", () => {
  assert.match(headerSource, /class="logo-img"/);
  assert.match(headerSource, /class="logo-title"/);
});

test("patient doctor search and filters share one responsive toolbar", () => {
  for (const source of [patientDashboardSource, loggedPatientDashboardSource]) {
    assert.match(source, /class="search-filter-toolbar"/);
    assert.match(source, /class="searchBar"[\s\S]*class="filter-wrapper"/);
  }
});
