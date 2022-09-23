package com.secure.voronoi;

import java.util.HashMap;
import java.util.HashSet;


public class DCEL {
	public HashSet<Vertex> vertices;
	public HashSet<HalfEdge> edges;
	public HashMap<Point, Face> faces;
	
	public DCEL(){
		vertices = new HashSet<Vertex>();
		edges = new HashSet<HalfEdge>();
		faces = new HashMap<Point, Face>();
	}
	
	public HashSet<Vertex> getVertices(){
		return vertices;
	}
	
	public HashSet<HalfEdge> getEdges(){
		return edges;
	}
	
	public HashMap<Point,Face> getFaces(){
		return faces;
	}
	
	
}

class HalfEdge
{
	public Vertex origin;
	private HalfEdge twin, next, previous;
	private Face incidentFace;
	
	public HalfEdge(Vertex origin, HalfEdge twin, HalfEdge next, HalfEdge previous, Face incidentFace){
		this.origin = origin;
		this.twin = twin;
		this.next = next;
		this.previous = previous;
		this.incidentFace = incidentFace;
	}
	
	public HalfEdge(){
		this(null, null, null, null, null);
	}
	public String toString()
	{
		return "Origin : "+ origin;
		
	}
	
	public Vertex getOrigin(){
		return origin;
	}
	
	public HalfEdge getTwin(){
		return twin;
	}
	public HalfEdge getNext(){
		return next;
	}
	public HalfEdge getPrev(){
		return previous;
	}
	
	public Face getIncident(){
		return incidentFace;
	}
	
	public void setOrigin(Vertex o){
		origin = o;
	}
	
	public void setTwin(HalfEdge e){
		twin = e;
	}
	
	public void setNext(HalfEdge e){
		next = e;
	}
	public void setPrev(HalfEdge e){
		previous = e;
	}
	public void setIncident(Face f){
		incidentFace = f;
	}
	
	
}
class Face
{
	private HalfEdge outerComponent,innerComponent;
	Point site;
	public Face(Point site, HalfEdge outerComponent, HalfEdge innerComponent){
		this.site = site;
		this.innerComponent = innerComponent;
		this.outerComponent = outerComponent;
	}
	
	public Face(Point p){
		this(p, null, null);
	}
	
	public Face(){
		this (null, null, null);
	}
	
	public String toString()
	{
		return "Center :"+site;
	}
	
	public HalfEdge getOuter(){
		return outerComponent;
	}
	public HalfEdge getInner(){
		return innerComponent;
	}
	
	public Point getSite(){
		return site;
	}
	public void setOuter(HalfEdge e){
		outerComponent = e;
	}
	
	public void setInner(HalfEdge e){
		innerComponent = e;
	}
	
	public void setSite(Point p){
		site = p;
	}
	
	@Override
	public int hashCode() {
		return site.hashCode();
	}
	
	@Override
	public boolean equals(Object o){
		return this == o;
	}
}

