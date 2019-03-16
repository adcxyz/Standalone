/******* THIS IS THE INTERNAL startup file within a Standalone.app.

Modify as you see fit, especially which project folder to load...

********/

/////// run standalone self test first - stops if it runs into problems
"standaloneSelfRepair.scd".loadRelative;

// initial startup file for macOS standalone.
if (Platform.userAppSupportDir.contains(Standalone.appName)) {
	'YUHU! app if independent!'.postln;
	unixCmd("say Welcome to % standalone!".format(Standalone.appName));

	// open this startupfile in the new app
	thisProcess.nowExecutingPath.openOS;

} {
	'OH NO! app still uses SuperCollider userAppSupportDir ...'.postln;
	this.halt;
};

//////// activate Standalone mode
try {
	Standalone.openStartup;
	Standalone.activate;
};

///// example for loading a standalone project folder:

// make a global dictionary q for stuff
q = q ? ();

////// set the path to the project folder:
/// q.projdir = "~/Desktop/StehAllein_proj";
// set to example folder next to standalone:
q.projdir = String.scDir.dirname.dirname.drop(-4) ++ "_proj";

// name of the loadfile
q.loadfilename = "00_load*.scd";

// a function to load code files which can wait and posts loading info:
q.load = { |q, filename, preText = "", postText = "", dirsup = 0, wait = 0.1|
	forkIfNeeded ({
		var here = "".resolveRelative;
		var loadDur, t0 = Main.elapsedTime;
		dirsup.do { here = here.dirname };
		"*** loading % : %\n".postf(filename, preText);
		(here +/+ filename).loadPaths.postln;
		if (s.serverRunning) { try { s.sync } };
		loadDur = (Main.elapsedTime - t0).round(0.001);
		"... loading took % secs. %\n---\n\n".postf(loadDur, postText);
		wait.wait;
		"";
	}, AppClock)
};

//// and now, redirect to the project startupfile outside this app,
//// in the correct Projects folder for this computer:
(q.projdir +/+ q.loadfilename).loadPaths;

