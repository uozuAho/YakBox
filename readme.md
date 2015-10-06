YakBox
======

A YakBak emulator for Android. Record sounds, play them back in forward
and reverse, at various speeds.

# Bugs
- Copying to buffer after AudioRecord.stop() on Android 4.1.2 doesn't work -
  have to copy to buffer while still recording. Probably have to make a copy
  thread.
- Recording & rotating device can sometimes cause crash, then can't
  reopen app until after reboot (can't init audio record).
    + Sometimes this happens even without rotating the device

# Todo
- update or remove appcompat dependency
- add 'about' screen