package com.secure.voronoi;

public class Vertex implements Comparable<Vertex>
{
    private Point coordinate;
    private HalfEdge incidentEdge;
    public Vertex(Point coordinate, HalfEdge incidentEdge)
    {
        this.coordinate = coordinate;
        this.incidentEdge = incidentEdge;
    }
    public Vertex(){
        this(null, null);
    }

    public Vertex(Point point){
        this(point, null);
    }

    public Point getCoordinate(){
        return coordinate;
    }

    public HalfEdge getIncident(){
        return incidentEdge;
    }

    public void setCoordinate(Point p){
        coordinate = p;
    }
    public void setIncident(HalfEdge e){
        incidentEdge = e;
    }

    @Override
    public int hashCode() {
        return coordinate.hashCode();
    }

    @Override
    public boolean equals(Object o){
        return this == o;
    }
    @Override
    public int compareTo(Vertex arg0) {
        return this.coordinate.compareTo(arg0.getCoordinate());
    }

    public String toString()
    {
        return coordinate.toString();
    }
}