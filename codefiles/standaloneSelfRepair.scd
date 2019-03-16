/*

THIS FILE REPAIRS Standalone apps made with SC3.

1. It checks whether macOS puts the app under "quarantine"
	and if so, advises how to delete the quarantine label.

2. It checks whether the Standalone will interfere with SC3,
	warns if it does, and advises to remake the standalone.

3. It adds a list of folders to the classlib configuration file,
	typically a single folder with all quarks to be added

4. It removes non/existent dirs from the includePaths,
	to avoid problems with relative paths after that may
	arise when moving the app to a different location.


//// to test filtering, add a folder to LanguageConfig that will be removed:
LanguageConfig.addIncludePath("~/Desktop/blabla".standardizePath);

// install a small quark to test removal:
Quarks.install("DayTimer");

*/


var classDirsToAdd = [
	/////// RECOMMENDED: ONLY add a path to extensions inside the app,
	/////// where all quarks to be included are copied to:
	String.scDir +/+ "InternalExtensions";
	/////// OR SECOND CHOICE: add a relative path directly next to <Stehallein>.app folder...
	// String.scDir.dirname.dirname.dirname +/+ "InternalExtensions"
].collect(_.standardizePath);

var classDirPlacesToKeep = [
	// RECOMMENDED: LEAVE NO OTHER PLACES where quarks live.
	//// in case you really want quarks in the default location for this app to be kept:
	// Platform.userAppSupportDir
].collect(_.standardizePath);

/// if true, app will recompile automagically on startup
/// when this script had to change any includePath:
var autoRecompile = true;

var currIncludePaths = LanguageConfig.includePaths;


"\n\n*** standaloneSelfRepair.scd script runs ...\n\n".postln;

////// 1. check quarantine..
if (Main.filenameSymbol.asString.contains("/private/var")) {
	"\n*** This Standalone has been put under quarantine by the mac OS.\n"
	"\n To fix this:\n"
	"1. Open the Terminal.app program,\n"
	"2. copy the line of shell code below there, \n"
	"3. replace <path/to/standalone.app> with the actual path to the standalone:"
	"\nxattr -d com.apple.quarantine <path/to/standalone.app>\n".warn;

	"\n*** STOPPING HERE.\n\n".postln;
	this.halt;
} {
	"*** OK: app is not quarantined.".postln;
};

////// 2. check independence!
if (Platform.userAppSupportDir.split($/).last == "SuperCollider") {
	"\n*** This Standalone will interfere with SuperCollider!"
	"\n*** Please remake it! \n*** STOPPING HERE.".warn;
	this.halt;
} {
	"*** OK: userAppSupportDir will not interfere with SuperCollider.".postln;
};

////// post info
"\n*** LanguageConfig.currentPath : ".postln;
LanguageConfig.currentPath.postcs;

// add the ones that should be included
classDirsToAdd.do { |path|
	LanguageConfig.addIncludePath(path);
};

//////// filter unwanted ones:
LanguageConfig.includePaths.do { |path|
	if (File.exists(path).not) {
		"removing non-existing includePath: ".postln;
		LanguageConfig.removeIncludePath(path.postcs);
	};
	// if not in the positive list ...
	if (classDirsToAdd.includesEqual(path).not
		and: {
			// or in the allowed places ...
			classDirPlacesToKeep.any(path.beginsWith(_)).not
	}) {
		"*** removing includePath: ".postln;
		path.postcs;
		LanguageConfig.removeIncludePath(path);
	}
};

////// always store, with the current config file name:
LanguageConfig.store(LanguageConfig.currentPath);


//////////// recompile automagically or by hand:
if (currIncludePaths == LanguageConfig.includePaths) {
	"... no includePaths changes, can just keep going  ...".postln
} {
	"*** includePaths have changed!".postln;
	if (autoRecompile) {
		"*** will autoRecompile in 3 seconds ...\n\n\n".postln;
		forkIfNeeded {
			(3..1).do { |i| i.postln; 1.wait };
			thisProcess.recompile;
		}
	} {
		"*** Please reboot interpreter by hand!".postln;
		"\n*** STOPPING HERE.\n\n".postln;
		this.halt;
	}
};

