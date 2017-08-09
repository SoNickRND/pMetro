package com.utyf.pmetro.map;

import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.support.annotation.Nullable;

import com.utyf.pmetro.MapActivity;
import com.utyf.pmetro.map.routing.RouteInfo;
import com.utyf.pmetro.map.vec.VEC;
import com.utyf.pmetro.util.ExtPointF;
import com.utyf.pmetro.util.StationsNum;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays metro map
 *
 * @author Utyf
 */

public class MAP {
    public MAP_Parameters parameters;
    private MapData mapData;

    private Line[] lines;
    private Route route;
    private Paint p;

    public MAP(MapData mapData) {
        this.mapData = mapData;
        p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setFilterBitmap(true);  // todo switch by settings
    }

    public int load(String name) {
        parameters = new MAP_Parameters();
        MAP_Parameters defaultParameters = mapData.mapMetro != null ? mapData.mapMetro.parameters : null;
        if (parameters.load(name, defaultParameters, mapData.transports) < 0) {
            return -1;
        }
        if (!mapData.routingState.routeExists()) {
            setActiveTransports();
        }

        lines = new Line[parameters.la.size()];
        for (int i = 0; i < parameters.la.size(); i++)
            lines[i] = new Line(parameters.la.get(i), parameters, mapData);

        for (Line l : lines) l.CreatePath(); // create line path for drawing

        return 0;
    }

    public PointF getSize() {  // todo   adjust to all vecs
        if (parameters.vecs == null || parameters.vecs[0] == null) return new PointF();
        return parameters.vecs[0].Size;
    }

    public void setActiveTransports() {
        //if( TRP.routeStart==null )  // do not change active if route marked
        mapData.routingState.setActive(parameters.allowedTRPs);
        MapActivity.mapActivity.setActiveTRP();

        mapData.routingState.resetRoute();
    }

    @Nullable
    public Line getLine(int tNum, int lNum) {
        if (lines == null) return null;
        for (Line ll : lines)
            if (ll.trpNum == tNum && ll.lineNum == lNum) return ll;
        return null;
    }

    @Nullable
    private StationsNum stationByPoint(float x, float y) {
        int st;

        for (Line ll : lines)
            if ((st = ll.stationByPoint(x, y)) != -1)
                return new StationsNum(ll.trpNum, ll.lineNum, st);

        return null;
    }

    private StationsNum[] stationsByPoint(float x, float y, int hitCircle) {  // todo  use it
        ArrayList<StationsNum> stns = new ArrayList<>();
        Integer[] st;

        for (Line ll : lines)
            if ((st = ll.stationsByPoint(x, y, hitCircle)) != null)
                for (Integer stn : st)
                    stns.add(new StationsNum(ll.trpNum, ll.lineNum, stn));

        return stns.toArray(new StationsNum[stns.size()]);
    } //*/

    public String singleTap(float x, float y, int hitCircle, boolean isLongTap) {
        StationsNum[] stns = stationsByPoint(x, y, hitCircle);

        if (stns.length == 1) {
            StationsNum ls = stns[0];
            MapActivity.mapActivity.mapView.selectStation(ls, isLongTap);
            return null;
        }
        else if (stns.length > 1) {
            //MapActivity.mapActivity.mapView.menuStns = stns;
            //MapActivity.mapActivity.mapView.showContextMenu();
            MapActivity.mapActivity.showStationSelectionMenu(stns, isLongTap);
            return null;
                /*ls=stns[1];
                String str="hits:";
                for( StationsNum stn : stns )
                    str = str + " " + stn.trp+","+ stn.line+","+ stn.stn;
                Log.e("MAP /225",str); */
        }
        else {
            if (parameters.vecs == null || parameters.vecs.length == 0 || parameters.vecs[0] == null)
                return null;
            String action = parameters.vecs[0].SingleTap(x, y);  // todo   proceed all vecs
            if (action == null) {
                mapData.routingState.clearRoute();
                clearStationTimes();
            }

            return action;
        }
    }

    public void showStationInfo(StationsNum stn) {
        StationData stationData = new StationData();
        stationData.load(stn, mapData);

        Intent intent = new Intent(MapActivity.mapActivity, StationInfoActivity.class);
        intent.putExtra("stationData", stationData);
        MapActivity.mapActivity.startActivity(intent);
    }

    public void Draw(Canvas canvas) {
        DrawMAP(canvas);

        if (mapData.routingState.isRouteStartSelected() && mapData.routingState.isRouteEndSelected()) {   // greying map
            canvas.drawColor(0xb4ffffff);
        }

        drawRoute(canvas, p);   // drawing route

        drawEndStation(canvas, p);   // mark end station
        drawStartStation(canvas, p);  // mark start station

        drawBlockedStations(canvas, p);
    }

    public void createRoute(RouteInfo routeInfo) {
        route = new Route(mapData);
        StationsNum[] stations = routeInfo.getStations();
        for (StationsNum station : stations) {
            route.addNode(station);
        }
        route.makePath();
    }

    public void clearRoute() {
        route = null;
    }

    private void drawRoute(Canvas canvas, Paint paint) {
        if (route != null) {
            route.Draw(canvas, paint);
        }
    }

