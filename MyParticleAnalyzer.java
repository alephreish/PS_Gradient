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

import ij.process.*;
import ij.gui.*;
import ij.plugin.filter.*;
import ij.measure.*;
import java.lang.Math.*;

public class MyParticleAnalyzer extends ParticleAnalyzer {

	private Line lTube = null;   // left tube
	private Line rTube = null;   // right tube

	/**
	 * Constructor.
	 */
	public MyParticleAnalyzer(double totalArea, double areaFraction, double minCirc, double maxCirc) {
		super(
			ParticleAnalyzer.EXCLUDE_EDGE_PARTICLES +
			ParticleAnalyzer.SHOW_NONE,
			Measurements.ELLIPSE
		, null, totalArea / areaFraction, totalArea / 2, minCirc, maxCirc);
	}

	/**
	 * Implementation of ParticleAnalyzer.saveResults()
	 *
	 * @param stats the data for the current particle
	 * @param roi   not used
	 */
	protected void saveResults(ImageStatistics stats, Roi roi) {
		if (this.lTube == null) {
			this.lTube = this.centralAxis(stats);
		}
		else if (this.rTube == null) {
			this.rTube = this.centralAxis(stats);
		}
	}

	private Line centralAxis(ImageStatistics stats) {
		if (Math.abs(stats.angle - 90) > 5) return null;
		double dx = stats.major*Math.cos(stats.angle/180.0*Math.PI)/2.0;
		double dy = - stats.major*Math.sin(stats.angle/180.0*Math.PI)/2.0;
		double x1 = stats.xCentroid + dx;
		double x2 = stats.xCentroid - dx;
		double y1 = stats.yCentroid + dy;
		double y2 = stats.yCentroid - dy;
		//double aspectRatio = stats.minor/stats.major;
		return new Line(x1,y1,x2,y2);
	}

	/**
	 * @returns Roi lTube
	 */
	public Line getLTube() {
		return this.lTube;
	}

	/**
	 * @returns Roi rTube
	 */
	public Line getRTube() {
		return this.rTube;
	}

}
