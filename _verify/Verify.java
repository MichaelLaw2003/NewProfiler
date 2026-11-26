import ij.ImagePlus;
import ij.gui.Line;
import ij.process.ColorProcessor;
import java.awt.Color;
import java.io.File;

// Direct-API verification of NewProfiler, bypassing the ImageJ macro engine
// (which cannot init menus in headless mode). Same code path the macro calls.
public class Verify {
    public static void main(String[] args) throws Exception {
        String out = args[0];

        // Build an RGB image with a red vertical band at x=20..31.
        ColorProcessor cp = new ColorProcessor(64, 32);
        cp.setColor(new Color(255, 0, 0));
        cp.fillRect(20, 0, 12, 32);
        ImagePlus imp = new ImagePlus("np_test", cp);
        // ROI starts at x=20 (non-zero offset) -> old code pinned x-axis at 20.
        imp.setRoi(new Line(20, 16, 63, 16));

        NP_ColorProfiler plugin = new NP_ColorProfiler();
        int flags = plugin.setup("save=" + out, imp);
        System.out.println("SETUP_FLAGS=" + flags);
        System.out.println("SAVE_PATH=" + NP_ColorProfiler.getSavePath());

        plugin.run(imp.getProcessor());

        // Inspect the plot's x-values via package-visible fields.
        float[] xv = plugin.pcp.plot.getXValues();
        System.out.println("X_COUNT=" + xv.length);
        System.out.println("X_FIRST=" + xv[0]);
        System.out.println("X_LAST=" + xv[xv.length - 1]);

        File f = new File(out);
        System.out.println("FILE_EXISTS=" + f.exists() + " SIZE=" + f.length());
        System.out.println("VERIFY_DONE");
        System.exit(0);
    }
}
