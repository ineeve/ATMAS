package utils;

public class Coordinates {
	private long x, y;
	
	public Coordinates(long l, long m){
		this.x = l;
		this.y = m;
	}
	
	public static double distance(Coordinates c1, Coordinates c2) {
		return Math.sqrt(Math.pow(c1.x - c2.x, 2) + Math.pow(c1.y - c2.y, 2));
	}
}
