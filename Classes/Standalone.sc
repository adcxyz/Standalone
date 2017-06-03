
Standalone {

	classvar <>internalExtDirName = "InternalExtensions", <internalExtDir;
	classvar <lockupFilename = "StandaloneLocks.scd";

	*initClass {
		internalExtDir = String.scDir +/+ internalExtDirName;
		if (this.locksOnStartup) { this.activate };
	}

	*isInClassLib { |post = true|
		var res = this.filenameSymbol.asString.dirname.contains("SCClassLibrary");
		if (res.not and: post) {
			"*** Standalone.sc is not in class lib yet.\n"
			"// Please move it there to use it:\n"
			"Standalone.moveToClassLib;\n"
			"thisProcess.recompile;".postln;
		};
		^res
	}

	// this happens when already moved to internalExtDir,
	// so better to move the files rather than copying them.
	*moveToClassLib {
		var currPath, newPath;
		var classFile, codeString;
		var schelpPath, newSchelpPath;
		if (this.isInClassLib(false)) { ^false };

		currPath = this.filenameSymbol.asString;
		newPath = String.scDir +/+ "SCClassLibrary/" +/+ currPath.basename;

		if (currPath != newPath) {
			"*** Standalone.sc is moved to internal class lib: ***".postln;
			unixCmd(("mv -f" + quote(currPath) + quote(newPath)).postcs);

			schelpPath = Standalone.filenameSymbol.asString.dirname.dirname
			+/+ "HelpSource/Standalone.schelp";
			if(File.exists(schelpPath)) {
				newSchelpPath = String.scDir +/+ "HelpSource/Standalone.schelp";
				unixCmd(("mv -f" + quote(schelpPath) + quote(newSchelpPath)).postcs);
			}
		};
		^true
	}

	*lockupPath { ^( String.scDir +/+ lockupFilename) }

	*locksOnStartup {
		var foundPath;
		if (this.isInClassLib(false).not) {
			"*** Standalone: cannot lock yet - ".postln;
			^false
		};
		foundPath = this.lockupPath.pathMatch.first;
		^if (foundPath.notNil) { foundPath.load == true } { false }
	}

	*lock {
		if (this.isInClassLib.not) {
			"Standalone cannot lock yet.".postln;
			^this
		};
		File(this.lockupPath, "w").write("true").close;
		"Standalone is locked now.".postln;
	}
	*unlock {
		if (this.isInClassLib.not) {
			"Standalone cannot lock or unlock yet.".postln;
			^this
		};
		File.delete(this.lockupPath);
		"Standalone is unlocked now.".postln;
	}

	*activate {
		var changed = false;
		if (this.isInClassLib(false).not) {
			this.moveToClassLib;
			changed = true;
		};
		if (this.locksOnStartup.not) {
			this.lock;
			changed = true;
		};

		if (this.checkAndSetDirPaths) {
			changed = true;
		};
		if (changed) {
			"*** Standalone is active and uses only internal Extensions. ***".postln;
			this.stopAndProposeReboot;
		};
	}

	*checkAndSetDirPaths { |force = true|
		var changed = false;
		"Standalone class checking directories... ".postln;
		changed = changed or: this.clearIncludePaths(force);
		changed = changed or: this.excludeExternalExtDirs(force);
		changed = changed or: this.includeInternalExtDir(force);
		^changed
	}

	*stopAndProposeReboot {

		"*** PLEASE REBOOT INTERPRETER NOW! ***".postln;
		"*** Standalone should boot normally then. ***".postln;
		"\n\n\n".postln;

		// stop code execution here when booting and config was changed!
		// should only happen the first time, no complaints after reboot
		this.halt;
	}

	*clearIncludePaths { |clearAll = false|
		var didClear = false;

		LanguageConfig.includePaths.copy.do { |path|
			if (clearAll or: { path.pathMatch.isEmpty }) {
				if (internalExtDir != path) {
					didClear = true;
					LanguageConfig.removeIncludePath(path);
				};
			};
		};
		if (didClear) {
			LanguageConfig.store(LanguageConfig.currentPath);
			"Standalone: cleared includePaths.".postln;
		};
		^didClear;
	}

	*excludeExternalExtDirs { |clearAll = false|
		var changed = false;
		var toExclude = [Platform.userExtensionDir, Platform.systemExtensionDir];

		LanguageConfig.excludePaths.copy.do { |path|
			if (clearAll or: { path.pathMatch.isEmpty }) {
				if (toExclude.includesEqual(path).not) {
					LanguageConfig.removeExcludePath(path);
					changed = true;
				};
			};
		};

		toExclude.do { |extdir|
			if (LanguageConfig.excludePaths.includesEqual(extdir).not) {
				LanguageConfig.addExcludePath(extdir);
				changed = true;
			};
		};
		if (changed) {
			"Standalone: excluded external extension dirs.".post;
			LanguageConfig.store(LanguageConfig.currentPath);
		};
		^changed
	}

	*includeInternalExtDir {
		if (internalExtDir.pathMatch.isEmpty) {
			"Standalone: creating internal extension dir.".postln;
			internalExtDir.mkdir;
		};
		if (LanguageConfig.includePaths.includesEqual(internalExtDir).not) {
			"Standalone: adding internalExtDir now: %\n".postf(internalExtDir);
			LanguageConfig.addIncludePath(internalExtDir);
			LanguageConfig.store(LanguageConfig.currentPath);
			^true
		} {
			"This standalone app uses an internal extension dir.".postln;
			^false
		}
	}
}
