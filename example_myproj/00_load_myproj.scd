// Standalone project example 'myproj'

// this loadfile loads everything else

Task {
	// everything before server boots:
	q.load("01_setup/0_loadGlobalUtils.scd");
	q.load("01_setup/1_serverConfig.scd");
	q.load("01_setup/2_networkConfig.scd");

	0.5.wait;

	ServerBoot.add({
		"// load my soundfiles into buffers when server boots:".postln;
		q.buffers = "soundfiles/*.aif".pathMatch.do { |path|
			path.basename.postcs;
			Buffer.read(s, path);
		};
		s.sync;
		"*** q.buffers loaded % files.\n".postf(q.buffers.size);
	});


	s.waitForBoot {
		0.5.wait;
		// load all patches that need a running server;
		q.load("02_processes/*.scd");

		// load all controllers that need sound processes ready
		0.5.wait;
		q.load("03_interfaces/*.scd");

		// load masterFX and similar
		0.5.wait;
		q.load("05_FX/*.scd");

		0.5.wait;
		// this one reboots the server whenever it dies
		q.load("LazarusServer.scd");

		0.5.wait;

		s.volume.volume = 0;


	};

}.play(AppClock);

s.w {
	Task {

		1.wait;
		s.sync;

		q.load("2_mutation/ToyDataMutation.scd");

		0.5.wait;

		///////////////// dont run the tasks themselves
		// q.load("/1_Arabidopsis/Arabidopsis.scd");
		// 0.5.wait;
		// q.load("3_Weizen/Winterweizen.scd");
		// 0.5.wait;
		// q.load("4_*/Patatareife.scd");

		q.load("browsergui_interface.scd");

		// test for installation on old MacPro:
		1.wait;

		// q.load("PlantiSonicsApp/gui_sketches/Arabidopsis_lines.scd", "", "", 1);
		q.load("9_GUI/gui_masta.scd");
		// Tdef('arabiGenom').play;

		"*** PlantiSonicsApp startup done! ***".postln;
	}.play(AppClock);
};

