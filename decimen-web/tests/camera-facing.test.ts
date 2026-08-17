import assert from "node:assert/strict";
import test from "node:test";
import {
  normalizeCameraFacing,
  oppositeCameraFacing,
} from "../shared/camera-facing.ts";

test("camera facing normalizes missing or unfamiliar values to the rear camera", () => {
  assert.equal(normalizeCameraFacing(undefined), "environment");
  assert.equal(normalizeCameraFacing("left"), "environment");
  assert.equal(normalizeCameraFacing("user"), "user");
});

test("camera facing toggles between rear and front", () => {
  assert.equal(oppositeCameraFacing("environment"), "user");
  assert.equal(oppositeCameraFacing("user"), "environment");
});
