/******* THIS IS THE INTERNAL startup file within a Standalone.app.

Modify as you see fit, especially which project folder to load...

********/

// make a global dictionary q for stuff
q = q ? ();

////////// open this startupfile in the app
thisProcess.nowExecutingPath.openOS;

///////////////// TELL the server program where to read plugins from //////////
/*
Explanation: by default, scsynth reads plugins from two places:
1. Those packaged inside the app (at String.scDir +/+ "plugins")
---> this is as intended
2. From ~/Library/Application Support/SuperCollider/Extensions
---> this creates an unpredictable dependency of the standalone on circumstances:
     if the user on that computer has used SuperCollider and installed sc3-plugins,
     these will be present and loaded; if not, they wont be there.
*/

///// Option 1 - minimal: read just the plugins inside the app, no extensions
s.options.ugenPluginsPath = String.scDir +/+ "plugins";

///// Option 2 - more plugins, still self-contained: read internal plugins
///// PLUS any plugins found in the Standalone's internal Extensions dir.
////// this is the recommended option for Standalones!
s.options.ugenPluginsPath =
([ String.scDir +/+ "plugins" ]
++ [Standalone.internalExtDir]).join($:);

// ///// Option 3 - more flexible, less self-contained: read internal plugins
// ///// PLUS those in internalExtDir, and the Standalone 's userAppSupportDir:
// s.options.ugenPluginsPath =
// ([ String.scDir +/+ "plugins" ]
// ++ [
// 	Standalone.internalExtDir,
// 	Platform.userAppSupportDir
// ]).join($:)


Task {

	var standaloneClass = \Standalone.asClass;
	/////// run standaloneSelfRepair script first
	/// - which stops if it runs into problems
	"standaloneSelfRepair.scd".loadRelative;

	if (standaloneClass.isNil) {
		"*** The Standalone class is not present - please put it in .../Resources/SCClassLibrary!".warn;
		"*** STOPPING HERE!".postln;
		this.halt;
	};

	//////// activate Standalone mode
	try { standaloneClass.activate };

	///// example for loading a standalone project folder:


	////// set the path to the project folder:
	/// q.projdir = "~/Desktop/StehAllein_proj";
	// set to an example folder next to the <Standalone>.app:
	q.projdir = String.scDir.dirname.dirname.drop(-4) ++ "_proj";

	// name of the loadfile begins with ""00_load
	q.loadfilename = "00_load*.scd";

	// define a function to load code files which can wait and posts loading info:
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

}.play(AppClock);

