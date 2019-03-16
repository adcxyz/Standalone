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

		Pbind('degree', Pseries([0, 2, 4], 1, 8), 'dur', 0.125).play;

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

		Pbind('degree', Pseries([0, 2, 4], 1, 8), 'dur', 0.125).play;

	};

}.play(AppClock);
