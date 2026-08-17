export type CameraFacing = "environment" | "user";

export function normalizeCameraFacing(value: string | undefined): CameraFacing {
  return value === "user" ? "user" : "environment";
}

export function oppositeCameraFacing(value: CameraFacing): CameraFacing {
  return value === "user" ? "environment" : "user";
}
