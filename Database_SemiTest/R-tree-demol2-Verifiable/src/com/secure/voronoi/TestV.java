package com.secure.voronoi;


import java.util.ArrayList;

public class TestV {

    public static void main(String[] args) {
        ArrayList<Point> pointList = new ArrayList<Point>();
        pointList.add(new Point(0,-18));
        pointList.add(new Point(38,-17));
        pointList.add(new Point(34,-24));
        pointList.add(new Point(24,-20));

        VoronoiSolution solusi3 = new VoronoiSolution(pointList,0,-200,0,322);
//        solusi3.printDCEL();
        for (Face f : solusi3.solution.faces.values()) {
            System.out.println("点"+f.toString());
            HalfEdge e = f.getOuter();
            HalfEdge start = e;
            while(true)
            {
                if(e.getTwin().getOrigin()!=null) {

                    System.out.println(e.getOrigin().toString()+" "+e.getTwin().getOrigin().toString());

                } else
                    continue;
                if(e.getNext()!=null && e.getNext()!=start)e = e.getNext();
                else break;
            }
            e = start;
            while(true)
            {
                if(e.getTwin().getOrigin()!=null) {

                    System.out.println(e.getOrigin().toString()+" "+e.getTwin().getOrigin().toString());
                }

                else continue;
                if(e.getPrev()!=null && e.getPrev()!=start)e = e.getPrev();
                else break;
            }
        }
    }
}
