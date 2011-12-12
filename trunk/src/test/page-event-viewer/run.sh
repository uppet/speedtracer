#!/bin/sh

CHROME=
if [ $# -eq 1 ]; then
  CHROME=$1
else
  case $OSTYPE in
    linux-gnu)
      CHROME=/usr/bin/google-chrome
    ;;
    darwin*)
      CHROME=/Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome
    ;;
    *)
      echo 1>&2 "I don't know where chrome is located on your computer"
      exit 1
    ;;
  esac
fi

"${CHROME}" \
  --no-first-run \
  --enable-extension-timeline-api \
  --load-extension=. \
  --enable-experimental-extension-apis \
  --user-data-dir=/tmp/speedtracer-test-profile
