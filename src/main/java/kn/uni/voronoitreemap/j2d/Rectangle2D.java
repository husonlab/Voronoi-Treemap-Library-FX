/*
 * Rectangle2D.java
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package kn.uni.voronoitreemap.j2d;

public class Rectangle2D {
	private final double minX;
	private final double width;
	private final double minY;
	private final double height;

	public Rectangle2D(double minX, double minY, double width, double height) {
		this.minX = minX;
		this.minY = minY;
		this.width = width;
		this.height = height;
	}

	public double getMinX() {
		return minX;
	}

	public double getMaxX() {
		return minX + width;
	}

	public double getWidth() {
		return width;
	}

	public double getMinY() {
		return minY;
	}

	public double getMaxY() {
		return minY + height;
	}


	public double getHeight() {
		return height;
	}

	public boolean contains(double x, double y) {
		return x >= minX && x <= minX + width && y >= minY && y <= minY + height;
	}

	public boolean contains(double minX1, double minY1, double width1, double height1) {
		return minX1 >= minX && minX1 <= minX + width && minY1 >= minY && minY1 <= minY + height
			   && minX1 + width1 >= minX && minX1 + width1 <= minX + width && minY1 + height1 >= minY && minY1 + height1 <= minY + height;
	}

	public boolean intersects(Rectangle2D that) {
		return this.minX < that.minX + that.width && minX + width > that.minX && minY < that.minY + that.height && minY + height > that.minY;
	}

	public boolean intersects(double minX1, double minY1, double width1, double height1) {
		return this.minX < minX1 + width1 && minX + width > minX1 && minY < minY1 + height1 && minY + height > minY1;
	}
}
