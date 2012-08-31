package de.jetsli.graph.ui;

import de.jetsli.graph.routing.AStar;
import de.jetsli.graph.routing.AlgoType;
import de.jetsli.graph.routing.Path;
import de.jetsli.graph.routing.RoutingAlgorithm;
import de.jetsli.graph.storage.Graph;
import de.jetsli.graph.storage.Location2IDQuadtree;
import de.jetsli.graph.trees.QuadTree;
import de.jetsli.graph.util.CoordTrig;
import de.jetsli.graph.util.CoordTrigObjEntry;
import de.jetsli.graph.util.EdgeIterator;
import de.jetsli.graph.util.shapes.BBox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.*;

/**
 * @author Kasturi Rangan Raghavan
 * @author Peter Karich
 */
public class MiniGraphUI {

  final private boolean fastPaint = false;
  final private Logger mLogger = LoggerFactory.getLogger(getClass());

  final private Graph mRoadGraph;
  final private QuadTree<String> mPlacesIndex;

  private Location2IDQuadtree mRoadIndex;
  private RoutingAlgorithm mRouter;

  private JPanel mPanel;
  private MyLayerPanel mLayeredPanel;
  private MapLayer mRoadsLayer;
  private SingleRouteLayer mSingleRouteLayer;
  private NearbyPlacesLayer mNearbyPlacesLayer;
  private WaypointsLayer mWaypointsLayer;
  private AnalysisLayer mAnalysisLayer;
  private MapLayer mHUDLayer;
  private MyGraphics mGraphicsUtil;
  private String mDebugStringLatLon = "";

  private AtomicBoolean mDragging;
  private int mCurrentPosX;
  private int mCurrentPosY;

  public MiniGraphUI(Graph roadGraph, QuadTree<String> placesIndex) {
    mPlacesIndex = placesIndex;
    mRoadGraph = roadGraph;
  }

  public void initialize() {
    mDragging = new AtomicBoolean(false);
    mGraphicsUtil = new MyGraphics(mRoadGraph);
    mRoadIndex = new Location2IDQuadtree(mRoadGraph);
    mRoadIndex.prepareIndex(90000);
    mRouter = new AStar(mRoadGraph);

    mPanel = new JPanel() {
      @Override
      protected void paintComponent(Graphics g) {
        Rectangle b = mPanel.getBounds();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, b.width, b.height);

        g.setColor(Color.BLUE);
        g.drawString("lat, lon: " + mDebugStringLatLon, 40, 20);
        g.drawString("scale:" + mGraphicsUtil.getScaleX(), 40, 40);
        int w = mLayeredPanel.getBounds().width;
        int h = mLayeredPanel.getBounds().height;
        g.drawString("bounds: " + mGraphicsUtil.setBounds(0, w, 0, h).toLessPrecisionString(), 40,
                     60);
      }
    };

    mLayeredPanel = new MyLayerPanel();

    mLayeredPanel.addLayer(mRoadsLayer = new DefaultMapLayer() {

      @Override
      public void paintComponent(Graphics2D g2) {
        clearGraphics(g2);

        Rectangle bounds = getBounds();
        BBox bbox = mGraphicsUtil.setBounds(0, bounds.width, 0, bounds.height);

        int numNodes = mRoadGraph.getNodes();
        for (int nodeIndex = 0; nodeIndex < numNodes; nodeIndex++) {
          double nodeLat = mRoadGraph.getLatitude(nodeIndex);
          double nodeLon = mRoadGraph.getLongitude(nodeIndex);
          if (nodeLat < bbox.minLat || nodeLat > bbox.maxLat ||
              nodeLon < bbox.minLon || nodeLon > bbox.maxLon) {
            continue;
          }

          EdgeIterator edgeIterator = mRoadGraph.getOutgoing(nodeIndex);
          while (edgeIterator.next()) {
            int nodeId = edgeIterator.node();
            int sum = nodeIndex + nodeId;
            double neighborLat = mRoadGraph.getLatitude(nodeId);
            double neighborLon = mRoadGraph.getLongitude(nodeId);
            mGraphicsUtil.plotEdge(g2, nodeLat, nodeLon, neighborLat, neighborLon);
          }
        }
      }
    });

