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

	/**
	 * The main program entry.
	 * <p>
	 * Implementation of PlugIn.run()
	 *
	 * @param  arg  plugin arguments
	 */
	public void run(String arg) {
		ImagePlus curImg = WindowManager.getCurrentImage();
		ImagePlus img = new Duplicator().run(curImg);
		ImageProcessor ip = img.getProcessor();
		IJ.log("Image: " + img.getTitle());
		
		if (getTubes()) {
			ip.invert();
			ImagePlus bkgdImg = new Duplicator().run(img);
			ImageProcessor bkgdIp = bkgdImg.getProcessor();
			bkgdIp.setColor(getBkgd(ip));
			bkgdIp.fill();
			ImageCalculator ic = new ImageCalculator();
			ImagePlus img2 = ic.run("Subtract create", img, bkgdImg);
			//img2.show();
			bkgdImg.close();

			ImageProcessor ip2 = img2.getProcessor();
			PlotProfile("Left tube",  ip2, lTube);
			PlotProfile("Right tube", ip2, rTube);
			img2.close();
		}
		img.close();
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
				if (count == 0) {
					lTube = (Line)roi;
				}
				else {
					rTube = (Line)roi;
				}
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
		IJ.log("MidX: " + Integer.toString(midX));
		IJ.log("MidY: " + Integer.toString(midY));

		Wand wand = new Wand(ip);
		wand.autoOutline(midX, midY, 20.0, Wand.FOUR_CONNECTED);

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

	private void PlotProfile(String title, ImageProcessor ip, Line Tube) {
		Point[] Pts = Tube.getContainedPoints();
		double[] distance   = new double[Pts.length];
		double[] rIntensity = new double[Pts.length];
		double[] gIntensity = new double[Pts.length];
		double[] bIntensity = new double[Pts.length];
		int count = 0;
		for (Point p : Pts) {
			int[] rgb = new int[3];
			ip.getPixel(p.x, p.y, rgb);
			distance[count]   = (double)p.y;
			rIntensity[count] = (double)rgb[0];
			gIntensity[count] = (double)rgb[1];
			bIntensity[count] = (double)rgb[2];
			count++;
		}
		Plot profile = new Plot(title, "Distance", "Value");
		profile.setColor(Color.CYAN);
		profile.addPoints(distance, rIntensity, Plot.LINE);
		profile.setColor(Color.MAGENTA);
		profile.addPoints(distance, gIntensity, Plot.LINE);
		profile.setColor(Color.YELLOW);
		profile.addPoints(distance, bIntensity, Plot.LINE);
		profile.setLimitsToFit(true);
		profile.draw();
		profile.show();
	}

}
