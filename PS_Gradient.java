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
import java.util.Vector;
import java.util.List;
import java.util.Collections;
import java.util.ArrayList;

import jMEF.*;
import Tools.KMeans;

import jMEF.BregmanHierarchicalClustering.LINKAGE_CRITERION;
import jMEF.Clustering.CLUSTERING_TYPE;

public class PS_Gradient implements PlugIn {

	private Line rTube = null;
	private Line lTube = null;
	public static final double TOLERANCE_WAND   = 20;
	public static final double TOLERANCE_MAXIMA = 5;
	public static final int COLOR_NUM = 2;

	/**
	 * The main program entry.
	 * <p>
	 * Implementation of PlugIn.run()
	 *
	 * @param  arg  plugin arguments
	 */
	public void run(String arg) {
		MixtureModel f = new MixtureModel(3);
		f.EF = new UnivariateGaussianFixedVariance(25);
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
		int n = 0;
		for (Roi roi : roiManager.getRoisAsArray()) {
			if (roi instanceof Line) {
				if (n > 2) {
					IJ.error("PS Gradient", "More than two line ROIs found");
					return false;
				}
				if (n == 0)
					lTube = (Line)roi;
				else
					rTube = (Line)roi;
				n++;
			}
		}
		if (n != 2) {
			IJ.error("PS Gradient", "Two line ROIs expected");
			return false;
		}
		return true;
	}

	private Color getBkgd(ImageProcessor ip) {
		int midX = (lTube.x1 + lTube.x2 + rTube.x1 + rTube.x2) / 4;
		int midY = (lTube.y1 + lTube.y2 + rTube.y1 + rTube.y2) / 4;

		Wand wand = new Wand(ip);
		wand.autoOutline(midX, midY, this.TOLERANCE_WAND, Wand.FOUR_CONNECTED);

		PolygonRoi bkgdRoi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.TRACED_ROI);

		int rSum = 0;
		int gSum = 0;
		int bSum = 0;
		int n = 0;

		for (Point p : bkgdRoi.getContainedPoints()) {
			n++;
			int[] rgb = new int[3];
			ip.getPixel(p.x, p.y, rgb);
			rSum += rgb[0];
			gSum += rgb[1];
			bSum += rgb[2];
		}
		if (n == 0) {
			IJ.error("PS Gradient", "Couldn't find the background color");
			return null;
		}
		return new Color(rSum/n, gSum/n, bSum/n);
	}

	private void PlotProfile(ImageProcessor ip, Line Tube, Plot profile, int shape) {
		Point[] Pts = Tube.getContainedPoints();
		double[]   distances   = new double[Pts.length];
		double[][] intensities = new double[3][Pts.length];
		Color[] colors = new Color[]{ Color.CYAN, Color.MAGENTA, Color.YELLOW };
		double x0 = Pts[0].x;
		double y0 = Pts[0].y;
		int n = 0;
		for (Point p : Pts) {
			int[] rgb = new int[3];
			ip.getPixel(p.x, p.y, rgb);
			double dx = x0 - p.x;
			double dy = y0 - p.y;
			distances[n] = Math.sqrt(dx*dx + dy*dy);
			for (int i = 0; i < this.COLOR_NUM; i++) {
				intensities[i][n] = (double)rgb[i];
			}
			n++;
		}

		for (int i = 0; i < this.COLOR_NUM; i++) {

			profile.setColor(colors[i]);
			profile.addPoints(distances, intensities[i], Plot.LINE);

			int[] maxima = MaximumFinder.findMaxima(intensities[i], this.TOLERANCE_MAXIMA, false);
			double maxDist = 0;
			for (int max : maxima) {
				if (distances[max] > maxDist) {
					maxDist = distances[max];
				}
			}
			double[] maxD = new double[maxima.length - 1];
			double[] maxI = new double[maxima.length - 1];
			n = 0;
			for (int max : maxima) {
				if (distances[max] < maxDist) {
					maxD[n] = distances[max];
					maxI[n] = intensities[i][max];
					n++;
				}
			}
			profile.setColor(Color.BLACK);
			profile.addPoints(maxD, maxI, Plot.CIRCLE);

			List<Double> sample = new ArrayList<Double>();
			for (int j = 0; j < distances.length; j++) {
				if (distances[j] < maxDist) {
					sample.addAll(Collections.nCopies((int)intensities[i][j], distances[j]));
				}
			}
			MixtureModel mmef1 = findGauss(sample, 4);
			//MixtureModel mmef2 = findOptimalGauss(sample);
			plotGauss(profile, distances, mmef1, sample.size());
		}
	}

	private MixtureModel findGauss(List<Double>sample, int components) {
		PVector[] points = DoubletoPVector(sample);
		Vector<PVector>[] clusters = KMeans.run(points, components);
		MixtureModel mmef1 = BregmanSoftClustering.initialize(clusters, new UnivariateGaussian());
		return BregmanSoftClustering.run(points, mmef1);
	}

	private MixtureModel findOptimalGauss(List<Double>sample) {
		int components1 = 6;
		MixtureModel mm1 = findGauss(sample, components1);
		HierarchicalMixtureModel hmm = BregmanHierarchicalClustering.build(mm1, CLUSTERING_TYPE.SYMMETRIC, LINKAGE_CRITERION.MAXIMUM_DISTANCE);
		MixtureModel mm2 = hmm.getOptimalMixtureModel(0.5);
		int componentsOpt = mm2.size + 2;
		if (components1 == componentsOpt) {
			return mm1;
		} else {
			return findGauss(sample, componentsOpt);
		}
	}

	private PVector[] DoubletoPVector(List<Double> sample) {
		int size = sample.size();
		PVector[] points = new PVector[size];
		for (int i = 0; i < size; i++) {
			PVector point = new PVector(1);
			point.array[0] = sample.get(i);
			points[i] = point;
		}
		return points;
	}

	private double Gauss(double x, double w, double a, double s2) {
		double dx = x - a;
		return w * Math.exp( - dx*dx / s2 / 2 ) / Math.sqrt(2 * Math.PI * s2);
	}

	private void plotGauss(Plot profile, double[] distances, MixtureModel mm, int scale) {
		for (int i = 0; i < mm.size; i++) {
			PVector params = (PVector)mm.param[i];
			double w  = scale * mm.weight[i];
			double a  = params.array[0];
			double s2 = params.array[1];
			int n = 0;
			double[] intensities = new double[distances.length];
			for (int j = 0; j < distances.length; j++) {
				intensities[j] = Gauss(distances[j], w, a, s2);
			}
//			IJ.log(String.format("Component %d", i));
//			IJ.log(String.format("Weight %f", w));
//			IJ.log(String.format("Points %d", distances.length));
			profile.setColor(Color.BLACK);
			profile.addPoints(distances, intensities, Plot.LINE);
		}
	}

}
