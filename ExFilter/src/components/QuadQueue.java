package components;

import android.graphics.PointF;

import components.Ops;

public class QuadQueue {

	public PointF p4,p3,p2,p1;
	public PointF aQuad,aCubeP3,aCubeP2;
	
	private PointF d2;
	
	public QuadQueue() {
		p1 = new PointF();
		p2 = new PointF();
		p3 = new PointF();
		p4 = new PointF();
		d2 = new PointF();
		aQuad = new PointF();
		aCubeP3 = new PointF();
		aCubeP2 = new PointF();
	}
	
	public void push(PointF pt){
		//shift all points and push new entry
		p4.set(p3);
		p3.set(p2);
		p2.set(p1);
		p1.set(pt);
		
		//shift slope data back and calculate newest value
		d2.set(Ops.slopeVec(p3, p1));
		aCubeP3 = aQuad;
		aQuad = Ops.nxtAnchor(p2, p1, d2);
		aCubeP2 = Ops.preAnchor(p2, p3, d2);
	}
}
