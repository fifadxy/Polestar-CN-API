package io.github.polestarcn.companion;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/** Generic car schematic, not extracted art or OEM imagery. */
public final class VehicleDiagramView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    public VehicleDiagramView(Context context) { super(context); setContentDescription("车辆结构示意，模拟状态"); }
    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int w = MeasureSpec.getSize(widthSpec);
        setMeasuredDimension(w, (int)(getResources().getDisplayMetrics().density * 170));
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float cx = getWidth()/2f, sy = getHeight()*0.10f, ey = getHeight()*0.90f;
        float half = Math.min(getWidth()*0.26f, getHeight()*0.23f);
        paint.setColor(Color.rgb(33,33,33)); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(3.5f);
        canvas.drawRoundRect(cx-half,sy,cx+half,ey,half*0.42f,half*0.42f,paint);
        canvas.drawLine(cx-half,sy+(ey-sy)*.33f,cx+half,sy+(ey-sy)*.33f,paint);
        canvas.drawLine(cx-half,sy+(ey-sy)*.67f,cx+half,sy+(ey-sy)*.67f,paint);
        paint.setColor(Color.rgb(180,180,180)); paint.setStrokeWidth(2);
        canvas.drawLine(cx,sy+8,cx,ey-8,paint);
        paint.setColor(Color.rgb(20,132,87)); paint.setStyle(Paint.Style.FILL);
        for (float y : new float[]{sy+(ey-sy)*.29f,sy+(ey-sy)*.72f}) {
            canvas.drawCircle(cx-half,y,4,paint); canvas.drawCircle(cx+half,y,4,paint);
        }
    }
}
