import assert from "node:assert/strict";
import test from "node:test";
import { crc32, storeZip } from "../shared/zip-store.ts";

test("CRC-32 matches the standard check vector", () => {
  assert.equal(crc32(new TextEncoder().encode("123456789")), 0xcbf43926);
});

test("stored ZIP contains local, central and end records with original bytes", () => {
  const text = new TextEncoder().encode("hello optical world");
  const zip = storeZip([
    { name: "notes.txt", bytes: text },
    { name: "pixel.bin", bytes: new Uint8Array([0, 1, 254, 255]) },
  ]);
  const view = new DataView(zip.buffer);
  assert.equal(view.getUint32(0, true), 0x04034b50);
  assert.ok(zip.includes(0x50));
  assert.equal(view.getUint32(zip.length - 22, true), 0x06054b50);
  assert.equal(view.getUint16(zip.length - 12, true), 2);
  assert.equal(view.getUint16(26, true), "notes.txt".length);
  const firstData = 30 + "notes.txt".length;
  assert.deepEqual(zip.subarray(firstData, firstData + text.length), text);
});

test("duplicate and path-like names are made safe and unique", () => {
  const zip = storeZip([
    { name: "../same.txt", bytes: new Uint8Array([1]) },
    { name: "same.txt", bytes: new Uint8Array([2]) },
  ]);
  const rendered = new TextDecoder().decode(zip);
  assert.match(rendered, /same\.txt/);
  assert.match(rendered, /same \(2\)\.txt/);
  assert.doesNotMatch(rendered, /\.\.\//);
});
