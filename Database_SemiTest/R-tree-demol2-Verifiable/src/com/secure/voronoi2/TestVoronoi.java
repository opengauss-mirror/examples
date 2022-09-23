package com.secure.voronoi2;

import java.util.HashSet;
import java.util.List;

public class TestVoronoi {
    private static int initialSize = 100000000;     // Size of initial triangle
    private Triangulation dt;                   // Delaunay triangulation
    private Triangle initialTriangle;           // Initial triangle

    public TestVoronoi() {
        initialTriangle = new Triangle(
                new Pnt(-initialSize, -initialSize),
                new Pnt( initialSize, -initialSize),
                new Pnt(           0,  initialSize));
        dt = new Triangulation(initialTriangle);
    }

    public Triangulation getDt() {
        return dt;
    }

    public void setDt(Triangulation dt) {
        this.dt = dt;
    }

    public Triangle getInitialTriangle() {
        return initialTriangle;
    }

    public void setInitialTriangle(Triangle initialTriangle) {
        this.initialTriangle = initialTriangle;
    }

    public static void main(String[] args) {
        TestVoronoi test = new TestVoronoi();
        Pnt point1 = new Pnt(0,18);
        Pnt point2 = new Pnt(24,20);
        Pnt point3 = new Pnt(38,17);
        Pnt point4 = new Pnt(34,24);

        test.getDt().delaunayPlace(point1);
        test.getDt().delaunayPlace(point2);
        test.getDt().delaunayPlace(point3);
        test.getDt().delaunayPlace(point4);

        Pnt[] vertices = null;
        HashSet<Pnt> done = new HashSet<Pnt>(test.getInitialTriangle());
        for (Triangle triangle : test.getDt())
            for (Pnt site: triangle) {
                if (done.contains(site)) continue;
                done.add(site);
                List<Triangle> list = test.getDt().surroundingTriangles(site, triangle);
                vertices = new Pnt[list.size()];
                int i = 0;
                for (Triangle tri: list)
                    vertices[i++] = tri.getCircumcenter();
            }
    }
}
