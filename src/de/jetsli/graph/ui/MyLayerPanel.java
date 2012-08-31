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

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.*;

/**
 * @author Peter Karich
 * @author Kasturi Rangan Raghavan
 */
public class MyLayerPanel extends JPanel {

  private final Collection<MapLayer> layers;
  private Rectangle currentBounds;

  public MyLayerPanel() {
    this(new ArrayList<MapLayer>());
    currentBounds = null;
  }

  public MyLayerPanel(Collection<MapLayer> layer) {
    this.layers = layer;
    this.addComponentListener(new ComponentAdapter() {

      @Override
      public void componentResized(ComponentEvent e) {
        int w = e.getComponent().getWidth();
        int h = e.getComponent().getHeight();
        System.out.println("mainResized:" + w + " " + h);
        for (MapLayer ml : layers) {
          currentBounds = new Rectangle(0, 0, w, h);
          ml.setBounds(currentBounds);
        }
        repaint();
      }
    });
  }

  public void setBuffering(boolean enable) {
    for (MapLayer ml : layers) {
      ml.setBuffering(enable);
    }
  }

  public void addLayer(MapLayer ml) {
    layers.add(ml);
    if (currentBounds != null) {
      ml.setBounds(currentBounds);
    }
  }

  public void removeLayer(MapLayer ml) {
    layers.remove(ml);
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g;
    g2.clearRect(0, 0, getBounds().width, getBounds().height);
    for (MapLayer ml : layers) {
      ml.paint(g2);
    }
  }
}
