package com.cahue.iweco;

/**
 * Interface for controlling when details fragment are displayed
 */
public interface DetailsViewManager {

    DetailsFragment getDetailsFragment();

    void setDetailsFragment(AbstractMarkerDelegate caller, DetailsFragment fragment);

    void hideDetails();

}
