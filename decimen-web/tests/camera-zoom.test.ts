import assert from "node:assert/strict";
import test from "node:test";
import {
  cameraZoomCrop,
  clampCameraZoom,
  pinchCameraZoom,
  wheelCameraZoom,
} from "../shared/camera-zoom.ts";

test("camera zoom clamps and snaps to tenths", () => {
  assert.equal(clampCameraZoom(0.4), 1);
  assert.equal(clampCameraZoom(2.26), 2.3);
  assert.equal(clampCameraZoom(9), 9);
  assert.equal(clampCameraZoom(30), 10);
  assert.equal(clampCameraZoom(Number.NaN), 1);
});

test("pinch out zooms in and pinch in zooms out", () => {
  assert.equal(pinchCameraZoom(1, 100, 200), 2);
  assert.equal(pinchCameraZoom(2, 200, 100), 1);
  assert.equal(pinchCameraZoom(2, 0, 300), 2);
});

test("mouse wheel changes zoom in the expected direction", () => {
  assert.ok(wheelCameraZoom(2, -120) > 2);
  assert.ok(wheelCameraZoom(2, 120) < 2);
});

test("digital zoom makes a centered crop with the original aspect ratio", () => {
  assert.deepEqual(cameraZoomCrop(1280, 720, 2), {
    x: 320,
    y: 180,
    width: 640,
    height: 360,
  });
  assert.deepEqual(cameraZoomCrop(1280, 720, 1), {
    x: 0,
    y: 0,
    width: 1280,
    height: 720,
  });
});
