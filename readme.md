YakBox
======

A YakBak emulator for Android. Record sounds, play them back in forward
and reverse, at various speeds.

# Bugs
- Recording & rotating device can sometimes cause crash, then can't
  reopen app until after reboot (can't init audio record).
    + Sometimes this happens even without rotating the device

# Todo
## toolbar
- follow http://developer.android.com/training/appbar/setting-up.html
- load icon
- remove title from toolbar (invisible but still taking up room?)
- load activity toolbar
    + back
    + search
    + multi-select + delete
- pre-release: compare with current release
## other
- share:
    + facebook: https://developers.facebook.com/docs/sharing/android
        * can't share audio files directly. Upload to soundcloud?
    + twitter: same as fb
- dark menus (currently white bg)
- file io on separate thread
- options / settings
    + normalise recordings
    + strip silence @ start & end
- delete multiple recordings
- load/save as icons?
