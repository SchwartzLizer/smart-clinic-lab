import test from "node:test";
import assert from "node:assert/strict";

import { bangkokDate } from "../../main/resources/static/js/clinicDate.js";

test("formats clinic dates in Asia/Bangkok across UTC calendar rollover", () => {
  assert.equal(bangkokDate(new Date("2029-12-31T18:00:00Z")), "2030-01-01");
  assert.equal(bangkokDate(new Date("2030-01-10T16:59:59Z")), "2030-01-10");
  assert.equal(bangkokDate(new Date("2030-01-10T17:00:00Z")), "2030-01-11");
});
