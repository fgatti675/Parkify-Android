package com.bahpps.cahue;

import com.google.android.gms.maps.CameraUpdate;

/**
 * Interface used by delegates to update the camera location
 */
public interface CameraUpdateListener {
    void onCameraUpdateRequest(CameraUpdate cameraUpdate);
}
