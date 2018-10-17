package utils;

public class Coordinates {
	private Integer x, y;
	
	Coordinates(Integer x, Integer y){
		this.x = x;
		this.y = y;
	}
	
	public static double distance(Coordinates c1, Coordinates c2) {
		return Math.sqrt(Math.pow(c1.x - c2.x, 2) + Math.pow(c1.y - c2.y, 2));
	}
}
