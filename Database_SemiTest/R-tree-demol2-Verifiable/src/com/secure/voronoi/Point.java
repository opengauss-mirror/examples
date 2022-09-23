package com.secure.voronoi;

public class Point implements Comparable<Point> {
	static int hash = 0;
	int hashCode;
	private double x;
	private double y;
	public Point(double x, double y)
	{
		this.hashCode = hash++;
		this.x = x;
		this.y = y;
	}
	@Override
	public int compareTo(Point o) {
		if(this.y==o.y)
		{
			if(this.x - o.x>0) return 1;
			else if(this.x - o.x<0) return -1;
			else return 0;
		}
		else
		{
			if(o.y - this.y>0) return 1;
			else if(o.y - this.y<0) return -1;
			else return 0;
		}
	}

	public double getX()
	{
		return x;
	}
	public double getY()
	{
		return y;
	}

	public void setX(double x)
	{
		this.x = x;
	}
	
	public void setY(double y)
	{
		this.y = y;
	}
	
	public boolean equals(Point o)
	{
		return this.getX()==o.getX() && this.getY()==o.getY();
	}
	public int hashCode()
	{
		return this.hashCode;
	}
	
	public String toString()
	{
		return "("+getX()+","+getY()+")";
	}
	
}
