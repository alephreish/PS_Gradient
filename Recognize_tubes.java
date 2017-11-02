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

public class Recognize_tubes implements PlugIn {

	public static final double AREA_FRACTION = 20;
	public static final double MIN_CIRC = 0.4;
	public static final double MAX_CIRC = 0.7;

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
		ImageConverter imgC = new ImageConverter(imp);
		imgC.convertToGray8();
		IJ.run(imp, "Auto Local Threshold", "method=Bernsen radius=15 parameter_1=0 parameter_2=0 white");
		//IJ.run(imp, "Auto Threshold", "method=Yen white");
		IJ.run(imp, "Fill Holes", "");
		IJ.run(imp, "Watershed", "");
		ImageStatistics stats = imp.getRawStatistics();
		MyParticleAnalyzer partAnalyzers = new MyParticleAnalyzer(stats.area, this.AREA_FRACTION, this.MIN_CIRC, this.MAX_CIRC);
		partAnalyzers.analyze(imp);
		Line lAxis = partAnalyzers.getLTube();
		Line rAxis = partAnalyzers.getRTube();
		ImageProcessor ip = imp.getProcessor();
		Line lTube = findIntersect(ip, lAxis);
		Line rTube = findIntersect(ip, rAxis);
		
		RoiManager roiManager = RoiManager.getRoiManager();
		if (lTube != null)
			roiManager.addRoi(lTube);
		if (rTube != null)
			roiManager.addRoi(rTube);
		//imp.show();
		imp.close();
	}
	
	private Line findIntersect(ImageProcessor ip, Line axis) {
		Point[] points = axis.getContainedPoints();
		Point p1 = null;
		Point p2 = null;
		for (int i = 0; i < points.length; i++) {
			Point p = points[i];
			if (ip.getPixelValue(p.x, p.y) < 1) {
				p1 = p;
				break;
			}
		}
		for (int i = points.length - 1; i > -1; i--) {
			Point p = points[i];
			if (ip.getPixelValue(p.x, p.y) < 1) {
				p2 = p;
				break;
			}
		}
		return new Line(p1.x, p1.y, p2.x, p2.y);
	}

}
