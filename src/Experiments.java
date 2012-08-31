/*
 * Copyright (c) 2012, UCLA Networked and Embedded Systems Lab (NESL)
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the UCLA nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import de.jetsli.graph.reader.OSMReader;
import de.jetsli.graph.storage.Graph;
import de.jetsli.graph.trees.QuadTree;
import de.jetsli.graph.trees.QuadTreeSimple;
import de.jetsli.graph.ui.MiniGraphUI;
import de.jetsli.graph.util.CoordTrig;
import de.jetsli.graph.util.CoordTrigObjEntry;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * TODO: Give a one line description.
 *
 * @author Kasturi Rangan Raghavan (kastur@gmail.com)
 */
public class Experiments {

  public static void main(String[] args) throws IOException {
    Experiments e = new Experiments("data/westwood.osm", "data/los-angeles.amenities.json");
    e.load();
  }

  final private String mOsmFile;
  final private String mPlacesFile;


  private Graph mGraph;
  private HashMap<String, ArrayList<CoordTrig<String>>> mPlacesByType;
  private QuadTree<String> mPlacesIndex;

  public Experiments(String osmFile, String placesFile) {
    mOsmFile = osmFile;
    mPlacesFile = placesFile;
    mGraph = null;
    mPlacesByType = null;
    mPlacesIndex = null;
  }

  private void load() throws IOException {
    String storageLocation = "graph_storage";
    new File(storageLocation).mkdir();
    OSMReader reader = new OSMReader(storageLocation, 5 * 1000 * 1000);
    {
      FileInputStream is = new FileInputStream(mOsmFile);
      reader.preprocessAcceptHighwaysOnly(is);
    }

    {
      FileInputStream is = new FileInputStream(mOsmFile);
      reader.writeOsm2Graph(is);
    }

    reader.cleanUp();
    reader.flush();

    mGraph = reader.getGraph();

    loadPlaces();
    ArrayList<CoordTrig<String>> hospitals = mPlacesByType.get("hospital");
    MiniGraphUI ui = new MiniGraphUI(mGraph, mPlacesIndex);
    ui.initialize();
    ui.visualize();
  }

  public void loadPlaces() throws IOException {
    mPlacesIndex = new QuadTreeSimple<String>();
    mPlacesByType = new HashMap<String, ArrayList<CoordTrig<String>>>();

    FileReader reader = new FileReader(mPlacesFile);
    JsonElement root = new JsonParser().parse(reader);
    for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject().entrySet()) {
      final String amenity = entry.getKey();
      ArrayList<CoordTrig<String>> placesList = new ArrayList<CoordTrig<String>>();
      mPlacesByType.put(amenity, placesList);
      for (JsonElement o : entry.getValue().getAsJsonArray()) {
        JsonArray list = o.getAsJsonArray();
        double lon = list.get(0).getAsDouble();
        double lat = list.get(1).getAsDouble();
        String name = list.get(2).getAsString();
        // If we want to, we can also put the name of the place into the quad-tree.
        CoordTrigObjEntry<String> coord = new CoordTrigObjEntry<String>(amenity, lat, lon);
        placesList.add(coord);
        mPlacesIndex.add(lat, lon, name);
      }
    }
  }

}
