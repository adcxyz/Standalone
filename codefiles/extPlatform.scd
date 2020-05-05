+ OSXPlatform {
	startupFiles {
		^[Platform.resourceDir +/+ "startup.scd"]
	}
}

+ Platform {
	*hasIndependentSupportFolder {
		Platform.userAppSupportDir.basename != "SuperCollider";
	}

	*openStartupFiles {
		thisProcess.platform.startupFiles.do(_.openOS);
	}
}

