package org.example.bean;

import java.util.SortedSet;
import java.util.TreeSet;

public class TraceLayer {
    public SortedSet<PointInfo> pointInfos = new TreeSet<>();

    public String colorNane;

    public TraceLayer(String color) {
        this.colorNane = color;
    }
}
