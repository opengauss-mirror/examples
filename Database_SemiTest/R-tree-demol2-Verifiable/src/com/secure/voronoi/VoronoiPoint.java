package com.secure.voronoi;

import com.secure.vtree.VPoint;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VoronoiPoint {
    //    private ArrayList<Point> pointList = new ArrayList<Point>();
    private static double HIGH = 1000000.0;
    private static double WIDTH = 1000000.0;


    public ArrayList<Vertex> generatePolygons(Point cellPoint, ArrayList<Point> pointList){
        ArrayList<Vertex> polygons = new ArrayList<Vertex>();
        ArrayList<Vertex> tmpPolygons = new ArrayList<Vertex>();

        VoronoiSolution solusi3 = new VoronoiSolution(pointList,HIGH,-HIGH,-WIDTH,WIDTH);
        for (Face f : solusi3.solution.faces.values()) {
            if (cellPoint.equals(f.getSite())){
//                System.out.println("点"+f.toString());
                HalfEdge e = f.getOuter();
                if (e==null){
                    System.out.println("为null！！！！！！！！！");
                }
                HalfEdge start = e;
                while(true)
                {
                    if(e.getTwin().getOrigin()!=null) {
//                        System.out.println(e.getOrigin().toString()+" "+e.getTwin().getOrigin().toString());
                        if (polygons.isEmpty()){
                            polygons.add(e.getOrigin());
                            polygons.add(e.getTwin().getOrigin());
                        }else {
                            if (!polygons.get(polygons.size()-1).equals(e.getOrigin())){
                                System.out.println("Error,Conflict!!!");
                            }else {
                                polygons.add(e.getTwin().getOrigin());
                            }
                        }
                    } else
                        continue;
                    if(e.getNext()!=null && e.getNext()!=start)
                        e = e.getNext();
                    else
                        break;
                }
                e = start;
                while(true)
                {
                    if(e.getTwin().getOrigin()!=null) {
//                        System.out.println(e.getOrigin().toString()+" "+e.getTwin().getOrigin().toString());
                        if ((!isInPolugon(polygons,e.getOrigin()))||(!isInPolugon(polygons,e.getTwin().getOrigin()))){
                            tmpPolygons.add(e.getOrigin());
                            tmpPolygons.add(e.getTwin().getOrigin());
                        }
                    }
                    else
                        continue;
                    if(e.getPrev()!=null && e.getPrev()!=start)
                        e = e.getPrev();
                    else
                        break;
                }
            }

        }

        if (!tmpPolygons.isEmpty()){
            while (!tmpPolygons.isEmpty()){
                Vertex tmpVertx1 =  tmpPolygons.get(0);
                tmpPolygons.remove(0);
                Vertex tmpVertx2 =  tmpPolygons.get(0);
                tmpPolygons.remove(0);

                if (tmpVertx1.equals(polygons.get(0))){
                    polygons.add(0,tmpVertx2);
                }else if (tmpVertx1.equals(polygons.get(polygons.size()-1))){
                    polygons.add(tmpVertx2);
                }else if (tmpVertx2.equals(polygons.get(0))){
                    polygons.add(0,tmpVertx1);
                }else if(tmpVertx2.equals(polygons.get(polygons.size()-1))){
                    polygons.add(tmpVertx1);
                }else {
                    System.out.println("都没有！！！");
                    polygons.add(tmpVertx1);
                    polygons.add(tmpVertx2);
                }
            }
        }

        //检查是否有错误的点
        ArrayList<Vertex> newPolygons = new ArrayList<Vertex>();


        if (polygons.size()>2){
            newPolygons.add(polygons.get(0));
            newPolygons.add(polygons.get(1));
            for (int i=2;i<polygons.size();i++){
                Point vt0 = newPolygons.get(newPolygons.size()-2).getCoordinate();
                Point vt1 = newPolygons.get(newPolygons.size()-1).getCoordinate();

                int fg1 = checkDirection(vt1,vt0,cellPoint);

                Point vt2 = polygons.get(i).getCoordinate();

                int fg2 = checkDirection(vt2,vt1,cellPoint);

                if (fg1!=fg2){
                    double dist1 = getDistFromP2L(cellPoint.getX(),cellPoint.getY(),
                                                  vt1.getX(),vt1.getY(),
                                                    vt0.getX(),vt0.getY());
                    double dist2 = getDistFromP2L(cellPoint.getX(),cellPoint.getY(),
                            vt2.getX(),vt2.getY(),
                            vt1.getX(),vt1.getY());
                    if (dist1>dist2){
//                        polygons.remove(vt0);
                        newPolygons.remove(newPolygons.size()-2);
                        newPolygons.add(polygons.get(i));
                    }
                }else{
                    newPolygons.add(polygons.get(i));
                }
            }
        }else{
            newPolygons = polygons;
        }

        return newPolygons;
    }

    double getDistFromP2L(double px, double py, double p1x, double p1y, double p2x, double p2y)

    {
        double y2_y1 = p2y - p1y;

        double x2_x1 = p2x - p1x;

//        if (fabs(y2_y1) < EOPS && fabs(y2_y1) < EOPS) {
//            return 0.0;
//
//        }

        return fabs(y2_y1 * (px - p1x) -

                x2_x1 * (py - p1y)) / Math.sqrt(y2_y1 * y2_y1 + x2_x1 * x2_x1);

    }
    public double fabs(double a){
        return (a<0)?-a : a;
    }

    public boolean isInPolugon(ArrayList<Vertex> polygons,Vertex e){
        boolean flag = false;
        for (Vertex v: polygons){
            if (e.equals(v)){
                flag = true;
                break;
            }
        }
        return flag;
    }

    public Point getIntersectPoint(Point p1, Point p2,
                                                      Point p3, Point p4){

        double A1=p1.getY()-p2.getY();
        double B1=p2.getX()-p1.getX();
        double C1=A1*p1.getX()+B1*p1.getY();

        double A2=p3.getY()-p4.getY();
        double B2=p4.getX()-p3.getX();
        double C2=A2*p3.getX()+B2*p3.getY();

        double det_k=A1*B2-A2*B1;

        if(Math.abs(det_k)<0.00001){
            return null;
        }

        double a=B2/det_k;
        double b=-1*B1/det_k;
        double c=-1*A2/det_k;
        double d=A1/det_k;

        double x=a*C1+b*C2;
        double y=c*C1+d*C2;

        return  new Point(x,y);
    }


    public ArrayList<Vertex> cellWithPolygons
            (Point cell,ArrayList<Vertex> verticesArray){


        ArrayList<Vertex> newVertex = new ArrayList<Vertex>();
        ArrayList<Vertex> newdoubleVertex = new ArrayList<Vertex>();

        if (!verticesArray.get(0).equals(verticesArray.get(verticesArray.size()-1))){
            Vertex v0 = verticesArray.get(0);
            Vertex v1 = verticesArray.get(verticesArray.size()-1);
            if ((v0.getCoordinate().getX()==0&&v1.getCoordinate().getX()==0)
                  ||(v0.getCoordinate().getY()==0&&v1.getCoordinate().getY()==0)
                    ||(v0.getCoordinate().getY()==-HIGH&&v1.getCoordinate().getY()==-HIGH)
                  ||(v0.getCoordinate().getX()==WIDTH&&v1.getCoordinate().getX()==WIDTH)){
                verticesArray.add(v0);
            }else{
                Vertex L1 = new Vertex(new Point(0,0));
                Vertex L2 = new Vertex(new Point(WIDTH,0));
                Vertex L3 = new Vertex(new Point(WIDTH,-HIGH));
                Vertex L4 = new Vertex(new Point(0,-HIGH));
                if (v1.getCoordinate().getX()==WIDTH&&(v1.getCoordinate().getY()!=0||v1.getCoordinate().getY()!=-HIGH)){
                    int flag = checkDirection(v1.getCoordinate(),verticesArray.get(verticesArray.size()-2).getCoordinate(),cell);
                    if (checkDirection(L3.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L3);
                        if (v0.getCoordinate().getY()==-HIGH){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L4);

                            if (v0.getCoordinate().getX()==0){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L1);
                                verticesArray.add(v0);
                            }
                        }
                    }
                    if (checkDirection(L2.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L2);
                        if (v0.getCoordinate().getY()==0){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L1);

                            if (v0.getCoordinate().getX()==0){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L4);
                                verticesArray.add(v0);
                            }
                        }
                    }
                }

                if (v1.getCoordinate().getY()==-HIGH&&(v1.getCoordinate().getX()!=0||v1.getCoordinate().getX()!=WIDTH)){
                    int flag = checkDirection(v1.getCoordinate(),verticesArray.get(verticesArray.size()-2).getCoordinate(),cell);
                    if (checkDirection(L4.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L4);
                        if (v0.getCoordinate().getX()==-HIGH){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L1);

                            if (v0.getCoordinate().getY()==0){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L2);
                                verticesArray.add(v0);
                            }
                        }
                    }
                    if (checkDirection(L3.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L3);
                        if (v0.getCoordinate().getX()==WIDTH){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L2);

                            if (v0.getCoordinate().getY()==0){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L1);
                                verticesArray.add(v0);
                            }
                        }
                    }
                }

                if (v1.getCoordinate().getX()==0&&(v1.getCoordinate().getY()!=0||v1.getCoordinate().getX()!=-HIGH)){
                    int flag = checkDirection(v1.getCoordinate(),verticesArray.get(verticesArray.size()-2).getCoordinate(),cell);
                    if (checkDirection(L1.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L1);
                        if (v0.getCoordinate().getY()==0){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L2);

                            if (v0.getCoordinate().getX()==WIDTH){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L3);
                                verticesArray.add(v0);
                            }
                        }
                    }
                    if (checkDirection(L4.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L4);
                        if (v0.getCoordinate().getY()==-HIGH){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L3);

                            if (v0.getCoordinate().getX()==WIDTH){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L2);
                                verticesArray.add(v0);
                            }
                        }
                    }
                }

                if (v1.getCoordinate().getY()==0&&(v1.getCoordinate().getY()!=0||v1.getCoordinate().getX()!=-HIGH)){
                    int flag = checkDirection(v1.getCoordinate(),verticesArray.get(verticesArray.size()-2).getCoordinate(),cell);
                    if (checkDirection(L2.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L2);
                        if (v0.getCoordinate().getX()==WIDTH){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L3);

                            if (v0.getCoordinate().getY()==-HIGH){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L4);
                                verticesArray.add(v0);
                            }
                        }
                    }
                    if (checkDirection(L1.getCoordinate(),v1.getCoordinate(),cell)==flag){
                        verticesArray.add(L1);
                        if (v0.getCoordinate().getX()==0){
                            verticesArray.add(v0);
                        }else{
                            verticesArray.add(L4);

                            if (v0.getCoordinate().getY()==-HIGH){
                                verticesArray.add(v0);
                            }else{
                                verticesArray.add(L3);
                                verticesArray.add(v0);
                            }
                        }
                    }
                }
            }



        }

