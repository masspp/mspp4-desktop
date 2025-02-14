package ninja.mspp.operation.peaks;

import ninja.mspp.core.model.ms.DataPoints;
import ninja.mspp.core.model.ms.Peak;
import ninja.mspp.core.model.ms.PeakList;
import ninja.mspp.core.model.ms.Point;

public class WakuraPeakDetection {
	public PeakList detect(DataPoints points) {
		PeakList peaks = new PeakList();
		
		int index = 1;
		while (index < points.size()) {
			Point point = points.get(index);
			
			double x = point.getX();
			double y = point.getY();
			double base = y * 0.5;
			
			double position = x * base;
			double weight = base;
			
			boolean foundStart = false;
			int start = index - 1;
			
			while (start >= 0 && !foundStart) {
				Point current = points.get(start);
				double currentX = current.getX();
				double currentY = current.getY();
				
				if(currentY >= y) {
					start = -1;
				}
				else if (currentY > base) {
					double w = currentY - base;
					weight += w;
					position += currentX * w;
				}
				else if(currentY < base) {
					foundStart = true;
				}
				start--;
			}
			
			boolean foundEnd = false;
			int end = index + 1;
			
			while (end < points.size() && !foundEnd) {
				Point current = points.get(end);
				double currentX = current.getX();
				double currentY = current.getY();

				if (currentY >= y) {
					end = points.size();
				}
				else if (currentY > base) {
					double w = currentY - base;
					weight += w;
					position += currentX * w;
				}
				else if (currentY < base) {
					foundEnd = true;
				}
				end++;
			}
			
			if (foundStart && foundEnd) {
				double peakX = position / weight;
				double peakY = y;
				double peakStart = points.get(start + 1).getX();
				double peakEnd = points.get(end - 1).getX();

				peaks.add(new Peak(peakX, peakY, peakStart, peakEnd));
			}
			
			index++;
		}
		
		return peaks;
	}
}
