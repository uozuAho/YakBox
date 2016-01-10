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
- doesn't run on android 5.1
- pre-release
    + check code todos
    + compare with current release
    + update app store images

## other
- highlight selected items in load screen
    + can't simply make a background color selector, as this overrides
      long click animation
- 'load' fragment for bigger screens
- user can rename shared recording?
- share:
    + facebook: https://developers.facebook.com/docs/sharing/android
        * can't share audio files directly. Upload to soundcloud?
    + twitter: same as fb
- centralise error toasts, put error messages in string resources
- file io on separate thread
- change to MVP architecture
- options / settings
    + don't rotate screen
    + normalise recordings
    + strip silence @ start & end
