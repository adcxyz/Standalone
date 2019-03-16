+ OSXPlatform {
	startupFiles {
		^[String.scDir +/+ "startup.scd"]
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

