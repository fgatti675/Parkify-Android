package com.cahue.iweco;

/**
 * Interface for controlling when details fragment are displayed
 */
public interface DetailsViewManager {

    DetailsFragment getDetailsFragment();

    void setDetailsFragment(DetailsFragment fragment);

    void hideDetails();

}
