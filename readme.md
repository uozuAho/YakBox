YakBox
======

A YakBak emulator for Android. Record sounds, play them back in forward
and reverse, at various speeds.

# Bugs
- Recording & rotating device can sometimes cause crash, then can't
  reopen app until after reboot (can't init audio record).
    + Sometimes this happens even without rotating the device

# Todo
- use executor service to run audio reader?
- sync access to shared data
- find storage location, put on 'about' page
- longer recording time
- share: email, facebook, twitter etc.
- options / settings
    + think of some options...colour scheme?
    + normalise recordings
- send / receive yaks
- delete multiple recordings
- load/save as icons?
