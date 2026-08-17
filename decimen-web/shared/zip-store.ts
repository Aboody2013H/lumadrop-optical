const encoder = new TextEncoder();

export interface ZipEntry {
  name: string;
  bytes: Uint8Array;
}

const crcTable = (() => {
  const table = new Uint32Array(256);
  for (let n = 0; n < table.length; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c >>> 0;
  }
  return table;
})();

export function crc32(bytes: Uint8Array): number {
  let crc = 0xffffffff;
  for (const byte of bytes) crc = crcTable[(crc ^ byte) & 0xff]! ^ (crc >>> 8);
  return (crc ^ 0xffffffff) >>> 0;
}

function basename(name: string): string {
  const clean = name.split(/[\\/]/).pop()?.replace(/[\u0000-\u001f\u007f]/g, "").trim();
  return clean && clean !== "." && clean !== ".." ? clean : "file.bin";
}

function uniqueNames(entries: readonly ZipEntry[]): Array<{ name: Uint8Array; bytes: Uint8Array }> {
  const used = new Set<string>();
  return entries.map((entry) => {
    const original = basename(entry.name);
    let candidate = original;
    let suffix = 2;
    while (used.has(candidate.toLocaleLowerCase())) {
      const dot = original.lastIndexOf(".");
      const stem = dot > 0 ? original.slice(0, dot) : original;
      const extension = dot > 0 ? original.slice(dot) : "";
      candidate = `${stem} (${suffix++})${extension}`;
    }
    used.add(candidate.toLocaleLowerCase());
    return { name: encoder.encode(candidate), bytes: entry.bytes };
  });
}

function writeU16(view: DataView, offset: number, value: number): number {
  view.setUint16(offset, value, true);
  return offset + 2;
}

function writeU32(view: DataView, offset: number, value: number): number {
  view.setUint32(offset, value >>> 0, true);
  return offset + 4;
}

/** Build a standards-compliant ZIP using the store method (no recompression). */
export function storeZip(entries: readonly ZipEntry[]): Uint8Array {
  if (entries.length === 0) throw new Error("Choose at least one file.");
  if (entries.length > 0xffff) throw new Error("Too many files for one ZIP archive.");
  const files = uniqueNames(entries);
  for (const file of files) {
    if (file.name.length > 0xffff) throw new Error("A filename is too long for ZIP.");
  }

  const localBytes = files.reduce((sum, file) => sum + 30 + file.name.length + file.bytes.length, 0);
  const centralBytes = files.reduce((sum, file) => sum + 46 + file.name.length, 0);
  const out = new Uint8Array(localBytes + centralBytes + 22);
  const view = new DataView(out.buffer);
  const records: Array<{ name: Uint8Array; bytes: Uint8Array; crc: number; offset: number }> = [];
  let offset = 0;

  for (const file of files) {
    const record = { ...file, crc: crc32(file.bytes), offset };
    records.push(record);
    offset = writeU32(view, offset, 0x04034b50);
    offset = writeU16(view, offset, 20);
    offset = writeU16(view, offset, 0x0800); // UTF-8 names
    offset = writeU16(view, offset, 0); // store
    offset = writeU16(view, offset, 0); // time
    offset = writeU16(view, offset, 0); // date
    offset = writeU32(view, offset, record.crc);
    offset = writeU32(view, offset, file.bytes.length);
    offset = writeU32(view, offset, file.bytes.length);
    offset = writeU16(view, offset, file.name.length);
    offset = writeU16(view, offset, 0);
    out.set(file.name, offset);
    offset += file.name.length;
    out.set(file.bytes, offset);
    offset += file.bytes.length;
  }

  const centralOffset = offset;
  for (const record of records) {
    offset = writeU32(view, offset, 0x02014b50);
    offset = writeU16(view, offset, 20);
    offset = writeU16(view, offset, 20);
    offset = writeU16(view, offset, 0x0800);
    offset = writeU16(view, offset, 0);
    offset = writeU16(view, offset, 0);
    offset = writeU16(view, offset, 0);
    offset = writeU32(view, offset, record.crc);
    offset = writeU32(view, offset, record.bytes.length);
    offset = writeU32(view, offset, record.bytes.length);
    offset = writeU16(view, offset, record.name.length);
    offset = writeU16(view, offset, 0);
    offset = writeU16(view, offset, 0);
    offset = writeU16(view, offset, 0);
    offset = writeU16(view, offset, 0);
    offset = writeU32(view, offset, 0);
    offset = writeU32(view, offset, record.offset);
    out.set(record.name, offset);
    offset += record.name.length;
  }

  offset = writeU32(view, offset, 0x06054b50);
  offset = writeU16(view, offset, 0);
  offset = writeU16(view, offset, 0);
  offset = writeU16(view, offset, records.length);
  offset = writeU16(view, offset, records.length);
  offset = writeU32(view, offset, offset - centralOffset);
  offset = writeU32(view, offset, centralOffset);
  writeU16(view, offset, 0);
  return out;
}
