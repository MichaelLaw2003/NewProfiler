// NewProfiler runtime verification macro
setBatchMode(true);

// Build a small RGB test image with a bright band so the profile has signal.
newImage("np_test", "RGB Color", 64, 32, 1);
setColor(255, 0, 0);
fillRect(20, 0, 12, 32);   // red vertical band at x=20..31

out = getDirectory("current") + "np_out.txt";
makeLine(0, 16, 63, 16);

// Programmatic export: must write out with NO file dialog.
run("New Profiler", "save=" + out);

print("NP_EXPORT_PATH=" + out);
setBatchMode(false);
// -batch mode exits automatically when the macro finishes.
