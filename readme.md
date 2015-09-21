YakBox
======

A YakBak emulator for Android. Record sounds, play them back in forward
and reverse, at various speeds.

# Bugs
- Recording & rotating device can sometimes cause crash, then can't
  reopen app until after reboot (can't init audio record).
    + Sometimes this happens even without rotating the device

# Todo
## save/load
- make item text smaller in load dialog
- show keyboard on save dialog
- ability to delete recordings
    + might need a dedicated load view, hold to mark/delete item
- all other todos relating to save/load/delete

## Unscheduled
- options / settings
    + think of some options...colour scheme?
- add user guide
    + written & graphical format (overlay info on GUI)
- refactor
    + MainActivity's getting big
