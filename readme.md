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
- fix layout - doesn't look good in landscape on my phone
    + action bar too
- load/save as icons?
- tests

## Unscheduled
- options / settings
    + think of some options...colour scheme?
    + normalise recordings
- refactor
    + MainActivity's getting big
- send / receive yaks
- delete multiple recordings
