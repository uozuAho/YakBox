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
- overwrite warning on save
- filter by input on load recording
- show similar filenames in save dialog
- all other todos relating to save/load/delete
- fix layout - doesn't look good in landscape on my phone
- load/save as icons
- delete multiple recordings

## Unscheduled
- add 'about' screen
- options / settings
    + think of some options...colour scheme?
    + normalise recordings
- add user guide
    + written & graphical format (overlay info on GUI)
- refactor
    + MainActivity's getting big
