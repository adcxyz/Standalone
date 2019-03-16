// example for useful conveniences

q = q ? ();

// helpful things to find all the files in this setup folder:
q.utilDir = thisProcess.nowExecutingPath.dirname;
q.topDir = q.utilDir.dirname;
q.fulldirnames = (q.topDir +/+ "*/").pathMatch;
q.dirnames = q.fulldirnames.collect { |path| path.basename };

"/// q.topDir: %\n".postf(q.topDir);
"/// q.dirnames: ".postln;
q.dirnames.printAll;

q.warnWin = { |q, string="?!?", title="?!?"|
	var w = Window("Warning: %".format(title)).front.alwaysOnTop_(true);
	var tv = StaticText(w, w.view.bounds);
	tv.background_(Color(1, 0.75));
	tv.align_(\center);
	w.alpha_(0.9);
	tv.string_(string).font_(Font("Arial", 24));
	w
};

/*
q.warnWin("Test - Something in the setup went wrong, please do something or other", "OOPS!");
*/