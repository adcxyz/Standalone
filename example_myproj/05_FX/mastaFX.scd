
s.volume.volume = 0;

(
ProxyChain.add3(
	\rev, \filterIn -> { |in, revTime= 2.0|
		AdCVerb.ar(in * 0.2, revTime);
	}, 0.2,
	( revTime: [0.1, 100, \exp] )
);
ProxyChain.add3(
	\limit, \filter -> { |in, gain = 2, maxLevel = 0.8|
		Limiter.ar(in * gain, 0.8);
	}, 0.2,
	( gain: [1, 4, \exp],
		maxLevel: [0.25, 1, \exp]
	)
);

MasterFX(s).clear;
m = MasterFX(s, 2, [\rev, \limit]);
m.add(\rev, 0.05);
m.add(\limit, 1);

m.gui;
);
