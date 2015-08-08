YakBak
======

A YakBak emulator for Android.

# TODO
- bug: audiorecord sometimes fails to initialise on config
  change. Hang on to the audio objects with a fragment.
- test on other emulators (4.2+, 3.0+)
    + Try to get working on 4.0, then test other emulators
- error reporting (https://github.com/ACRA/acra)
    + make sure unhandled exceptions are also logged, with a custom
      error report to the user
- change external storage location
    + ie. follow how other apps do it, eg aho.uozu.data or whatever