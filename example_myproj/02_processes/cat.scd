// SOUND 2 // the DubCat example from our Class:
(
// controlspecs
// // if JITLibExtensions are used, you can make space local:
// Ndef(\cat).addSpec(\pFreq, [20, 400, \exp]);
// Ndef(\cat).addSpec(\pWidth, [0.01, 0.5, \exp]);
// Ndef(\cat).addSpec(\filt, [1, 32, \exp]);
// Ndef(\cat).addSpec(\q, [1, 1000, \exp]);
// Ndef(\cat).addSpec(\modFreq, [0, 200, 7]);
// Ndef(\cat).addSpec(\moDepth, [0, 12*8]);

//// if no JITLibExtensions, specs are global
Spec.add(\pFreq, [20, 400, \exp]);
Spec.add(\pWidth, [0.01, 0.5, \exp]);
Spec.add(\filt, [1, 32, \exp]);
Spec.add(\reso, [1, 1000, \exp]);
Spec.add(\modFreq, [0, 200, 7]);
Spec.add(\moDepth, [0, 12*8]);


// a Synth with 6 control parameters
(
Ndef(\cat, { |amp=0, pFreq=100, modFreq=0, filt=2, reso=1, pWidth=0.5,  moDepth=12|
	var signal;
	pFreq = pFreq * SinOsc.ar(modFreq * 0.5 + [0, 0.1], 0).product.madd(moDepth).midiratio;
	signal = RLPF.ar(
		LFPulse.ar(
			pFreq * [1, 1.01],
			0,
			pWidth
		),
		(pFreq * filt).clip(50, 15000),
		reso.reciprocal
	)
	* AmpComp.kr(pFreq.lag(1));
	Limiter.ar(signal) * amp;
})
);

Ndef(\cat).set(\amp, 0.2).vol_(0.2);	// don't play it yet, just prepare
Ndef(\cat).gui.moveTo(400, 650);

//NdefPreset( Ndef(\cat) ).loadSettings;
);
