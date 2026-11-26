import java.awt.*;
import ij.*;
import ij.process.*;
import ij.util.*;
import ij.measure.*;
import ij.gui.*;

/*     Profile color plot. It provides the same functionality as the ProfilePlot
 *     but for RGB images. It provides superior functionality to the RGBProfiler
 *     It is based largely on the Plot class
 *    
 *     @version 1.2;  06 Feb 2005
 *              1.1;  26 Nov 2004
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


public class NP_ProfileColorPlot {
    
    static final int MIN_WIDTH = 350;
    static final double ASPECT_RATIO = 0.5;
    static final int WIN_WIDTH = 300;
    static final int WIN_HEIGHT = 410;
    static final int HIST_WIDTH = 256;
    static final int HIST_HEIGHT = 80;
    static final int BAR_HEIGHT = 12;
    static final int XMARGIN = 20;
    static final int YMARGIN = 10;
    
    private double min, max;
    private boolean minAndMaxCalculated;
    private static double fixedMin = Prefs.getDouble("pp.min",0.0);
    private static double fixedMax = Prefs.getDouble("pp.max",0.0);
  // private  double[][] pdf=new double[256][3];
    //private int[] mask;
    private int area=1;
    private int width, height=1;
    
    protected ImagePlus imp;
    protected double[][] profile;
    protected double magnification;
    protected double pixelSize;
    protected String units;
    protected String yLabel;
    protected ColorProcessor ip;
    protected  NP_MultyPlot plot;
    
    protected int[] histMin=new int[3];
    protected int[] histMax=new int[3];
    protected double[] histMean=new double[3];

    protected double[] histStdev=new double[3];
    
       public  double gr=-1.0f; // g/r
       public  double br=-1.0f;// b/r
       public  double rg=-1.0f;// r/g
       public  double rb=-1.0f;// r/b
       public  double gb=-1.0f; // b/b
       public  double bg=-1.0f; // r/b
      
       private boolean averageHorizontally=true;
       
   // public NP_ProfileColorPlot() {
    //}
    
    public NP_ProfileColorPlot(ImagePlus imp) {
        this(imp, false);
        
    }
    
    public NP_ProfileColorPlot(ImagePlus imp, boolean averageHorizontally) {
        this.imp = imp;
        this.averageHorizontally=averageHorizontally;
        Roi roi = imp.getRoi();
        if (roi==null) {
            IJ.error("Selection required.");
            return;
        }
       // int roiType = roi.getType();
       // if (!(roiType==Roi.LINE || roiType==Roi.POLYLINE || roiType==Roi.FREELINE || roiType==Roi.RECTANGLE)) {
         //   IJ.error("Line or rectangular selection required.");
      //      return;
       // }
       if (! calculateProfile(roi,  averageHorizontally)){
           IJ.error("Line or rectangular selection required.");
           return;
        }
     
        Calibration cal = imp.getCalibration();
        pixelSize = cal.pixelWidth;
        units = cal.getUnits();
        yLabel = cal.getValueUnit();
        ip = (ColorProcessor) imp.getProcessor();
        ip.setCalibrationTable(cal.getCTable());
        /*
        if (roiType==Roi.LINE) {
            ip.setInterpolate(true);
            //profile = ((Line)roi).getPixels();
            
            profile= getLineSegment(ip,  ((Line)roi). x1, ((Line)roi).y1, ((Line)roi).x2, ((Line)roi).y2) ;
        } else if (roiType==Roi.POLYLINE || roiType==Roi.FREELINE)
            profile = getIrregularProfile(roi, ip);
        else if (averageHorizontally)
            profile = getColumnAverageProfile (roi.getBoundingRect(), ip);
        else
            profile = getRowAverageProfile(roi.getBoundingRect(), ip);
         */
        ip.setCalibrationTable(null);
        ImageWindow win = imp.getWindow();
        if (win!=null)
            magnification = win.getCanvas().getMagnification();
        else
            magnification = 1.0;
        
        //initPlot(profile.length);
         //String xLabel = "Distance ("+units+")";
          //int offset=roi.getBoundingRect().x;
          //plot = new NP_MultyPlot("Plot of "+imp.getShortTitle(), xLabel, yLabel, offset, offset+profile.length-1,  0, 255);
    }
    
    
    /** Returns the size of the plot that createWindow() creates. */
    public Dimension getPlotSize() {
        if (profile==null) return null;
        int width = (int)(profile.length*magnification);
        int height = (int)(width*ASPECT_RATIO);
        if (width<MIN_WIDTH) {
            width = MIN_WIDTH;
            height = (int)(width*ASPECT_RATIO);
        }
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int maxWidth = Math.min(screen.width-200, 1000);
        if (width>maxWidth) {
            width = maxWidth;
            height = (int)(width*ASPECT_RATIO);
        }
        return new Dimension(width, height);
    }
    

    
    /** Displays this profile plot in a window. */
    public void drawProfile() {
       if (wnd!=null) wnd.close();
        if (profile==null)
            return;
        Roi roi=imp.getRoi();
        //calculateProfile(roi,  averageHorizontally);
        calculateStatistics(profile);
        int n = profile.length;
       // float[] dummy=new float[n];
        //dummy[0]=255;
    
        Dimension d = getPlotSize();
        String xLabel = "Distance ("+units+")";
        //int n = profile.length;
      
        // ---- NewProfiler fix (x-axis) ------------------------------------
        // Original code used the ROI bounding box absolute x as `offset` and
        // pinned the axis to [offset, offset+n-1]. That made the x-range depend
        // on WHERE the line was drawn rather than on its length, so resizing a
        // ROI did not rescale the axis.  Use the sample index (i) as the x
        // coordinate so the axis always spans the full ROI length and rescales
        // automatically. Calibrated images still get physical units via
        // pixelSize; uncalibrated images use plain index 0..n-1.
        float[] xValues = new float[n];
        for (int i=0; i<n; i++)
            xValues[i] = (float)(i*pixelSize);
        // ------------------------------------------------------------------
       
        float[] rValues = new float[n];
        float[] gValues = new float[n];
        float[] bValues = new float[n];
        for (int i=0; i<n; i++){
            rValues[i] = (float)profile[i][0]; //red
            gValues[i] = (float)profile[i][1]; //green
            bValues[i] = (float)profile[i][2]; //blue
        }
        
      
        
       // boolean fixedYScale = fixedMin!=0.0 || fixedMax!=0.0;
       
        // ---- NewProfiler fix (x-axis) ------------------------------------
        // X range now tracks the ROI length (sample index), not its absolute
        // pixel location. getMinMax of the NEW xValues gives the true extent.
        float xLo = xValues[0];
        float xHi = xValues[n-1];
        plot = new NP_MultyPlot("Plot of "+imp.getShortTitle(), xLabel, yLabel, xLo, xHi,  0, 255);
        plot.setColor(new Color(0xff, 0, 0)); // red
        plot.addPoints(xValues,rValues,2);
        
        plot.setColor(new Color(0, 0xff, 0)); // green
        plot.addPoints(xValues,gValues,2);
        
        plot.setColor(new Color(0, 0, 0xff)); //blue
        plot.addPoints(xValues,bValues,2);
        
            plot.setLimits(xLo, xHi, 0, 255);
        // ------------------------------------------------------------------
       //wnd=plot.show();
        //plot.repaint();
        int[]precision={0,0};
        wnd=new NP_MultyPlotWindow(plot, headings,precision) ;
        wnd.draw();
     //   wnd.drawPlot(plot);
    }
    
    private NP_MultyPlotWindow wnd;
    private static final String[] headings={"L","R","G","B"};

    /** ---- NewProfiler addition ------------------------------------------
     * Write the RGB profile values to `path` with NO file dialog.
     * Delegates to NP_MultyPlotWindow's toString(EOL), so the bytes are exactly
     * what the manual "Save..." button would produce (tab-separated, leading
     * empty x-field, one R/G/B row per sample). Called by NP_ColorProfiler when
     * the macro passes save=<path>. */
    public void saveProfileAsText(String path) {
        if (wnd==null) {
            IJ.error("NewProfiler: no profile window to export from.");
            return;
        }
        wnd.saveValuesTo(path, true);  // force legacy R/G/B layout for scripts
    }
    
    public boolean  calculateProfile(Roi roi, boolean averageHorizontally){
         int roiType = roi.getType();
        if (!(roiType==Roi.LINE || roiType==Roi.POLYLINE || roiType==Roi.FREELINE || roiType==Roi.RECTANGLE)) {
          //  IJ.error("Line or rectangular selection required.");
            return false;
        }
        ip = (ColorProcessor) imp.getProcessor();
        
        if (roiType==Roi.LINE) {
            ip.setInterpolate(true);
                 
            profile= getLineSegment(ip,  ((Line)roi). x1, ((Line)roi).y1, ((Line)roi).x2, ((Line)roi).y2) ;
        } else if (roiType==Roi.POLYLINE || roiType==Roi.FREELINE)
            profile = getIrregularProfile(roi, ip);
        else if (averageHorizontally)
            profile = getColumnAverageProfile (roi.getBoundingRect(), ip);
        else
            profile = getRowAverageProfile(roi.getBoundingRect(), ip);
        return true;
    }
    
    public void calculateProfile () {
       Roi roi=imp.getRoi();
        calculateProfile(roi,  averageHorizontally);
    }
    public void dispose() {
        //plot.getImagePlus().
    }
    protected Rectangle frame;
    
 public void PrintResults(){
        IJ.setColumnHeadings("channel\tmean\tstd.dev.\tP5%\tP95%\tred\tgreen\tblue");
        IJ.write("red\t"+ IJ.d2s(histMean[0],2) + "\t " + IJ.d2s(histStdev[0],2)+"\t"+
        IJ.d2s(p5[0],2)+"\t"+ IJ.d2s(p95[0],2) +"\t"+"na"+"\t"+ IJ.d2s(rg,2)+"\t"+ IJ.d2s(rb,2) );
   
         IJ.write("green\t" + IJ.d2s(histMean[1],2)+"\t" + IJ.d2s(histStdev[1],2)+"\t"+ 
         IJ.d2s(p5[1],2)+"\t"+ IJ.d2s(p95[1],2)+"\t"+ IJ.d2s(gr,2)+"\t"+"na"+"\t"+ IJ.d2s(gb,2) );
         
         IJ.write("blue\t" + IJ.d2s(histMean[2],2)+"\t" + IJ.d2s(histStdev[2],2)+"\t"+
         IJ.d2s(p5[2],2)+"\t"+ IJ.d2s(p95[2],2)+"\t"+ IJ.d2s(br,2)+"\t"+ IJ.d2s(bg,2)+"\t"+"na");
 }
    
    
    private int getWidth(double d, ImageProcessor ip) {
        return ip.getStringWidth(IJ.d2s(d,0));
    }
    
   
    
    /** Returns the profile plot data. */
    public double[][] getProfile() {
        return profile;
    }
    
    /** Returns the calculated minimum value. */
    public double getMin() {
        if (!minAndMaxCalculated)
            findMinAndMax();
        return min;
    }
    
    /** Returns the calculated maximum value. */
    public double getMax() {
        if (!minAndMaxCalculated)
            findMinAndMax();
        return max;
    }
    
        /** Returns histogram mean. */
    public double[] getHistMean() {
      return this.histMean;
    }
    
            /** Returns histogram standard deviation. */
    public double[] getHistStdev() {
      return this.histStdev;
    }
    
    /** Sets the y-axis min and max. Specify (0,0) to autoscale. */
    public static void setMinAndMax(double min, double max) {
        fixedMin = min;
        fixedMax = max;
        IJ.register(ProfilePlot.class);
    }
    
    /** Returns the profile plot y-axis min. Auto-scaling is used if min=max=0. */
    public static double getFixedMin() {
        return fixedMin;
    }
    
    /** Returns the profile plot y-axis max. Auto-scaling is used if min=max=0. */
    public static double getFixedMax() {
        return fixedMax;
    }
    
    //double[][] getRowAverageProfile(Rectangle rect, Calibration cal, ColorProcessor ip) {
    double[][] getRowAverageProfile(Rectangle rect, ColorProcessor ip) {   
   // double[][] getColumnAverageProfile(Rectangle rect, ColorProcessor ip) {
        double[][] profile = new double[rect.height][3];
        double[][] aLine;
        
        ip.setInterpolate(false);
        //double x1=rect.x;
        //double x2=rect.x+rect.width-1;
       // double x2=rect.x;
        double y1=rect.y;
        double y2=rect.y+ rect.height;
        for (double x=rect.x; x<rect.x+rect.width; x++) {
            //aLine = ip.getLine(x, rect.y, x, rect.y+rect.height-1);
            aLine =getLineSegment(ip,x, y1, x, y2);
            for (int i=0; i<rect.height; i++) {
                profile[i][0] += aLine[i][0];
                profile[i][1] += aLine[i][1];
                profile[i][2] += aLine[i][2];
            }
        }
      //  IJ.log("ra pl: "+profile.length);
        for (int i=0; i<profile.length; i++) {
           profile[i][0] /= rect.width;
           profile[i][1] /= rect.width;
           profile[i][2] /= rect.width;
       }
   //     if (cal!=null)
       //     pixelSize = cal.pixelHeight;
        return profile;
    }
    
   double[][] getColumnAverageProfile(Rectangle rect, ColorProcessor ip) {
  // double[][] getRowAverageProfile(Rectangle rect, ColorProcessor ip) {   
        double[][] profile = new double[rect.width][3];
        double[][] aLine;
        
        ip.setInterpolate(false);
        double x1=rect.x;
        //double x2=rect.x+rect.width-1;
        double x2=rect.x+rect.width;
        //double y1=rect.y;
       // double y2=rect.y;
        for (double y=rect.y; y<rect.y+rect.height; y++) {
            //aLine = ip.getLine(rect.x, y, rect.x+rect.width-1, y);
            aLine =getLineSegment(ip,x1, y, x2, y);
            for (int i=0; i<rect.width; i++) {
                profile[i][0] += aLine[i][0];
                profile[i][1] += aLine[i][1];
                profile[i][2] += aLine[i][2];
            }
           // y1++;
           // y2++;
        }
       // IJ.log("ca pl: "+profile.length);
        for (int i=0; i<profile.length; i++){
            profile[i][0] /= rect.height;
            profile[i][1] /= rect.height;
            profile[i][2] /= rect.height;
        }
        return profile;
    }
    
    double[][] getIrregularProfile(Roi roi, ColorProcessor ip) {
        int n = ((PolygonRoi)roi).getNCoordinates();
        int[] x = ((PolygonRoi)roi).getXCoordinates();
        int[] y = ((PolygonRoi)roi).getYCoordinates();
        Rectangle r = roi.getBoundingRect();
        int xbase = r.x;
        int ybase = r.y;
        double length = 0.0;
        double segmentLength;
        int xdelta, ydelta, iLength;
        double[] segmentLengths = new double[n];
        int[] dx = new int[n];
        int[] dy = new int[n];
        for (int i=0; i<(n-1); i++) {
            xdelta = x[i+1] - x[i];
            ydelta = y[i+1] - y[i];
            segmentLength = Math.sqrt(xdelta*xdelta+ydelta*ydelta);
            length += segmentLength;
            segmentLengths[i] = segmentLength;
            dx[i] = xdelta;
            dy[i] = ydelta;
        }
        double[][] values = new double[(int)length][3];
        double leftOver = 1.0;
        double distance = 0.0;
        int index;
        //double oldx=xbase, oldy=ybase;
        for (int i=0; i<n; i++) {
            double len = segmentLengths[i];
            if (len==0.0)
                continue;
            double xinc = dx[i]/len;
            double yinc = dy[i]/len;
            double start = 1.0-leftOver;
            double rx = xbase+x[i]+start*xinc;
            double ry = ybase+y[i]+start*yinc;
            double len2 = len - start;
            int n2 = (int)len2;
            int[] rgb=new int[3];
            //double d=0;;
            //IJ.write("new segment: "+IJ.d2s(xinc)+" "+IJ.d2s(yinc)+" "+IJ.d2s(len)+" "+IJ.d2s(len2)+" "+IJ.d2s(n2)+" "+IJ.d2s(leftOver));
            for (int j=0; j<=n2; j++) {
                index = (int)distance+j;
                if (index<values.length)
                    //values[index] = ip.getInterpolatedValue(rx, ry);
                    rgb=ip.getPixel((int)rx, (int)ry, rgb);
                values[i][0]=(double)rgb[0];
                values[i][1]=(double)rgb[1];
                values[i][2]=(double)rgb[2];
                //d = Math.sqrt((rx-oldx)*(rx-oldx)+(ry-oldy)*(ry-oldy));
                //IJ.write(IJ.d2s(rx)+"    "+IJ.d2s(ry)+"    "+IJ.d2s(d));
                //IJ.log(IJ.d2s(rx)+"    "+IJ.d2s(ry));
                //oldx = rx; oldy = ry;
                rx += xinc;
                ry += yinc;
            }
            distance += len;
            leftOver = len2 - n2;
        }
        
        return values;
        
    }
    
    private double[][] getLineSegment(ColorProcessor ip, double x1, double y1, double x2, double y2) {
        double dx = x2-x1;
        double dy = y2-y1;
        int n = (int)Math.round(Math.sqrt(dx*dx + dy*dy));
        double xinc = dx/n;
        double yinc = dy/n;
        n++;
        double[][] data = new double[n][3];
        double rx = x1;
        double ry = y1;
        int[] rgb=new int[3];
        for (int i=0; i<n; i++) {
            //data[i] = ip.getInterpolatedValue(rx, ry);
            rgb=ip.getPixel((int)rx, (int)ry, rgb);
            data[i][0]=(double)rgb[0];
            data[i][1]=(double)rgb[1];
            data[i][2]=(double)rgb[2];
            rx += xinc;
            ry += yinc;
        }
        return data;
    }
    
    
    void findMinAndMax() {
        if (profile==null) return;
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        double vmin,vmax;
        for (int i=0; i<profile.length; i++) {
            vmin =Math.min(profile[i][0],Math.min(profile[i][1],profile[i][2]));
            vmax =Math.max(profile[i][0],Math.max(profile[i][1],profile[i][2]));
            if (vmin<min) min=vmin;
            if (vmax>max) max=vmax;
        }
        this.min = min;
        this.max = max;
    }
    
    /** Value of pixels included in masks. */
    public static final int BLACK = 0xFF000000;
    
    
    public static int[] getMinMax(int[] a) {
        int min = a[0];
        int max =a[0];
        int value;
        for (int i=0; i<a.length; i++) {
            value = a[i];
            if (value<min)
                min = value;
            if (value>max)
                max = value;
        }
        int[] minAndMax = new int[2];
        minAndMax[0] = min;
        minAndMax[1] = max;
        return minAndMax;
    }
    
  private double[][] pdf=new double[3][256];
  protected double[] p5=new double[3];
  protected double[] p95=new double[3];
   
  public double[] getP5(){
      return p5;
  }
  
  public double[] getP95(){
      return p95;
  }
   public void calculateStatistics() {
         calculateStatistics(this.profile);   
   }
   
    protected void calculateStatistics(double[][]profile) {
        int N=profile.length;
		//IJ.log("N: "+N);
     double[][] hist=new double[3][256];
        for (int col=0;col<3;col++) {
            
            double cumsum=0;
            double cumsum2=0;
            for (int i=0; i<N;i++){
               
                cumsum+=profile[i][col];
                cumsum2+=profile[i][col]*profile[i][col];
                int bin=(int)profile[i][col];
                //hist[col][bin]=hist[col][bin]+1.0/N;
                hist[col][bin]++;
               // IJ.log("bin "+bin+" pr "+pdf[col][bin]);
            }
            //IJ.log("cumsum: "+cumsum);
            // IJ.log("cumsum2: "+cumsum2);
            histMean[col]=cumsum/N;
            double stdDev = (N*cumsum2-cumsum*cumsum)/N;
            histStdev[col] = Math.sqrt(stdDev/(N-1.0));
            cumsum=0;
            for (int i=0;i<256;i++) {
                cumsum+=hist[col][i]/(float)N;
                pdf[col][i]+=cumsum;
                 if (pdf[col][i]<=0.05) p5[col]=i;
                if (pdf[col][i]<=0.95) p95[col]=i;
            }
            }
 //     IJ.log("r "+p5[0]+" g "+p5[1]+" b "+p5[2]);  
 //       IJ.log("r "+p95[0]+" g "+p95[1]+" b "+p95[2]);  
           // for (int i=0;i<256;i++) {
          
          //IJ.log("r "+pdf[0][i]+" g "+pdf[1][i]+" b "+pdf[2][i]);  
            //}
  
     if (p95[0]>0)  {
          gr=histMean[1]/p95[0]; // g/r
          br=histMean[2]/p95[0]; // b/r
     }
      if (p95[1]>0)  {
          rg=histMean[0]/p95[1]; // r/g
           bg=histMean[2]/p95[1]; // r/b
      }
     if (p95[2]>0) {
          rb=histMean[0]/p95[2]; // r/b
          gb=histMean[1]/p95[2]; // b/b
    }
    }
    
}