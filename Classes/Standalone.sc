
Standalone {

	classvar <>internalExtDirName = "InternalExtensions", <internalExtDir;
	classvar <lockupFilename = "StandaloneLocks.scd";

	*initClass {
		internalExtDir = String.scDir +/+ internalExtDirName;
		if (this.locksOnStartup) { this.activate };
	}

	////////// NOT WORKING YET
	// - STILL GETS SuperCollider userAppSupportDir sometimes - why?!?

	*export { |newAppName = "StehAllein", location = "~/Desktop/"|

		// make some needed paths and folders
		var newAppLocation = location.standardizePath;
		var pathToThisApp = String.scDir.dirname.dirname;
		var thisAppName = pathToThisApp.basename.splitext.first;
		var pathToNewApp = newAppLocation +/+ newAppName ++ ".app";
		var newAppResDir = pathToNewApp +/+ "Contents/Resources";
		var newAppSupportDir = Platform.userConfigDir.dirname +/+ newAppName;

		var infoPListPath = pathToNewApp +/+ "Contents/Info.plist";
		var infoString, executableIndex, nameToReplace, newInfoString;
		var overWritesDir;

		if (File.exists(pathToNewApp)) {
			"App at % already exists! please delete or move it elsewhere before exporting.".warn;
			^this
		};

		forkIfNeeded {
			var cond = Condition();

			// copying the current app with new name to new location:
			"% - copying new app to:".postf(thisMethod);
			unixCmd(("cp -ir"
				+ quote(pathToThisApp.postcs)
				+ quote(pathToNewApp.postcs)), { cond.unhang }
			);
			cond.hang;

			if (newAppLocation.pathMatch.isEmpty) {
				"% - copying app failed! stopping export.".postln;
				^this
			};

			File.mkdir(newAppSupportDir);

			0.2.wait;

			///////// FIXUPS in the new app:
			// a. change the Info.plist file by replacing the app name:
			// get its string, replace the SC names, write it out again

			if (infoPListPath.pathMatch.isEmpty) {
				"infoPlist file not dound! stopping export.".warn;
				^this
			};

			infoString = File.use(infoPListPath, "r", (_.readAllString));
			executableIndex = infoString.find("<key>CFBundleExecutable</key>");
			nameToReplace = infoString.copyRange(
				infoString.find("<string>", offset: executableIndex) + 8,
				infoString.find("</string>", offset: executableIndex) - 1
			);

			"replacing bundleName % in plist file found at these indices: "
			.postf(nameToReplace.cs);
			infoString.findAll(nameToReplace).postln;

			newInfoString = infoString.replace( nameToReplace, newAppName );
			unixCmdGetStdOut("mv" + quote(infoPListPath) + quote(infoPListPath ++ "BK"));
			File.use(infoPListPath, "w", { |f| f.write(newInfoString) });

			// b. rename the binary file to match:
			"renaming macos binary to: ".post;
			// fixups in the new app - 2. rename the binary inside the app folder
			unixCmdGetStdOut("mv -i"
				+ quote(pathToNewApp +/+ "Contents/MacOS/" +/+ nameToReplace)
				+ quote(pathToNewApp +/+ "Contents/MacOS/" ++ newAppName)
			);

			0.2.wait;

			// 4. write a class extension file to look for the startupFile
			// in the app itself, in String.scDir for self-containment.
			overWritesDir = newAppResDir +/+ "SCClassLibrary/SystemOverwrites";
			File.mkdir(overWritesDir);
			File.use((overWritesDir +/+ "extModStartupFile.sc").postcs, "w", { |f|
				f.write(this.startupExtCode)
			});

			// 5. write a basic startupFile:
			File.use((newAppResDir +/+ "startup.scd").postcs, "w", { |f|
				f.write(this.startupFileText)
			});

			0.2.wait;

			(3..0).do { |i| if (i.postln > 0) { 1.wait } };
			// wakeup kiss:
			unixCmd("open" + pathToNewApp);

		}
	}

	*appName { ^String.scDir.dirname.dirname.basename.splitext.first }

	*startupExtCode {
		^
		"+ OSXPlatform {
startupFiles {
^[String.scDir +/+ \"startup.scd\"]
}
}"
	}

	*startupFileText {
		^
		"// basic startup file for macOS standalone.
if (Platform.userAppSupportDir.contains(Standalone.appName)) {
'YUHU! app if independent!'.postln;
s.waitForBoot { Pbind('degree', Pseries([0, 2, 4], 1, 8), 'dur', 0.125).play }
} {
'OH NO! app still uses SuperCollider userAppSupportDir ...'.postln;
};"
	}

	*isInClassLib { |post = true|
		var res = this.filenameSymbol.asString.beginsWith(
			String.scDir +/+ "SCClassLibrary");
		if (post) {
			if (res.not) {
				"*** Standalone.sc is not in SCClassLibrary yet.\n"
				"// If you are in a Standalone already, you can move it there to use it:\n"
				"Standalone.moveToClassLib;\n"
				"thisProcess.recompile;".postln
			} {
				"*** Standalone.sc is already in SCClassLibrary.\n"
				.postln
			}
		};
		^res
	}

	*isInIntExt { |post = true|
		var res = this.filenameSymbol.asString.beginsWith("internalExtDir");
		if (post) {
			"*** Standalone.sc is %in .../Resources/InternalExtensions.\n"
			.postf(if (res, "", "_NOT_ "));
		};
		^res
	}

	// this happens when already moved to internalExtDir,
	// so better to move the files rather than copying them.
	*moveToClassLib {
		var currPath, newPath;
		var classFile, codeString;
		var schelpPath, newSchelpPath;
		var copyCmd = "cp";
		if (this.isInClassLib(false)) { ^false };
		// if already internalized, move it instead of copying,
		// to avoid discrepancy on next recompile
		if (this.isInIntExt(false)) { copyCmd = "mv" };

		currPath = this.filenameSymbol.asString;
		newPath = String.scDir +/+ "SCClassLibrary/" +/+ currPath.basename;

		if (currPath != newPath) {
			"*** moving Standalone.sc to internal class lib: ***".postln;
			unixCmd((copyCmd + "-f" + quote(currPath) + quote(newPath)).postcs);

			schelpPath = Standalone.filenameSymbol.asString.dirname.dirname
			+/+ "HelpSource/Standalone.schelp";
			if(File.exists(schelpPath)) {
				"*** moving Standalone.schelp to internal HelpSource: ***".postln;
				newSchelpPath = String.scDir +/+ "HelpSource/Standalone.schelp";
				unixCmd((copyCmd + "-f" + quote(schelpPath)
					+ quote(newSchelpPath)).postcs);
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

	*prStopAndProposeReboot {

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
