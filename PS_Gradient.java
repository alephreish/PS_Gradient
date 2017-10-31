/*

 Written by Andrey Rozenberg (jaera at yandex.com)
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program. If not, see <http://www.gnu.org/licenses/>.

*/

import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.plugin.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.measure.*;
import ij.io.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.Math.*;

public class PS_Gradient implements PlugIn {

	private Line rTube = null;
	private Line lTube = null;
	public static final double TOLERANCE_WAND   = 20;
	public static final double TOLERANCE_MAXIMA = 5;

	/**
	 * The main program entry.
	 * <p>
	 * Implementation of PlugIn.run()
	 *
	 * @param  arg  plugin arguments
	 */
	public void run(String arg) {
		ImagePlus curImp = WindowManager.getCurrentImage();
		if (curImp == null) return;
		ImagePlus imp = new Duplicator().run(curImp);
		ImageProcessor ip = imp.getProcessor();
		//IJ.log("Image: " + imp.getTitle());
		
		if (getTubes()) {
			ip.invert();
			ImagePlus bkgdImp = new Duplicator().run(imp);
			ImageProcessor bkgdIp = bkgdImp.getProcessor();
			bkgdIp.setColor(getBkgd(ip));
			bkgdIp.fill();
			ImageCalculator ic = new ImageCalculator();
			ImagePlus imp2 = ic.run("Subtract create", imp, bkgdImp);
			//imp2.show();
			bkgdImp.close();

			ImageProcessor ip2 = imp2.getProcessor();
			
			Plot profile1 = new Plot("Left tube", "Distance", "Value");
			PlotProfile(ip2, lTube, profile1, Plot.LINE);
			showPlot(profile1);

			Plot profile2 = new Plot("Right tube", "Distance", "Value");
			PlotProfile(ip2, rTube, profile2, Plot.LINE);
			showPlot(profile2);
			
			imp2.close();
		}
		imp.close();
	}

	private void showPlot(Plot profile) {
		profile.setLimitsToFit(true);
		profile.draw();
		profile.show();
	}

	private boolean getTubes() {
		RoiManager roiManager = RoiManager.getRoiManager();
		int count = 0;
		for (Roi roi : roiManager.getRoisAsArray()) {
			if (roi instanceof Line) {
				if (count > 2) {
					IJ.error("PS Gradient", "More than two line ROIs found");
					return false;
				}
				if (count == 0)
					lTube = (Line)roi;
				else
					rTube = (Line)roi;
				count++;
			}
		}
		if (count != 2) {
			IJ.error("PS Gradient", "Two line ROIs expected");
			return false;
		}
		return true;
	}

	private Color getBkgd(ImageProcessor ip) {
		int midX = (lTube.x1 + lTube.x2 + rTube.x1 + rTube.x2) / 4;
		int midY = (lTube.y1 + lTube.y2 + rTube.y1 + rTube.y2) / 4;
		//IJ.log("MidX: " + Integer.toString(midX));
		//IJ.log("MidY: " + Integer.toString(midY));

		Wand wand = new Wand(ip);
		wand.autoOutline(midX, midY, this.TOLERANCE_WAND, Wand.FOUR_CONNECTED);

		PolygonRoi bkgdRoi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);

		int rSum = 0;
		int gSum = 0;
		int bSum = 0;
		int count = 0;

		for (Point p : bkgdRoi.getContainedPoints()) {
			count++;
			int[] rgb = new int[3];
			ip.getPixel(p.x, p.y, rgb);
			rSum += rgb[0];
			gSum += rgb[1];
			bSum += rgb[2];
		}
		if (count == 0) {
			IJ.error("PS Gradient", "Couldn't find the background color");
			return null;
		}
		return new Color(rSum/count, gSum/count, bSum/count);
	}

	private void PlotProfile(ImageProcessor ip, Line Tube, Plot profile, int shape) {
		Point[] Pts = Tube.getContainedPoints();
		double[]   distances   = new double[Pts.length];
		double[][] intensities = new double[3][Pts.length];
		Color[] colors = new Color[]{ Color.CYAN, Color.MAGENTA, Color.YELLOW };
		double x0 = Pts[0].x;
		double y0 = Pts[0].y;
		int count = 0;
		for (Point p : Pts) {
			int[] rgb = new int[3];
			ip.getPixel(p.x, p.y, rgb);
			double dx = x0 - p.x;
			double dy = y0 - p.y;
			distances[count] = Math.sqrt(dx*dx + dy*dy);
			for (int i = 0; i < 3; i++) {
				intensities[i][count] = (double)rgb[i];
			}
			count++;
		}
		for (int i = 0; i < 3; i++) {
			profile.setColor(colors[i]);
			profile.addPoints(distances, intensities[i], Plot.LINE);

			int[] maxima = MaximumFinder.findMaxima(intensities[i], this.TOLERANCE_MAXIMA, true);
			double[] maxD = new double[maxima.length];
			double[] maxI = new double[maxima.length];
			for (int j = 0; j < maxima.length; j++) {
				int maximum = maxima[j];
				maxD[j] = distances[maximum];
				maxI[j] = intensities[i][maximum];
			}
			profile.setColor(Color.BLACK);
			profile.addPoints(maxD, maxI, Plot.CIRCLE);
		}
	}

}
