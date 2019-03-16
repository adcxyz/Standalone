//////// example for server configuration
"\n**********  loading serverConfig:  **********".postln;

Server.default = s = Server.local;

// tweak for fast response and rare complaints:
// default is 0.2 secs, which feels slow;
// less than 0.03 may introduce many late messages from server.
// latency nil means do things immediately, but will compromise timing:
// it quantizes to the hardwareBufferSize border.
s.latency = 0.05;
s.latency = nil;

// hardware setup first
s.options
// .device_("Fireface 400 (297)")	// device name
// .hardwareBufferSize_(128)		// take fastest power of two hardware allows
.numInputBusChannels_(8)		//
.numOutputBusChannels_(8)		//
.blockSize_(64)					//  .kr bus rate, default is 64 samples
;


"Setting server options: ".postln;
if (s.serverRunning) {
	q.warnWin(
		"Server options were set when s was running already!"
		" Settings will not take effect until server is rebooted:"
		"\ns.reboot;",
		"Server options!"
	);
};

// other general server setup:
s.options
// .numInputBusChannels_(q.numChans)
// .numOutputBusChannels_(q.numChans)
.numAudioBusChannels_(4096)		// for proxy usage: lots of ar bus space
.memSize_(8192 * 64)			// 512 MB memory for buffers, soundfiles!
.maxNodes_(1024 * 16)			// lots of synth nodes
.numWireBufs_(64*8)				// fat synthdefs need lots of interconnects
;

// set recording options:
s
.recChannels_(q.numChans)			// record stereo out only
.recSampleFormat_("float"); 			// in float to recover from clipping

q.postServerOptions = {
	"********** Server info : **********".postln;
	"s.name: % \n"
	"s.address: % \n"
	"s.latency: % \n"
	.postf(s.name, s.addr, s.latency);
	"server options:".postln;
	s.options.dump;
	"********************".postln;
	"serverConfig loaded.".postln;
	"********************".postln;
};