//        Vertex v0  = verticesArray.get(0);
//        for(int i = 1;i < verticesArray.size(); i=i+1){
//            Vertex v1  = verticesArray.get(i);
//
//            if (v0.getCoordinate().getY()>0&&v1.getCoordinate().getY()<=0){
//                Point sectPoint = getIntersectPoint(v0.getCoordinate(),v1.getCoordinate(),
//                        new Point(0,0),new Point(WIDTH,0));
//                Vertex newV = new Vertex(sectPoint);
//                newVertex.add(newV);
//            }else if (v0.getCoordinate().getY()<=0&&v1.getCoordinate().getY()<=0){
//                newVertex.add(v0);
//            }else if (v0.getCoordinate().getY()<=0&&v1.getCoordinate().getY()>0){
//                Point sectPoint = getIntersectPoint(v0.getCoordinate(),v1.getCoordinate(),
//                        new Point(0,0),new Point(WIDTH,0));
//                Vertex newV = new Vertex(sectPoint);
//                newVertex.add(newV);
//            }
//            v0 = v1;
//        }
//        newVertex.add(v0);
//
//        map.put(cell,newVertex);

        for(int i = 0;i < verticesArray.size()-1; i=i+1){
            Vertex v0 =verticesArray.get(i);
            int k = (i+1);
            Vertex v1 =verticesArray.get(k);
            if (v0.getCoordinate().getY()>0&&v1.getCoordinate().getY()<=0) {
                Point sectPoint = getIntersectPoint(v0.getCoordinate(),v1.getCoordinate(),
                        new Point(0,0),new Point(WIDTH,0));
                Vertex newV = new Vertex(sectPoint);
                newVertex.add(newV);
            }
            if (v0.getCoordinate().getY()<=0&&v1.getCoordinate().getY()<=0){
                newVertex.add(v0);
            }
            if (v0.getCoordinate().getY()<=0&&v1.getCoordinate().getY()>0){
                Point sectPoint = getIntersectPoint(v0.getCoordinate(),v1.getCoordinate(),
                        new Point(0,0),new Point(WIDTH,0));
                Vertex newV = new Vertex(sectPoint);
                newVertex.add(v0);
                newVertex.add(newV);
            }

        }

        for(int i = 0;i < newVertex.size(); i=i+1) {
            Vertex v0 = newVertex.get(i);
            int k = (i + 1)%newVertex.size();
            Vertex v1 = newVertex.get(k);
            if (v0.getCoordinate().getX() < 0 && v1.getCoordinate().getX() >= 0) {
                Point sectPoint = getIntersectPoint(v0.getCoordinate(), v1.getCoordinate(),
                        new Point(0, 0), new Point(0, -HIGH));
                Vertex newV = new Vertex(sectPoint);
                newdoubleVertex.add(newV);
            }
            if (v0.getCoordinate().getX() >= 0 && v1.getCoordinate().getX() >= 0) {
                newdoubleVertex.add(v0);
            }
            if (v0.getCoordinate().getX() >= 0 && v1.getCoordinate().getX() < 0) {
                Point sectPoint = getIntersectPoint(v0.getCoordinate(), v1.getCoordinate(),
                        new Point(0, 0), new Point(0, -HIGH));
                Vertex newV = new Vertex(sectPoint);
                newdoubleVertex.add(v0);
                newdoubleVertex.add(newV);
            }
        }
        return newdoubleVertex;
    }



    public int checkDirection(Point p1, Point p2,
                                  Point p3) {
        Point a = new Point(p1.getX() - p2.getX(), p1.getY() - p2.getY());
        Point b = new Point(p3.getX() - p2.getX(), p3.getY() - p2.getY());
        double z = a.getX() * b.getY() - a.getY() * b.getX();
        if (z > 0) {
            return 1;
        } else if (z == 0) {
//            if (a.getX() < 0){
//                return 1;
//            }else
//            {
//                return 0;
//            }
            return -1;
        } else{
            return 0;
        }
    }

    public ArrayList<Vertex> generateCellWithPolygons
            (Point cell, ArrayList<Point> pointList){
        ArrayList<Vertex> vertexAL =  generatePolygons(cell,pointList);
        return cellWithPolygons_new(cell,vertexAL);
    }

    private ArrayList<Vertex> cellWithPolygons_new(Point cell, ArrayList<Vertex> vertexAL) {
        ArrayList<Vertex> newVertexAl = vertexAL;

        if (!((vertexAL.get(0).getCoordinate().getX()==vertexAL.get(vertexAL.size()-1).getCoordinate().getX())
                &&(vertexAL.get(0).getCoordinate().getY()==vertexAL.get(vertexAL.size()-1).getCoordinate().getY()))){

            if((!(vertexAL.get(0).getCoordinate().getX()==-WIDTH&&-WIDTH==vertexAL.get(vertexAL.size()-1).getCoordinate().getX()))&&
                    (!(vertexAL.get(0).getCoordinate().getX()==WIDTH&&WIDTH==vertexAL.get(vertexAL.size()-1).getCoordinate().getX()))&&
                    (!(vertexAL.get(0).getCoordinate().getY()==HIGH&&HIGH==vertexAL.get(vertexAL.size()-1).getCoordinate().getY()))&&
                    (!(vertexAL.get(0).getCoordinate().getY()==-HIGH&&-HIGH==vertexAL.get(vertexAL.size()-1).getCoordinate().getY()))) {

                Point pt0 = vertexAL.get(vertexAL.size() - 2).getCoordinate();
                Point pt1 = vertexAL.get(vertexAL.size() - 1).getCoordinate();

                int fg = checkDirection(pt1, pt0, cell);
                if (pt1.getY() >= 0 || pt1.getY() == -HIGH) {
                    if (fg == checkDirection(new Point(pt1.getX() + 1, pt1.getY()), pt1, cell)) {
                        newVertexAl.add(new Vertex(new Point(WIDTH, pt1.getY())));
//                    return newVertexAl;
                    }
                    if (fg == checkDirection(new Point(pt1.getX() - 1, pt1.getY()), pt1, cell)) {
                        newVertexAl.add(new Vertex(new Point(-WIDTH, pt1.getY())));
//                    return newVertexAl;
                    }
                }

                if (pt1.getX() <= 0 || pt1.getX() == WIDTH) {
                    if (fg == checkDirection(new Point(pt1.getX(), pt1.getY() + 1), pt1, cell)) {
                        newVertexAl.add(new Vertex(new Point(pt1.getX(), HIGH)));
//                    return newVertexAl;
                    }
                    if (fg == checkDirection(new Point(pt1.getX(), pt1.getY() - 1), pt1, cell)) {
                        newVertexAl.add(new Vertex(new Point(pt1.getX(), -HIGH)));
//                    return newVertexAl;
                    }
                }

                Point qpt0 = vertexAL.get(1).getCoordinate();
                Point qpt1 = vertexAL.get(0).getCoordinate();

                int qfg = checkDirection(qpt1, qpt0, cell);
                if (qpt1.getY() >= 0 || qpt1.getY() == -HIGH) {
                    if (qfg == checkDirection(new Point(qpt1.getX() + 1, qpt1.getY()), qpt1, cell)) {
                        newVertexAl.add(0,new Vertex(new Point(WIDTH, qpt1.getY())));
//                    return newVertexAl;
                    }
                    if (qfg == checkDirection(new Point(qpt1.getX() - 1, qpt1.getY()), qpt1, cell)) {
                        newVertexAl.add(0,new Vertex(new Point(-WIDTH, qpt1.getY())));
//                    return newVertexAl;
                    }
                }

                if (qpt1.getX() <= 0 || qpt1.getX() == WIDTH) {
                    if (qfg == checkDirection(new Point(qpt1.getX(), qpt1.getY() + 1), qpt1, cell)) {
                        newVertexAl.add(0,new Vertex(new Point(qpt1.getX(), HIGH)));
//                    return newVertexAl;
                    }
                    if (qfg == checkDirection(new Point(qpt1.getX(), qpt1.getY() - 1), qpt1, cell)) {
                        newVertexAl.add(0,new Vertex(new Point(qpt1.getX(), -HIGH)));
//                    return newVertexAl;
                    }
                }
            }
            if ((newVertexAl.get(0).getCoordinate().getY()==-HIGH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getX()==WIDTH)||
                    (newVertexAl.get(0).getCoordinate().getX()==WIDTH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getY()==-HIGH)){
                newVertexAl.add(new Vertex(new Point(WIDTH, -HIGH)));
            }

            if ((newVertexAl.get(0).getCoordinate().getY()==HIGH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getX()==WIDTH)||
                    (newVertexAl.get(0).getCoordinate().getX()==WIDTH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getY()==HIGH)){
                newVertexAl.add(new Vertex(new Point(WIDTH, HIGH)));
            }

            if ((newVertexAl.get(0).getCoordinate().getY()==-HIGH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getX()==-WIDTH)||
                    (newVertexAl.get(0).getCoordinate().getX()==-WIDTH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getY()==-HIGH)){
                newVertexAl.add(new Vertex(new Point(-WIDTH, -HIGH)));
            }

            if ((newVertexAl.get(0).getCoordinate().getY()==HIGH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getX()==-WIDTH)||
                    (newVertexAl.get(0).getCoordinate().getX()==-WIDTH&&newVertexAl.get(newVertexAl.size()-1).getCoordinate().getY()==HIGH)){
                newVertexAl.add(new Vertex(new Point(-WIDTH, HIGH)));
            }

        }else{
            newVertexAl.remove(vertexAL.size()-1);
        }


        return newVertexAl;

    }

    public static void main(String[] args) {
        ArrayList<Point> pointList = new ArrayList<Point>();
//        pointList.add(new Point(0,-18));
//        pointList.add(new Point(38,-17));
//        pointList.add(new Point(34,-24));
//        pointList.add(new Point(24,-20));

//          pointList.add(new Point(30,0));
//          pointList.add(new Point(34,-24));
//          pointList.add(new Point(38,0));

        pointList.add(new Point(0,-18));
        pointList.add(new Point(24,-20));
//        pointList.add(new Point(16,-18));

        VoronoiPoint VP = new VoronoiPoint();
       ArrayList<Vertex> xx =
                VP.generateCellWithPolygons(new Point(24,-20),pointList);
        int u=VP.checkDirection(new Point(4,2),new Point(1,2),new Point(5,1));

        System.out.println("距离是："+VP.getDistFromP2L(1,0,4,3,4,4));
        Point p1 = new Point(100,-2.419);
        Point p2 = new Point(13.7840, -2.4190);
        Point p3 = new Point(35,25);

        int fg1 = VP.checkDirection(p2,p1,p3);

        Point p4 = new Point(93, 100);
        int fg2 = VP.checkDirection(p4,p2,p3);

        Point p5 = new Point(100, 100);
        int fg3 = VP.checkDirection(p5,p4,p3);

        int fg4 = VP.checkDirection(p1,p5,p3);

        System.out.println(">>>>>>>>>>");
        double x1 = 4181.0;
        double y1 = 10000.0;
        double x2 = -4152.0;
        double y2 = -10000.0;

        double A= y2-y1;
        double B = x1-x2;
        double C = x2*y1-x1*y2;

        double x = 10;
        double y=(-C-A*x)/B;
        System.out.println(">>>>>>>>>>");
    }


}
