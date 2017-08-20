package com.cahue.iweco;

/**
 * Interface defining the possible navigation options from the main screen.
 */
public interface Navigation {

    void signOutAndGoToLoginScreen(boolean resetPreferences);

    void goToPreferences();

    void openDonationDialog();

    void goToCarManager();

}
