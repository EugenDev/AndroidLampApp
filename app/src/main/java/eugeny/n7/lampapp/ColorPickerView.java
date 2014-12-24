package eugeny.n7.lampapp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

public class ColorPickerView extends View {

    private final String TAG = "ColorPicker";
    private final int TRACKER_STROKE_WIDTH = 8;

    public ColorPickerView(Context context) {
        super(context);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ColorPickerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private int mCurrentColor;

    private int GetCurrentColor(float x){
        float g = x * 360f / gradientRect.width();
        return Color.HSVToColor(new float [] {g, 1f, 1f});
    }

    public void setColor(int color){
        float[] res = new float[3];
        Color.RGBToHSV(Color.red(color),
                Color.green(color),
                Color.blue(color),
                res);
        float x = res[0] * gradientRect.width() / 360f;
        trackerRect.offsetTo(x, trackerRect.top);
        invalidate();
    }

    private void init() {

        /*this.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                int action = MotionEvent.

                switch (action) {

                    case (MotionEvent.ACTION_MOVE):
                    case (MotionEvent.ACTION_DOWN):
                    case (MotionEvent.ACTION_UP):
                        float x = event.getX();
                        if(TRACKER_STROKE_WIDTH < x && x < mWidth - 4 * TRACKER_STROKE_WIDTH) {
                            trackerRect.offsetTo(x, trackerRect.top);
                            int newColor = GetCurrentColor(x);
                            if (newColor != mCurrentColor) {
                                invalidate();
                                mCurrentColor = newColor;
                                OnColorChanged(mCurrentColor);
                            }
                        }
                        break;
                }
                return true;
            }
        });*/

        generateGradientColors();
        initPaints();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            //TODO: Нету Move!!
            case (MotionEvent.ACTION_MOVE):
            case (MotionEvent.ACTION_DOWN):
            case (MotionEvent.ACTION_UP):
                float x = event.getX();
                if(TRACKER_STROKE_WIDTH < x && x < mWidth - 4 * TRACKER_STROKE_WIDTH) {
                    trackerRect.offsetTo(x, trackerRect.top);
                    int newColor = GetCurrentColor(x);
                    if (newColor != mCurrentColor) {
                        invalidate();
                        mCurrentColor = newColor;
                        OnColorChanged(mCurrentColor);
                    }
                }
                break;
        }
        return super.onTouchEvent(event);
    }

    private int mWidth;
    private int mHeight;

    private Rect gradientRect;
    private RectF trackerRect;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        mWidth = MeasureSpec.getSize(widthMeasureSpec);
        mHeight = mWidth / 10;

        if(gradientRect == null) {
            gradientRect = new Rect(0, 0, mWidth, mHeight);
            gradientRect.inset(TRACKER_STROKE_WIDTH, TRACKER_STROKE_WIDTH);
        }

        if(trackerRect == null) {
            trackerRect = new RectF(0, 0, TRACKER_STROKE_WIDTH * 4, mHeight);
            trackerRect.inset(TRACKER_STROKE_WIDTH / 2, TRACKER_STROKE_WIDTH / 2);
        }

        setMeasuredDimension(mWidth, mHeight);
    }

    private int[] mColors;
    private void generateGradientColors() {
        int HSV_RANGE = 361;
        mColors = new int[HSV_RANGE];

        for(int i = 0; i < HSV_RANGE; i++) {
            mColors[i] = Color.HSVToColor(new float[] {i, 1f, 1f});
        }
    }

    private Paint mGradientPaint;
    private Shader scaleGradientShader;

    private Paint mTrackerPaint;
    private Shader trackerGradientShader;

    private void initPaints(){
        mGradientPaint = new Paint();

        mTrackerPaint = new Paint();
        mTrackerPaint.setStyle(Paint.Style.STROKE);
        mTrackerPaint.setStrokeWidth(8);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //Выполниться только единожды, не будет вызывать проблем с производительностью
        if(scaleGradientShader == null) {
            scaleGradientShader = new LinearGradient(0, 0, mWidth, mHeight, mColors, null, Shader.TileMode.CLAMP);
            mGradientPaint.setShader(scaleGradientShader);
        }

        if(trackerGradientShader == null){
            trackerGradientShader = new LinearGradient(0, 0, 0, mHeight, Color.LTGRAY, Color.GRAY, Shader.TileMode.CLAMP);
            mTrackerPaint.setShader(trackerGradientShader);
        }

        canvas.drawRect(gradientRect, mGradientPaint);
        canvas.drawRoundRect(trackerRect, 8, 8, mTrackerPaint);
    }

    private OnColorChangedListener mListener;

    private void OnColorChanged(int color){
        if(mListener != null){
            mListener.onColorChanged(color);
        }
    }

    public void setOnColorChangedListener(OnColorChangedListener listener){
        mListener = listener;
    }

    public interface OnColorChangedListener {
        public void onColorChanged(int color);
    }
}