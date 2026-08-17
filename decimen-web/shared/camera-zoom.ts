export const MIN_CAMERA_ZOOM = 1;
export const MAX_CAMERA_ZOOM = 10;
export const CAMERA_ZOOM_STEP = 0.1;

export function clampCameraZoom(value: number): number {
  const finite = Number.isFinite(value) ? value : MIN_CAMERA_ZOOM;
  const clamped = Math.min(MAX_CAMERA_ZOOM, Math.max(MIN_CAMERA_ZOOM, finite));
  return Math.round(clamped * 10) / 10;
}

export function wheelCameraZoom(current: number, deltaY: number): number {
  return clampCameraZoom(current * Math.exp(-deltaY * 0.0015));
}

export function pinchCameraZoom(
  startZoom: number,
  startDistance: number,
  currentDistance: number,
): number {
  if (startDistance <= 0 || currentDistance <= 0) return clampCameraZoom(startZoom);
  return clampCameraZoom(startZoom * (currentDistance / startDistance));
}

export interface ZoomCrop {
  x: number;
  y: number;
  width: number;
  height: number;
}

/** Center crop that is stretched back to the capture dimensions for decode. */
export function cameraZoomCrop(frameWidth: number, frameHeight: number, zoom: number): ZoomCrop {
  const z = clampCameraZoom(zoom);
  const width = Math.max(1, Math.round(frameWidth / z));
  const height = Math.max(1, Math.round(frameHeight / z));
  return {
    x: Math.floor((frameWidth - width) / 2),
    y: Math.floor((frameHeight - height) / 2),
    width,
    height,
  };
}
