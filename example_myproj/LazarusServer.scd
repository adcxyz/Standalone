/*
LAZARUS - server rises from the dead if it dies for whatever reason.

// tests that it wakes up if
// it has quit
s.quit;

// the system killed it
unixCmd("killall scsynth supernova");

// SC kills it
Server.killAll;

// it becomes unresponsive;
s.addr.port_(57105);

*/

q = q ? ();
q.laz = ();
q.laz.skip.stop;

q.laz.counter = 3;
/// awakes when no activity for a long time.
q.laz.skip = SkipJack({

	if (s.serverRunning.not or: { s.unresponsive }) {
		q.laz.counter = q.laz.counter + 1;
		"*** Lazarus counting up: %\n".postf(q.laz.counter);
		if (q.laz.counter > 2) {
			"*** Lazarus waking up server s!".postln;
			fork {
				q.laz.counter = 0;
				if (s.serverBooting.not) {
					Server.killAll;
					1.wait;
					s.boot;
				};
			}
		}
	} {
		// "... server is up, lazarus ok...".postln;
		q.laz.counter = 0;
	}
}, 3, name: \lazarus);