    // TODO: Add ---MODE--- display and other status.
    /*
    mLayeredPanel.addLayer(mHUDLayer = new DefaultMapLayer() {

      @Override
      protected void paintComponent(Graphics2D g2) {
        makeTransparent(g2);
        Collection<CoordTrig<String>>
            nearbyPlaces =
            mPlacesIndex.getNodes(findIDLat, findIDLon, 0.1);
        try {
          CoordTrig<String> entry = nearbyPlaces.iterator().next();
          mLogger.info("found " + entry.getValue());
          g2.drawString(entry.getValue(), 200, 30);
        } catch (NoSuchElementException ex) {
          mLogger.info("no element found");
        }
      }
    });*/
  }

  private void drawRoute(Graphics2D g2, int fromId, int toId, Color strokeColor, int strokeWidth) {
    Path path = mRouter.clear().setType(AlgoType.FASTEST).calcPath(fromId, toId);

    if (path == null) {
      return;
    }

    g2.setColor(Color.BLUE.brighter().brighter());
    int tmpLocs = path.locations();
    double prevLat = -1;
    double prevLon = -1;
    for (int i = 0; i < tmpLocs; i++) {
      int id = path.location(i);
      double lat = mRoadGraph.getLatitude(id);
      double lon = mRoadGraph.getLongitude(id);
      if (prevLat >= 0) {
        mGraphicsUtil.plotEdge(g2, prevLat, prevLon, lat, lon, strokeWidth, strokeColor);
      }
      prevLat = lat;
      prevLon = lon;
    }
  }

  public void visualize() {
    try {
      SwingUtilities.invokeAndWait(new Runnable() {
        @Override
        public void run() {
          int frameHeight = 800;
          int frameWidth = 1200;
          JFrame frame = new JFrame("GraphHopper 6b72 style");
          frame.setLayout(new BorderLayout());
          frame.add(mLayeredPanel, BorderLayout.CENTER);
          frame.add(mPanel, BorderLayout.NORTH);
          mPanel.setPreferredSize(new Dimension(300, 100));

          mLayeredPanel.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
              mGraphicsUtil.scale(e.getX(), e.getY(), e.getWheelRotation() < 0);
              mLayeredPanel.repaint();
            }
          });

          MouseAdapter mouseListener = new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

            }

            @Override
            public void mouseDragged(MouseEvent e) {
              mDragging.set(true);
              update(e);
              updateLatLon(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
              boolean dragging = mDragging.getAndSet(false);
              if (dragging) {
                update(e);
              }
            }

            private void update(MouseEvent e) {
              mGraphicsUtil.setNewOffset(e.getX() - mCurrentPosX, e.getY() - mCurrentPosY);
              repaintRoads();
            }

            @Override
            public void mouseMoved(MouseEvent e) {
              updateLatLon(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
              updateLatLon(e);
            }
          };

          mLayeredPanel.addMouseListener(mouseListener);
          mLayeredPanel.addMouseMotionListener(mouseListener);
          mLayeredPanel.addKeyListener(new KeyAdapter() {
            private CoordTrig<String> nearbyLocation = null;

            @Override
            public void keyPressed(KeyEvent keyEvent) {
              CoordTrig<String> mouseLocation = getMouseLatLon();

              if (keyEvent.getKeyChar() == 'b') {
                mLogger.info("Inside 'b' action");

                if (mNearbyPlacesLayer != null) {
                  mLayeredPanel.removeLayer(mNearbyPlacesLayer);
                }
                mNearbyPlacesLayer = new NearbyPlacesLayer(mouseLocation);
                mLayeredPanel.addLayer(mNearbyPlacesLayer);

                if (mWaypointsLayer != null)
                  mLayeredPanel.removeLayer(mWaypointsLayer);
                mWaypointsLayer = new WaypointsLayer(mouseLocation);
                mLayeredPanel.addLayer(mWaypointsLayer);

                nearbyLocation = mouseLocation;

                mNearbyPlacesLayer.repaint();
                mLayeredPanel.repaint();

              } else if (keyEvent.getKeyChar() == 'r') {
                mLogger.info("Inside 'r' action");
                if (nearbyLocation == null) {
                  mLogger.info("Do 'b' first.");
                  return;
                }

                if (mSingleRouteLayer != null) {
                  mLayeredPanel.removeLayer(mSingleRouteLayer);
                }
                mSingleRouteLayer = new SingleRouteLayer(nearbyLocation, mouseLocation);
                mLayeredPanel.addLayer(mSingleRouteLayer);

                mSingleRouteLayer.repaint();
                mLayeredPanel.repaint();

              } else if (keyEvent.getKeyChar() == 'w') {
                if (mWaypointsLayer == null) {
                  mLogger.info("Do 'b' first");
                  return;
                }

                mWaypointsLayer.addWaypoint(mouseLocation);
                mWaypointsLayer.repaint();
                mLayeredPanel.repaint();

              } else if (keyEvent.getKeyChar() == 'a') {

                if (mNearbyPlacesLayer == null || mSingleRouteLayer == null || mWaypointsLayer == null) {
                  mLogger.info("Do 'b' 'r' 'w' first");
                  return;
                }

                if (mAnalysisLayer != null)
                  mLayeredPanel.removeLayer(mAnalysisLayer);
                mAnalysisLayer = new AnalysisLayer(
                    mSingleRouteLayer.getStartPlace(),
                    mSingleRouteLayer.getmDestPlace(),
                    mWaypointsLayer.getLastWaypoint());
                mLayeredPanel.addLayer(mAnalysisLayer);
                mAnalysisLayer.repaint();
                mLayeredPanel.repaint();
              }
            }
          });
          mLayeredPanel.setFocusable(true);

          frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
          frame.setSize(frameWidth + 10, frameHeight + 30);
          frame.setVisible(true);
        }
      });
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private CoordTrig<String> getMouseLatLon() {
    return new CoordTrigObjEntry<String>(
        "currentLocation", mGraphicsUtil.getLat(mCurrentPosY), mGraphicsUtil.getLon(mCurrentPosX));
  }

  private void updateLatLon(MouseEvent e) {
    mDebugStringLatLon = mGraphicsUtil.getLat(e.getY()) + "," + mGraphicsUtil.getLon(e.getX());
    mPanel.repaint();
    mCurrentPosX = e.getX();
    mCurrentPosY = e.getY();
  }

  private void repaintRoads() {
    mRoadsLayer.repaint();

    if (mNearbyPlacesLayer != null)
      mNearbyPlacesLayer.repaint();

    if (mSingleRouteLayer != null)
      mSingleRouteLayer.repaint();

    if (mWaypointsLayer != null)
      mWaypointsLayer.repaint();

    if (mAnalysisLayer != null)
      mAnalysisLayer.repaint();

    mLayeredPanel.repaint();
  }

  private class SingleRouteLayer extends DefaultMapLayer {

    private CoordTrig<String> mStartPlace;
    private CoordTrig<String> mDestPlace;

    public SingleRouteLayer(CoordTrig<String> startPlace, CoordTrig<String> destPlace) {
      mStartPlace = startPlace;
      mDestPlace = destPlace;
    }

    public CoordTrig<String> getStartPlace() {
      return mStartPlace;
    }

    public CoordTrig<String> getmDestPlace() {
      return mDestPlace;
    }

    @Override
    protected void paintComponent(Graphics2D g2) {
      makeTransparent(g2);
      int sourceId = mRoadIndex.findID(mStartPlace.lat, mStartPlace.lon);
      int destId = mRoadIndex.findID(mDestPlace.lat, mDestPlace.lon);

      drawRoute(g2, sourceId, destId, Color.BLUE, 3);

      mGraphicsUtil.plotPOI(g2, mStartPlace, Color.CYAN, 10);
      mGraphicsUtil.plotNode(g2, sourceId, Color.CYAN.darker().darker(), 7);

      mGraphicsUtil.plotPOI(g2, mDestPlace, Color.MAGENTA, 10);
      mGraphicsUtil.plotNode(g2, destId, Color.MAGENTA.darker().darker(), 7);
    }
  }

  private class NearbyPlacesLayer extends DefaultMapLayer {

    CoordTrig<String> mCenterPlace;

    public NearbyPlacesLayer(CoordTrig<String> centerPlace) {
      mCenterPlace = centerPlace;
    }

    public CoordTrig<String> getmCenterPlace() {
      return mCenterPlace;
    }

    @Override
    protected void paintComponent(Graphics2D g2) {
      makeTransparent(g2);
      mGraphicsUtil.plotPOI(g2, mCenterPlace, Color.GREEN, 10);

      Collection<CoordTrig<String>> nearbyPlaces =
          mPlacesIndex.getNodes(mCenterPlace.lat, mCenterPlace.lon, 2.0 /* kilometers */);

      for (CoordTrig<String> place : nearbyPlaces) {
        mGraphicsUtil.plotPOI(g2, place, Color.RED, 5);
      }
    }
  }

  private class WaypointsLayer extends DefaultMapLayer {
    List<CoordTrig<String>> mWaypoints;

    public WaypointsLayer(CoordTrig<String> startingPoint) {
      mWaypoints = new LinkedList<CoordTrig<String>>();
      mWaypoints.add(startingPoint);
    }

    public void addWaypoint(CoordTrig<String> waypoint) {
      mWaypoints.add(waypoint);
    }

    public CoordTrig<String> getLastWaypoint() {
      return mWaypoints.get(mWaypoints.size() - 1);
    }

    @Override
    protected void paintComponent(Graphics2D g2) {
      makeTransparent(g2);
      Iterator<CoordTrig<String>> waypointIterator = mWaypoints.iterator();
      CoordTrig<String> prevPoint = waypointIterator.next();
      int prevId = mRoadIndex.findID(prevPoint.lat, prevPoint.lon);
      mGraphicsUtil.plotNode(g2, prevId, Color.GREEN.darker().darker(), 4);

      while(waypointIterator.hasNext()) {
        CoordTrig<String> currPoint = waypointIterator.next();
        int currId = mRoadIndex.findID(currPoint.lat, currPoint.lon);
        drawRoute(g2, prevId, currId, Color.GREEN, 3);
        mGraphicsUtil.plotNode(g2, currId, Color.GREEN.darker().darker(), 4);
        prevId = currId;
      }


    }
  }

  public class AnalysisLayer extends DefaultMapLayer {
    CoordTrig<String> mStartPlace;
    CoordTrig<String> mDestPlace;
    CoordTrig<String> mLastWaypoint;
    public AnalysisLayer(
        CoordTrig<String> startPlace, CoordTrig<String> destPlace, CoordTrig<String> lastWaypoint) {
      mStartPlace = startPlace;
      mDestPlace = destPlace;
      mLastWaypoint = lastWaypoint;
    }

    @Override
    protected void paintComponent(Graphics2D g2) {
      drawRoute(g2,
                mRoadIndex.findID(mStartPlace.lat, mStartPlace.lon),
                mRoadIndex.findID(mLastWaypoint.lat, mLastWaypoint.lon),
                Color.PINK, 3);

      drawRoute(g2,
                mRoadIndex.findID(mLastWaypoint.lat, mLastWaypoint.lon),
                mRoadIndex.findID(mDestPlace.lat, mDestPlace.lon),
                Color.PINK.darker(), 4);
    }
  }
}
