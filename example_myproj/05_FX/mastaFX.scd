
s.volume.volume = 0;

////// enable this if you have JITLibExtensions are installed:
/*
(
ProxyChain.add3(
	\revDrift, \filterIn -> { |in, revTime= 2.0|
		Splay.ar(
			CombL.ar(in.sum, 0.2,
				LFDNoise3.kr([0.1, 0.14, 0.17, 0.22, 0.28]).exprange(0.002, 0.2),
				revTime
			)
		);
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
m = MasterFX(s, 2, [\revDrift, \limit]);
m.add(\rev, 0.05);
m.add(\limit, 1);

m.gui;
);
*/