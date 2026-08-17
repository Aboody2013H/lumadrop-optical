// Post-transfer support link — hosted builds only. The standalone build swaps
// this module for support.inline.ts (see build/use-inline-variants.ts), so a
// downloaded artifact never solicits; the code is not even parsed there.
// One quiet line, shown only after the app has actually delivered a file.
import { msg } from "../shared/i18n";

export function supportLink(): HTMLElement | null {
  const p = document.createElement("p");
  p.className = "support-after";
  const a = document.createElement("a");
  a.href = "https://buymeacoffee.com/bashalarmist";
  a.target = "_blank";
  a.rel = "noopener noreferrer";
  a.textContent = msg.receive.supportAfter;
  p.append(a);
  return p;
}
