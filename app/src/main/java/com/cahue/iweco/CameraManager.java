package com.cahue.iweco;

import com.google.android.gms.maps.CameraUpdate;

/**
 * Interface used by delegates to update the camera location
 */
public interface CameraManager {

    /**
     *
     * @param cameraUpdate
     * @param cameraUpdateRequester
     */
    void onCameraUpdateRequest(CameraUpdate cameraUpdate, CameraUpdateRequester cameraUpdateRequester);

}
