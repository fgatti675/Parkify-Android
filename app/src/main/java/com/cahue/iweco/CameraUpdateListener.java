package com.cahue.iweco;

import com.google.android.gms.maps.CameraUpdate;

/**
 * Interface used by delegates to update the camera location
 */
public interface CameraUpdateListener {
    void onCameraUpdateRequest(CameraUpdate cameraUpdate);
}
