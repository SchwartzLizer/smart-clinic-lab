import test from "node:test";
import assert from "node:assert/strict";
import { readdir, readFile } from "node:fs/promises";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const staticRoot = new URL("../../main/resources/static/", import.meta.url);

async function sources(directory) {
  const entries = await readdir(directory, { withFileTypes: true });
  const nested = await Promise.all(entries.map(async (entry) => {
    const path = join(directory, entry.name);
    return entry.isDirectory() ? sources(path) : [path];
  }));
  return nested.flat();
}

test("served frontend does not persist auth in localStorage or use inline executable handlers", async () => {
  const paths = await sources(fileURLToPath(staticRoot));
  const relevant = paths.filter((path) => path.endsWith(".js") || path.endsWith(".html"));
  const text = (await Promise.all(relevant.map((path) => readFile(path, "utf8")))).join("\n");

  assert.doesNotMatch(text, /localStorage/);
  assert.doesNotMatch(text, /<[^>]+\s+on[a-z][\w:-]*\s*=/i);
  assert.doesNotMatch(text, /\.innerHTML/);
  assert.match(text, /sessionStorage/);
});
