
Standalone {

	classvar <>internalExtDirName = "InternalExtensions", <internalExtDir;
	classvar <lockupFilename = "StandaloneLocks.scd";

	classvar <newAppName, <newAppLocation, <pathToNewApp;
	classvar <newAppResDir, <newAppSupportDir;
	classvar <pathToNewApp, <pathToThisApp;

	*dir { ^Standalone.filenameSymbol.asString.dirname.dirname }
	*codefiledir { ^this.dir +/+ "codefiles/" }

	*initClass {
		internalExtDir = Platform.resourceDir +/+ internalExtDirName;
		if (this.locksOnStartup) { this.activate };
	}

	*export { |appName = "StehAllein", location = "~/Desktop/", internalAppName|

		// make some needed paths and folders
		newAppName = appName;
		newAppLocation = location.standardizePath;
		pathToNewApp = newAppLocation +/+ newAppName ++ ".app";
		newAppResDir = pathToNewApp +/+ "Contents/Resources";
		newAppSupportDir = Platform.userConfigDir.dirname +/+ newAppName;

		pathToThisApp = Platform.resourceDir.dirname.dirname;

		if (newAppName.size < 10) {
			"App name must be at least 10 chars long to have an independent userAppSupportDir."
			"\nplease provide a longer internal name for the app;"
			"You can rename the app itself later.".warn;
			^this
		};

		if (File.exists(pathToNewApp)) {
			"App at % already exists! please delete or move it elsewhere before exporting.".format(pathToNewApp).warn;
			^this
		};


		forkIfNeeded {
			var cond = Condition();

			var infoPListPath = pathToNewApp +/+ "Contents/Info.plist";
			var infoString, executableIndex, nameToReplace, newInfoString;
			var classLibDir, overWritesDir, newIntExtDir;


			// copying the current app with new name to new location:
			"% - copying new Standalone app to:".postf(thisMethod);
			unixCmd(("cp -R"
				+ quote(pathToThisApp.postcs)
				+ quote(pathToNewApp.postcs)), { cond.unhang }
			);
			cond.hang;

			if (newAppLocation.pathMatch.isEmpty) {
				"% - copying app failed! stopping export.".postln;
				thisProcess.halt;
			};

			File.mkdir(newAppSupportDir);

			0.2.wait;

			///////// FIXUPS in the new app:
			// a. change the Info.plist file by replacing the app name:
			// get its string, replace the SC names, write it out again

			"\n% - FIXUPS in the new app: \n".postf(thisMethod);
			if (infoPListPath.pathMatch.isEmpty) {
				"infoPlist file not found! stopping export.".warn;
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
			"renaming the macOS binary to: ".post;
			// fixups in the new app - 2. rename the binary inside the app folder
			unixCmdGetStdOut("mv -i"
				+ quote(pathToNewApp +/+ "Contents/MacOS/" +/+ nameToReplace)
				+ quote(pathToNewApp +/+ "Contents/MacOS/" ++ newAppName)
			);

			0.2.wait;

			// copy quarks dir:
			"\n% - copying all installed quarks: \n".postf(thisMethod);
			newIntExtDir = newAppResDir +/+ internalExtDirName;
			File.mkdir(newIntExtDir);

			LanguageConfig.includePaths.reject { |path| (path.contains("Standalone")) }.do { |path|
				var baseDirs = [ "SCClassLibrary", "HelpSource"];
				var baseDir = baseDirs.detect(path.contains(_)).postln;

				if (baseDir.notNil) {
					"moving default dir % out of the way...".postf(baseDir);
					unixCmdGetStdOut("mv"
						+ quote(newAppResDir +/+ baseDir)
						+ quote(newAppResDir +/+ baseDir ++ "_ORIG_").postln
					);

					"moving external dev dir % in place...".postf(baseDir);
					unixCmdGetStdOut("cp -R"
						+ quote(path.postln)
						+ quote(newAppResDir +/+ baseDir).postln
					);
				} {
					//
					unixCmdGetStdOut("cp -R"
						+ quote(path.postln)
						+ quote(newIntExtDir +/+ path.basename).postln
					);
				}
			};

			0.2.wait;

			// 4. move the Standalone quark into SCClassLibrary:
			"\n% - installing Standalone in new classlib: \n".postf(thisMethod);
			classLibDir = newAppResDir +/+ "SCClassLibrary/";
			unixCmd(("cp -R" + quote(Standalone.dir) + quote(classLibDir)));

			overWritesDir = classLibDir +/+ "SystemOverwrites/";
			File.mkdir(overWritesDir);

			"\n% - moving startup and selftest files in place. \n".postf(thisMethod);
			unixCmd(("cp" + quote(Standalone.codefiledir +/+ "standaloneSelfRepair.scd")
				+ quote(newAppResDir +/+  "standaloneSelfRepair.scd").postln));
			unixCmd(("cp" + quote(Standalone.codefiledir +/+ "startup.scd")
				+ quote(newAppResDir +/+  "startup.scd").postln));
			unixCmd(("cp" + quote(Standalone.codefiledir +/+ "extPlatform.scd")
				+ quote(overWritesDir +/+  "extPlatform.sc").postln));

			"copying example project next to app...".postln;
			unixCmd(("cp -R" + quote(Standalone.dir +/+ "example_myproj")
				+ quote(newAppLocation +/+  newAppName ++ "_proj").postln));

			// not always working yet ...
			this.filterIDEConfigFile;

			this.writeSClangConfigFile(newIntExtDir);


			"*** Standalone is ready to go! Please quit this app, and open your standalone...".postln;

			// "\n% - countdown for wakeup kiss: \n".postf(thisMethod);
			/// (3..0).do { |i| if (i.postln > 0) { 1.wait } };
			// wakeup kiss:
			// unixCmd("open" + pathToNewApp);

		}
	}

	/// FIXME - filterIDEConfigFile not working reliably yet
	/// because sometimes the new app clobbers the copied file on startup.

	*filterIDEConfigFile {
		var my_ideConfStr = File.use(Platform.userAppSupportDir +/+ "sc_ide_conf.yaml", "r", (_.readAllString));
		var ideLines = my_ideConfStr.split($\n);
		var configLineIndex = ideLines.detectIndex(_.contains("    configFile:"));
		var standaloneIndex = ideLines.detectIndex(_.contains("    standalone:"));
		var new_ideConfStr;

		if (configLineIndex.notNil) {
			ideLines.put( configLineIndex, "configFile: \"\"");
		};
		if (standaloneIndex.notNil) {
			ideLines.put( standaloneIndex, "standalone: false");
		};

		new_ideConfStr = ideLines.join($\n).postcs;

		File.use(newAppSupportDir +/+ "sc_ide_conf.yaml", "w", (_.write(new_ideConfStr)));
		File.use(newAppResDir +/+ "sc_ide_conf.yaml", "w", (_.write(new_ideConfStr)));
	}

	*writeSClangConfigFile { |newIntExtDir|
		var new_sclangConfStr =
		"includePaths:\n"
		"    -   %\n"
		"excludePaths:\n"
		"    []\n"
		"postInlineWarnings: %\n"
		.format(newIntExtDir.standardizePath, LanguageConfig.postInlineWarnings).postcs;

		File.use(newAppSupportDir +/+ "sclang_conf.yaml", "w", (_.write(new_sclangConfStr)));
		File.use(newAppResDir +/+ "sclang_conf.yaml", "w", (_.write(new_sclangConfStr)));
	}

	*appPath { ^Platform.resourceDir.dirname.dirname }
	*appDir { ^this.appPath.dirname }
	*appName { ^this.appPath.basename.splitext.first }

	*openStartup {
		unixCmd("open" + quote(thisProcess.platform.startupFiles[0]))
	}

	*isInClassLib { |post = true|
		var res = this.filenameSymbol.asString.beginsWith(
			Platform.resourceDir +/+ "SCClassLibrary");
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
		newPath = Platform.resourceDir +/+ "SCClassLibrary/" +/+ currPath.basename;

		if (currPath != newPath) {
			"*** moving Standalone.sc to internal class lib: ***".postln;
			unixCmd((copyCmd + "-f" + quote(currPath) + quote(newPath)).postcs);

			schelpPath = Standalone.filenameSymbol.asString.dirname.dirname
			+/+ "HelpSource/Standalone.schelp";
			if(File.exists(schelpPath)) {
				"*** moving Standalone.schelp to internal HelpSource: ***".postln;
				newSchelpPath = Platform.resourceDir +/+ "HelpSource/Standalone.schelp";
				unixCmd((copyCmd + "-f" + quote(schelpPath)
					+ quote(newSchelpPath)).postcs);
			}
		};
		^true
	}

	*lockupPath { ^( Platform.resourceDir +/+ lockupFilename) }

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
			this.prStopAndProposeReboot;
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
