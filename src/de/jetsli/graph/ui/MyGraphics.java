/*
 *  Copyright 2012 Peter Karich info@jetsli.de
 * 
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package de.jetsli.graph.ui;

import de.jetsli.graph.storage.Graph;
import de.jetsli.graph.util.CoordTrig;
import de.jetsli.graph.util.shapes.BBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

/**
 * @author Peter Karich
 */
public class MyGraphics {

    private Graph g;
    private double scaleX = 0.00007;
    private double scaleY = 0.00007;
    // initial position to center unterfranken
    // 49.50381,9.953613 -> south unterfranken
    private double offsetX = 118.441318 + 0.05;
    private double offsetY = -(90 - (34.074949));
    private BBox bounds = new BBox(-180, 180, -90, 90);

    public MyGraphics(Graph g) {
        this.g = g;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public double getScaleX() {
        return scaleX;
    }

    public double getScaleY() {
        return scaleY;
    }

  public void plotEdge(Graphics2D g2, double lat, double lon, double lat2, double lon2, int width, Color c) {
    Color old = g2.getColor();
    g2.setColor(c);
    g2.setStroke(new BasicStroke(width));
    g2.drawLine((int) getX(lon), (int) getY(lat), (int) getX(lon2), (int) getY(lat2));
    g2.setColor(old);
  }

    public void plotEdge(Graphics2D g2, double lat, double lon, double lat2, double lon2, int width) {
        g2.setStroke(new BasicStroke(width));
        g2.drawLine((int) getX(lon), (int) getY(lat), (int) getX(lon2), (int) getY(lat2));
    }

    public void plotEdge(Graphics2D g2, double lat, double lon, double lat2, double lon2) {
        plotEdge(g2, lat, lon, lat2, lon2, 1);
    }

    public double getX(double lon) {
        return (lon + offsetX) / scaleX;
    }

    public double getY(double lat) {
        return (90 - lat + offsetY) / scaleY;
    }

    public double getLon(int x) {
        return x * scaleX - offsetX;
    }

    public double getLat(int y) {
        return 90 - (y * scaleY - offsetY);
    }

    public void plotPOI(Graphics2D g2, CoordTrig<String> poi, Color c) {
      plotPOI(g2, poi, c, 4);
    }

    public void plotPOI(Graphics2D g2, CoordTrig<String> poi, Color c, int dotRadius) {
      Color old = g2.getColor();
      g2.setColor(c);
      final double lat = poi.lat;
      final double lon = poi.lon;
      if (!(lat < bounds.minLat ||
            lat > bounds.maxLat ||
            lon < bounds.minLon ||
            lon > bounds.maxLon)) {
        plot(g2, lat, lon, dotRadius);
      }
      g2.setColor(old);
    }
  public void plotNode(Graphics2D g2, int loc, Color c) {
    plotNode(g2, loc, c, 4);
  }
    public void plotNode(Graphics2D g2, int loc, Color c, int radius) {
        Color old = g2.getColor();
        g2.setColor(c);
        double lat = g.getLatitude(loc);
        double lon = g.getLongitude(loc);
        if (!(lat < bounds.minLat ||
              lat > bounds.maxLat ||
              lon < bounds.minLon ||
              lon > bounds.maxLon)) {
          plot(g2, lat, lon, radius);
        }
        g2.setColor(old);
    }

    public void plot(Graphics2D g2, double lat, double lon, int width) {
        double x = getX(lon);
        double y = getY(lat);

        g2.fillOval((int) x, (int) y, width, width);
    }
    private Logger logger = LoggerFactory.getLogger(getClass());

    void scale(int x, int y, boolean zoomIn) {
        double tmpFactor = 0.5f;
        if (!zoomIn)
            tmpFactor = 2;

        double oldScaleX = scaleX;
        double oldScaleY = scaleY;
        double resX = scaleX * tmpFactor;
        if (resX > 0)
            scaleX = resX;

        double resY = scaleY * tmpFactor;
        if (resY > 0)
            scaleY = resY;

        // respect mouse x,y when scaling
        // TODO minor bug: compute difference of lat,lon position for mouse before and after scaling
        if (zoomIn) {
            offsetX -= (offsetX + x) * scaleX;
            offsetY -= (offsetY + y) * scaleY;
        } else {
            offsetX += x * oldScaleX;
            offsetY += y * oldScaleY;
        }

        logger.info("mouse wheel moved => repaint. zoomIn:" + zoomIn + " " + offsetX + "," + offsetY
                + " " + scaleX + "," + scaleY);
    }

    public void setNewOffset(int offX, int offY) {
        offsetX += offX * scaleX;
        offsetY += offY * scaleY;
    }

    public BBox setBounds(int minX, int maxX, int minY, int maxY) {
        double minLon = getLon(minX);
        double maxLon = getLon(maxX);

        double maxLat = getLat(minY);
        double minLat = getLat(maxY);
        bounds = new BBox(minLon, maxLon, minLat, maxLat);
        return bounds;
    }
}
