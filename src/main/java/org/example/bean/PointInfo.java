package org.example.bean;

import java.awt.*;

public class PointInfo implements Comparable<PointInfo> {
    public Point point;
    public double geoX;
    public double geoY;
    public long timestamp;

    public String markText = "";

    public boolean hasMark = false;

    public PointInfo(Point point, double geoX, double geoY, long timestamp) {
        this.point = point;
        this.geoX = geoX;
        this.geoY = geoY;
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(PointInfo o) {
        return Long.compare(this.timestamp, o.getTimestamp());
    }

    public long getTimestamp() {
        return timestamp;
    }
}
