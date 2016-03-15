package com.khasang.vkphoto.presentation.presenter.albums;

import android.support.v7.view.ActionMode;
import com.bignerdranch.android.multiselector.MultiSelector;
import com.khasang.vkphoto.R;
import org.greenrobot.eventbus.EventBus;

public abstract class AlbumsPresenterBase implements AlbumsPresenter {
    protected ActionMode actionMode;

    @Override
    public void onStart() {
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
    }

    public void checkActionModeFinish(MultiSelector multiSelector) {
        int size = multiSelector.getSelectedPositions().size();
        if (size == 0) {
            if (actionMode != null) {
                actionMode.finish();
            }
        } else if (size == 1) {
            actionMode.getMenu().findItem(R.id.action_edit_album).setVisible(true);
        } else {
            actionMode.getMenu().findItem(R.id.action_edit_album).setVisible(false);
        }
    }

    @Override
    public void initialize() {

    }
}
