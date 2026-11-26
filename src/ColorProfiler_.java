import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.gui.*;
import java.awt.*;
import java.awt.event.*;

/*     Implements the "ColorProfier" command.It provides the same functionality 
 *     as the Plot Profile but for RGB images. It provides superior functionality to the 
       I would like to acknowledge C. Laummonerie & J. Mutterer (authors of RGBProfiler)
 *     for the code for mouse event handling
 *     It is based largely on the Plot class
 *    
 *     @version 1.2  12 June 2005
 *              1.0;  02 Oct 2004
 *
 *     @author	Dimiter Prodanov
 *     @author  University of Leiden
 
 
 *
 *      This library is free software; you can redistribute it and/or
 *      modify it under the terms of the GNU Lesser General Public
 *      License as published by the Free Software Foundation; either
 *      version 2.1 of the License, or (at your option) any later version.
 *
 *      This library is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *       Lesser General Public License for more details.
 *
 *      You should have received a copy of the GNU Lesser General Public
 *      License along with this library; if not, write to the Free Software
 *      Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */


public class ColorProfiler_ implements PlugInFilter, MouseListener, MouseMotionListener  {

	ImagePlus imp;
	static boolean verticalProfile=true;
	ProfileColorPlot pcp;

	/* ---- NewProfiler addition: optional non-interactive export ----------
	 * Macro usage:
	 *   run("New Profiler", "save=/absolute/path/4_1.txt");
	 * When "save=" is present the profile is written straight to that path
	 * after it is computed, with NO "Save as Text..." file dialog. Everything
	 * else (plot window, Results table) behaves exactly as before. Combine
	 * with setBatchMode(true) to suppress all windows during a batch.
	 */
	private static String savePath = null;
	public static String getSavePath() {return savePath;}

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("set"))
			{doOptions(); return DONE;}
                if (arg.equals("about")){
                        showAbout();
                        return DONE;
                        }
		// Parse the NewProfiler macro option. Reset first so a stale value
		// from a previous run can never be reused.
		savePath = null;
		if (arg!=null && arg.startsWith("save=")) {
			savePath = arg.substring(5);
			if (savePath.startsWith("[") && savePath.endsWith("]"))
				savePath = savePath.substring(1, savePath.length()-1);
		}
		this.imp = imp;
                // NewProfiler: in batch macros the image may have no window,
                // so the interactive mouse listeners are optional only.
                ImageWindow win = imp.getWindow();
                if (win!=null) {
                    ImageCanvas canvas = win.getCanvas();
                    if (canvas!=null) {
                        canvas.addMouseListener(this);
                        canvas.addMouseMotionListener(this);
                    }
                }
		return DOES_RGB+NO_UNDO+NO_CHANGES+ROI_REQUIRED;
	}

	public void run(ImageProcessor ip) {
		boolean averageHorizontally = verticalProfile || IJ.altKeyDown();
		pcp=new ProfileColorPlot (imp, averageHorizontally);
	        pcp.calculateProfile();
		pcp.drawProfile();
		pcp.PrintResults();
		// NewProfiler: if the macro supplied save=<path>, write the same text
		// the "Save..." button produces, straight to disk (no dialog).
		if (savePath!=null) {
			pcp.saveProfileAsText(savePath);
			savePath = null;  // one-shot: never reuse for a later ROI
		}
	}

	public void doOptions() {
		double ymin = ProfilePlot.getFixedMin();
		double ymax = ProfilePlot.getFixedMax();
		boolean fixedScale = ymin!=0.0 || ymax!=0.0;
		boolean wasFixedScale = fixedScale;
		
		GenericDialog gd = new GenericDialog("Profile Plot Options", IJ.getInstance());
		gd.addNumericField("Width (pixels):", PlotWindow.plotWidth, 0);
		gd.addNumericField("Height (pixels):", PlotWindow.plotHeight, 0);
		gd.addNumericField("Minimum Y:", ymin, 2);
		gd.addNumericField("Maximum Y:", ymax, 2);
		gd.addCheckbox("Fixed Y-axis Scale", fixedScale);
		gd.addCheckbox("Do Not Save X-Values", !PlotWindow.saveXValues);
		gd.addCheckbox("Auto-close", PlotWindow.autoClose);
		gd.addCheckbox("Vertical Profile", verticalProfile);
		gd.addCheckbox("List Values", PlotWindow.listValues);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
		int w = (int)gd.getNextNumber();
		int h = (int)gd.getNextNumber();
		if (w<300) w = 300;
		if (w>screen.width-140) w = screen.width-140;
		if (h<100) h = 100;
		if (h>screen.height-300) h = screen.height-300;
		PlotWindow.plotWidth = w;
		PlotWindow.plotHeight = h;
		ymin = gd.getNextNumber();
		ymax = gd.getNextNumber();
		fixedScale = gd.getNextBoolean();
		MultyPlotWindow.saveXValues = !gd.getNextBoolean();
		MultyPlotWindow.autoClose = gd.getNextBoolean();
		verticalProfile = gd.getNextBoolean();
		MultyPlotWindow.listValues = gd.getNextBoolean();
//		if (!fixedScale && !wasFixedScale && (ymin!=0.0 || ymax!=0.0))
//			fixedScale = true;
//		if (!fixedScale) {
//			ymin = 0.0;
//			ymax = 0.0;
//		} else if (ymin>ymax) {
//			double tmp = ymin;
//			ymin = ymax;
//			ymax = tmp;
//		}
		ProfilePlot.setMinAndMax(0, 255);
		IJ.register(ColorProfiler_.class);
	}
            /*------------------------------------------------------------------*/
    void showAbout() {
        IJ.showMessage("About New Profiler...",
        "Fork of Color Profiler by Dimiter Prodanov (Univ. Leiden).\n"
        +"Displays RGB profiles of line/rect selections.\n"
        +"NewProfiler fixes: macro save=<path> export (no dialog);\n"
        +"x-axis rescales to ROI length.\n"
        +"Licensed under LGPL v2.1-or-later."
        );
    }    /* showAbout */
		
    public void mouseReleased(MouseEvent e) {
	    pcp.calculateProfile();
            pcp.drawProfile();
            pcp.PrintResults();
          //  IJ.log("mouse released");
    }
    public void mousePressed(MouseEvent e) {
       //  IJ.log("mouse pressed");
    }
    public void mouseDragged(MouseEvent e) {
      //      IJ.log("mouse dragged");
    }

    public void mouseExited(MouseEvent e) {
      //   IJ.log("mouse exited");
    }
    public void mouseClicked(MouseEvent e) {
     //   IJ.log("mouse clicked");
    }   
    public void mouseEntered(MouseEvent e) {
     //   IJ.log("mouse entered");
    }
    public void mouseMoved(MouseEvent e) {
   //   IJ.log("mouse moved");
    }

}

