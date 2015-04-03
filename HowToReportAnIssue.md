Thank you for taking the time to use Speed Tracer and report a problem.  The issue tracker is for reporting bugs and feature requests only, so if you are having trouble and are not sure it is a bug, please use the [speedtracer Google Group](http://groups.google.com/group/speedtracer).

## If you think you have found a bug, follow these steps ##

  1. Check the [FAQ](http://code.google.com/webtoolkit/speedtracer/faq.html) for some well known problems and their solutions.
  1. Search for an existing issue in the [issue tracker](http://code.google.com/p/speedtracer/issues/list) and on the [speedtracer](http://groups.google.com/group/speedtracer) Google Group.  If you find the issue has already been reported to the issue tracker, please click the star icon so we can get an idea of how many people are being affected by this problem.
  1. If you've found an unreported issue, post a message to the [speedtracer](http://groups.google.com/group/speedtracer) group along with the steps to reproduce it and the developers will determine if it is a bug that should be tracked.
  1. If you get no response or the developers request a new issue, go ahead and  [add an issue to the issue tracker](http://code.google.com/p/speedtracer/issues/entry).

## Information to include in a bug report ##

  * A description of the buggy behavior observed.
  * A description of the behavior you expected to see.
  * The version of Speed Tracer (check the [chrome://extensions chrome://extensions url]).
  * The build of Chrome you are using (check the About Chrome dialog).
  * The platform you are using (Windows XP, Mac OSX Leopard, Ubuntu Linux 9, etc.)
  * A copy of any exceptions in the Speed Tracer monitor window.

> To find exceptions in the monitor window, right click on the monitor window and choose 'Inspect element'.  Then, open up the console window (second button from the left at the bottom of the screen)
> It is normal to see a number of "Worker script imported" messages.  What we are looking for are any JavaScript exceptions.  We'd like a copy of any of those.

  * A copy of any exceptions in the Speed Tracer extension.

> To find exceptions in the extension, open a new tab, navigate to [chrome://extensions chrome://extensions].  Then, click on background.html link to open up the Web Inspector.  Again, open up the console window (second button from the left at the bottom of the screen) and look for JavaScript exceptions there.

  * If relevant, attach a screenshot of the Speed Tracer Monitor window.