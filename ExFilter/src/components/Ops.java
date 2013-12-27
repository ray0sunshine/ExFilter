package components;

import android.graphics.PointF;

//various custom math operations
public class Ops {
	
	//calculates the distance between 2 points
	public static float dist(PointF p1, PointF p2){
		float dx = p1.x - p2.x;
		float dy = p1.y - p2.y;
		return (float)Math.sqrt(dx*dx + dy*dy);
	}
	
	//calculates the distance between 2 coords
	public static float dist(float x1, float y1, float x2, float y2){
		float dx = x1 - x2;
		float dy = y1 - y2;
		return (float)Math.sqrt(dx*dx + dy*dy);
	}
	
	//returns the square distance to avoid square rooting altogether
	public static float distSq(float x1, float y1, float x2, float y2){
		float dx = x1 - x2;
		float dy = y1 - y2;
		return dx*dx+dy*dy;
	}
	
	//averages the 2 segment vectors pre->cur, cur->nxt and gives the normalized vector
	//2 ways either normalize both sample vectors first, or use vector between pre->nxt
	//note: just using the pre and nxt points is sufficient so we'll use that
	//gets the weighted tangent estimate
	public static PointF slopeVec(PointF pre, PointF nxt){
		float dx = nxt.x - pre.x;
		float dy = nxt.y - pre.y;
		float norm = (float)Math.sqrt(dx*dx + dy*dy);
		if(norm == 0){
			return new PointF();
		}else{
			return new PointF(dx/norm, dy/norm);
		}
	}
	
	//creates the previous anchor point based on a current coordinate and estimated slope
	public static PointF preAnchor(PointF cur, PointF pre, PointF vec){
		float d = -dist(cur,pre)/3;
		return new PointF(cur.x + d*vec.x, cur.y + d*vec.y);
	}
	
	//creates the next anchor point based on a current coordinate and estimated slope
	public static PointF nxtAnchor(PointF cur, PointF nxt, PointF vec){
		float d = dist(cur,nxt)/3;
		return new PointF(cur.x + d*vec.x, cur.y + d*vec.y);
	}
}
