# Real-Time "Google Maps" Experience Implementation Plan

This plan aims to refine the location tracking and UI to provide a professional, highly responsive experience similar to Google Maps.

## User Review Required

- **Automatic Following**: When "Follow Mode" is active, the map will automatically center on the blue dot as you move. Any manual drag/scroll by the user will disable this mode until the location button is pressed again (matching Google Maps behavior).
- **Map Aesthetics**: I will simplify building colors and add more contrast to the paths to make it look cleaner ("nitido").

## Proposed Changes

### Coordinate Precision & Projection

#### [MapDataLoader.kt](file:///C:/Users/nando/AndroidStudioProjects/UAM_MAP/app/src/main/java/com/example/uammap/utils/MapDataLoader.kt)
- Increase `MAP_SIZE` if necessary for better internal precision.
- Refine the `project` function to ensure consistent behavior across different screen sizes.

---

### Enhanced UI & Following Mode

#### [HomeScreen.kt](file:///C:/Users/nando/AndroidStudioProjects/UAM_MAP/app/src/main/java/com/example/uammap/screens/HomeScreen.kt)
- Add a `isFollowingUser` Boolean state.
- Update the location button to toggle `isFollowingUser`.
- Implement a `LaunchedEffect` that observes `userLocation` and updates `offsetX`/`offsetY` in real-time when `isFollowingUser` is true.
- Detect manual gestures to set `isFollowingUser = false`.
- Improve the blue dot visual: add a larger semi-transparent "accuracy" circle and a glowing effect.
- Simplify building rendering: use cleaner borders and more consistent colors.

---

### Stability & Permissions

#### [MainActivity.kt](file:///C:/Users/nando/AndroidStudioProjects/UAM_MAP/app/src/main/java/com/example/uammap/MainActivity.kt)
- Ensure location updates start as soon as permission is granted without requiring a restart.

## Verification Plan

### Manual Verification
- **Follow Mode**: Press the location button. Simulate GPS movement in the emulator. Verify the map camera follows the blue dot automatically.
- **Manual Override**: While following, drag the map. Verify that the map stops following the dot.
- **Visual Polish**: Verify buildings have clean lines and the blue dot is easily visible on all parts of the campus.
- **Accuracy**: Verify that the blue dot appears within the campus boundaries (using emulator coordinates known to be inside UAM).
