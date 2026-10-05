#!/usr/bin/env bash
# Captures README screenshots of the sample app on a running Android TV emulator.
# D-pad presses can't be timed reliably through adb on a fresh emulator, so the sample sets up
# each scene from the `scene` extra. Scenes use a fake player over generated frames: no network.
set -euo pipefail
source "$(dirname "$0")/screenshot-lib.sh"

install_sample

fresh_launch --es scene playing
capture tv-playing "The Lantern Coast"

fresh_launch --es scene scrubbing
capture tv-scrubbing "Storm warning"

fresh_launch --es scene skipintro
capture tv-skipintro "Skip intro"

fresh_launch --es scene tracks
capture tv-tracks "Subtitles"
