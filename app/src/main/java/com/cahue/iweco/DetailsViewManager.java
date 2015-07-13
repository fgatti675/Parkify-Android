package com.cahue.iweco;

/**
 * Created by f.gatti.gomez on 02/07/15.
 */
public interface DetailsViewManager {

    DetailsFragment getDetailsFragment();

    void setDetailsFragment(DetailsFragment fragment);

    void hideDetails();

}