    private void DrawMAP(Canvas canvas) {
        //int s = canvas.save();
        //canvas.scale(scale, scale);           // VEC is not scaled
        for (VEC vv : parameters.vecs) vv.Draw(canvas);   // draw background
        //canvas.restoreToCount(s);

        if (!parameters.IsVector) {  // for pixel maps - draw times end exit
            if (mapData.routingState.isRouteStartSelected())
                for (Line ll : lines) ll.drawAllTexts(canvas);
            return;
        }

        p.setStyle(Paint.Style.STROKE);
        for (Line ll : lines)
            ll.DrawLines(canvas, p);

        p.setStyle(Paint.Style.FILL);
        for (Line ll : lines)
            ll.DrawYellowStations(canvas, p);

        mapData.transports.DrawTransfers(canvas, p, this);

        p.setStyle(Paint.Style.FILL);
        for (Line ll : lines) ll.DrawStations(canvas, p);
        for (Line ll : lines) ll.DrawStationNames(canvas, p);
    }

    public void redrawRoute() {
        if (route != null) {
            route.makePath();
        }
    }

    public void setStationTimes(StationsNum[] stationNums, float[] stationTimes) {
//        Map<StationsNum, Float> stationTimesMap = new TreeMap<>(new Comparator<StationsNum>() {
//            @Override
//            public int compare(StationsNum lhs, StationsNum rhs) {
//                if (lhs.trp != rhs.trp) {
//                    return lhs.trp - rhs.trp;
//                }
//                else if (lhs.line != rhs.line) {
//                    return lhs.line - rhs.line;
//                }
//                else {
//                    return lhs.stn - rhs.stn;
//                }
//            }
//        });
//        for (Pair<StationsNum, Float> entry: stationTimes) {
//            stationTimesMap.put(entry.first, entry.second);
//        }
//        for (Line line: lines) {
//            TRP.TRP_line trp_line = mapData.transports.getLine(line.trpNum, line.lineNum);
//            int stationCount = trp_line.Stations.length;
//            for (int stn = 0; stn < stationCount; stn++) {
//                StationsNum stationsNum = new StationsNum(line.trpNum, line.lineNum, stn);
//                Float stationTime = stationTimesMap.get(stationsNum);
//                if (stationTime != null) {
//                    line.setStationTime(stn, stationTime);
//                } else {
//                    line.setStationTime(stn, -1);
//                }
//            }
//        }
        for (int i = 0; i < stationNums.length; i++) {
            StationsNum num = stationNums[i];
            Float time = stationTimes[i];
            Line line = getLine(num.trp, num.line);
            if (line != null) {
                line.setStationTime(num.stn, time);
            }
        }
    }

    public void clearStationTimes() {
        for (Line line: lines) {
            line.clearStationTimes();
        }
    }

    private void drawStartStation(Canvas canvas, Paint p) {
        StationsNum startStation = mapData.routingState.getStart();
        if (startStation == null)
            return;

        Line line = getLine(startStation.trp, startStation.line);
        if (line == null)
            return;

        PointF coord = line.getCoord(startStation.stn);
        if (ExtPointF.isNull(coord))
            return;

        p.setARGB(255, 10, 133, 26);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(coord.x, coord.y, parameters.StationRadius, p);
        p.setARGB(255, 240, 40, 200);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(parameters.StationRadius/2.5f);
        canvas.drawCircle(coord.x, coord.y, parameters.StationRadius*0.875f, p);
        line.drawText(canvas, startStation.stn);
    }

    private void drawEndStation(Canvas canvas, Paint p) {
        StationsNum endStation = mapData.routingState.getEnd();
        if (endStation == null)
            return;

        Line line = getLine(endStation.trp, endStation.line);
        if (line == null)
            return;

        PointF coord = line.getCoord(endStation.stn);
        if (ExtPointF.isNull(coord))
            return;

        p.setARGB(255, 11, 5, 203);
        p.setStyle(Paint.Style.FILL);
        canvas.drawCircle(coord.x, coord.y, parameters.StationRadius, p);
        p.setARGB(255, 240, 40, 200);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(parameters.StationRadius / 2.5f);
        canvas.drawCircle(coord.x, coord.y, parameters.StationRadius * 0.875f, p);
        line.drawText(canvas, endStation.stn);
    }

    private void drawBlockedStations(Canvas canvas, Paint p) {
        List<StationsNum> blockedStations = mapData.routingState.getBlockedStations();

        p.setStyle(Paint.Style.FILL);
        for (StationsNum station : blockedStations) {
            Line line = getLine(station.trp, station.line);
            if (line == null)
                return;

            PointF coord = line.getCoord(station.stn);
            if (ExtPointF.isNull(coord))
                return;

            float circleRadius = parameters.StationRadius + 2;
            p.setARGB(255, 254, 0, 3);
            canvas.drawCircle(coord.x, coord.y, circleRadius, p);

            float blockWidth = circleRadius * 1.45f;
            float blockHeight = circleRadius * 0.45f;
            p.setARGB(255, 255, 255, 255);
            canvas.drawRect(coord.x - blockWidth / 2, coord.y - blockHeight / 2,
                            coord.x + blockWidth / 2, coord.y + blockHeight / 2, p);
        }
    }
}
