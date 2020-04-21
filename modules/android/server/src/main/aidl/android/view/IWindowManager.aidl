package android.view;

import android.view.IRotationWatcher;

interface IWindowManager {
    void getBaseDisplaySize(int displayId, out Point size);
    int getDefaultDisplayRotation();
    int watchRotation(IRotationWatcher watcher, int displayId);
}