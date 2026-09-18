package com.fgmachines.rck;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/** Tiny dependency-free power-history chart. */
public final class HistorySparklineView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private List<HistoryStore.PowerPoint> points = new ArrayList<>();

    public HistorySparklineView(Context context) { this(context, null); }
    public HistorySparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(4f);
        linePaint.setColor(0xFF55F29A);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setStrokeWidth(1f);
        gridPaint.setColor(0x334FA4D8);
    }

    public void setPoints(List<HistoryStore.PowerPoint> values) {
        points = values == null ? new ArrayList<>() : new ArrayList<>(values);
        invalidate();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width = getWidth();
        float height = getHeight();
        if (width <= 0 || height <= 0) return;

        for (int i = 1; i <= 3; i++) {
            float y = height * i / 4f;
            canvas.drawLine(0, y, width, y, gridPaint);
        }
        if (points.size() < 2) return;

        double max = 1.0;
        for (HistoryStore.PowerPoint p : points) max = Math.max(max, p.powerW);
        float prevX = 0;
        float prevY = (float) (height - (points.get(0).powerW / max) * height);
        for (int i = 1; i < points.size(); i++) {
            float x = width * i / (points.size() - 1f);
            float y = (float) (height - (points.get(i).powerW / max) * height);
            canvas.drawLine(prevX, prevY, x, y, linePaint);
            prevX = x; prevY = y;
        }
    }
}
