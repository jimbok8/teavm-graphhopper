package org.teavm.graphhopper.webapp;

import com.graphhopper.routing.Path;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.teavm.dom.browser.Window;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.graphhopper.ClientSideGraphHopper;
import org.teavm.graphhopper.webapp.leaflet.LatLng;
import org.teavm.graphhopper.webapp.leaflet.LatLngBounds;
import org.teavm.graphhopper.webapp.leaflet.LeafletEventListener;
import org.teavm.graphhopper.webapp.leaflet.LeafletMap;
import org.teavm.graphhopper.webapp.leaflet.LeafletMapOptions;
import org.teavm.graphhopper.webapp.leaflet.LeafletMouseEvent;
import org.teavm.graphhopper.webapp.leaflet.Marker;
import org.teavm.graphhopper.webapp.leaflet.Polyline;
import org.teavm.graphhopper.webapp.leaflet.TileLayer;
import org.teavm.graphhopper.webapp.leaflet.TileLayerOptions;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperUI {
    private static Window window = (Window)JS.getGlobal();
    private static HTMLDocument document = window.getDocument();
    private HTMLElement element;
    private LeafletMap map;
    private ClientSideGraphHopper graphHopper = new ClientSideGraphHopper();
    private Marker firstMarker;
    private Marker secondMarker;
    private Polyline pathDisplay;

    public GraphHopperUI() {
        this(document.createElement("div"));
        element.setAttribute("style", "width: 800px; height: 480px");
    }

    public GraphHopperUI(String elementId) {
        this(document.getElementById(elementId));
    }

    public GraphHopperUI(HTMLElement element) {
        this.element = element;
        map = LeafletMap.create(element, LeafletMapOptions.create());
        TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create()
                .attribution("&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a> " +
                        "contributors"))
                .addTo(map);
        map.onClick(new LeafletEventListener<LeafletMouseEvent>() {
            @Override public void occur(LeafletMouseEvent event) {
                click(event.getLatlng());
            }
        });
    }

    public void load(InputStream input) throws IOException {
        graphHopper.load(input);
        BBox bounds = graphHopper.getBounds();
        LatLng southWest = LatLng.create(bounds.minLat, bounds.minLon);
        LatLng northEast = LatLng.create(bounds.maxLat, bounds.maxLon);
        LatLngBounds leafletBounds = LatLngBounds.create(southWest, northEast);
        map.setMaxBounds(leafletBounds);
        map.setView(leafletBounds.getCenter(), 10);
    }

    public HTMLElement getElement() {
        return element;
    }

    private void click(LatLng latlng) {
        if (secondMarker != null) {
            map.removeLayer(firstMarker);
            map.removeLayer(secondMarker);
            if (pathDisplay != null) {
                map.removeLayer(pathDisplay);
            }
            firstMarker = Marker.create(latlng).addTo(map);
            secondMarker = null;
            pathDisplay = null;
        } else if (firstMarker == null) {
            firstMarker = Marker.create(latlng).addTo(map);
        } else {
            secondMarker = Marker.create(latlng).addTo(map);
            LatLng first = firstMarker.getLatLng();
            LatLng second = secondMarker.getLatLng();
            int firstNode = graphHopper.findNode(first.getLat(), first.getLng());
            int secondNode = graphHopper.findNode(second.getLat(), second.getLng());
            if (firstNode < 0 || secondNode < 0) {
                pathDisplay = null;
                window.alert("One of the provided points is outside of the known region");
                return;
            }
            Path path = graphHopper.route(firstNode, secondNode);
            InstructionList instructions = path.calcInstructions(new TeaVMTranslation());
            List<LatLng> array = new ArrayList<>();
            for (Instruction insn : instructions) {
                PointList points = insn.getPoints();
                for (int i = 0; i < points.size(); ++i) {
                    array.add(LatLng.create(points.getLat(i), points.getLon(i)));
                }
            }
            pathDisplay = Polyline.create(array).addTo(map);
        }
    }
}