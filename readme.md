YakBox
======

A YakBak emulator for Android. Record sounds, play them back in forward
and reverse, at various speeds.

# Bugs
- Recording & rotating device can sometimes cause crash, then can't
  reopen app until after reboot (can't init audio record).
    + Sometimes this happens even without rotating the device

# Todo
- save/load
    + full 5s of audio buffer is getting saved - only save up to numSamples
- ability to delete recordings
    + might need a dedicated load view, hold to mark/delete item
- options / settings
    + think of some options...
- add user guide
    + written & graphical format (overlay info on GUI)