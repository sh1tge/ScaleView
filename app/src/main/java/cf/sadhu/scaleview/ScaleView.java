package cf.sadhu.scaleview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.OverScroller;

/**
 * Created by sadhu on 2017/10/13.
 * 描述:
 */
public class ScaleView extends View {
    private static final String TAG = "ScaleView";
    private boolean mAutoAlign; // 是否自动对齐
    private float mInitialValue; // 初始化的值
    private float mMinValue; // 最小值
    private float mMaxValue; // 最大值
    private float mCurrentValue;//当前值
    private float mGraduationLineMargin; // 刻度线间的间隔
    private float mGraduationStep;// 刻度的步进
    private int mGraduationStepHelper;// 将刻度的步进转换成整数
    private int mGraduationLineColor;//刻度线颜色
    private float mGraduationLineWidth; // 刻度线宽
    private float mGraduationLineHeight; // 刻度线高

    private Drawable mIndicator;
    private float mIndicatorWidth;
    private float mIndicatorHeight;

    private float mTextMargin;

    private Paint mTextPaint;
    private Paint mGraduationPaint;
    private GestureDetectorCompat mGestureDetectorCompat;
    private OverScroller mScroller;
    private float mOffset;
    private float mMinOffset;
    private float mMaxOffset;
    private float mStartX;
    private int mStep;
    private int mModStep;
    private int mIntMinValue;
    private int mIntMaxValue;
    private float mBaselineY;
    private boolean mIsFling;

    public ScaleView(Context context) {
        this(context, null);
    }

    public ScaleView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttrs(attrs);
    }

    private void initAttrs(AttributeSet attrs) {
        TypedArray typedArray = getContext().obtainStyledAttributes(attrs, R.styleable.ScaleView);

        mMinValue = typedArray.getFloat(R.styleable.ScaleView_minValue, 0f);
        mMaxValue = typedArray.getFloat(R.styleable.ScaleView_maxVaule, 100f);
        mInitialValue = typedArray.getFloat(R.styleable.ScaleView_initialValue, (mMinValue + mMaxValue) / 2);
        mCurrentValue = mInitialValue;
        mAutoAlign = typedArray.getBoolean(R.styleable.ScaleView_autoAlign, true);

        mGraduationStep = typedArray.getFloat(R.styleable.ScaleView_graduationStep, 1);
        mGraduationStepHelper = typedArray.getInt(R.styleable.ScaleView_graduationStepHelper, 1);
        mGraduationLineMargin = typedArray.getDimension(R.styleable.ScaleView_graduationLineMargin, dp2px(4));
        mGraduationLineColor = typedArray.getColor(R.styleable.ScaleView_graduationLineColor, ContextCompat.getColor(getContext(), R.color.colorE2));
        mGraduationLineWidth = typedArray.getDimension(R.styleable.ScaleView_graduationLineWidth, dp2px(2));
        mGraduationLineHeight = typedArray.getDimension(R.styleable.ScaleView_graduationLineHeight, dp2px(10));

        mIndicator = typedArray.getDrawable(R.styleable.ScaleView_indicator);
        if (mIndicator == null) {
            mIndicator = ContextCompat.getDrawable(getContext(), R.drawable.bg_indicator);
        }
        mIndicatorWidth = mIndicator.getIntrinsicWidth();
        mIndicatorHeight = mIndicator.getIntrinsicHeight();
        if (mIndicatorWidth == 0 || mIndicatorHeight == 0) {
            mIndicatorWidth = mGraduationLineWidth;
            mIndicatorHeight = mGraduationLineHeight;
        }


        int textColor = typedArray.getColor(R.styleable.ScaleView_graduationTextColor, ContextCompat.getColor(getContext(), R.color.color33));
        float textSize = typedArray.getDimension(R.styleable.ScaleView_graduationTextSize, TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 14f, getContext().getResources().getDisplayMetrics()));
        mTextMargin = typedArray.getDimension(R.styleable.ScaleView_graduationTextMargin, dp2px(4));
        typedArray.recycle();

        initData();
        mTextPaint = new Paint();
        mTextPaint.setAntiAlias(true);
        mTextPaint.setColor(textColor);
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setTextSize(textSize);
        mGraduationPaint = new Paint();
        mGraduationPaint.setAntiAlias(true);
        mGraduationPaint.setColor(mGraduationLineColor);
        mGraduationPaint.setStrokeWidth(mGraduationLineWidth);
        mBaselineY = getPaddingTop() + getPaddingBottom()
                + Math.max(mIndicatorHeight, mGraduationLineHeight)
                + Math.abs(mTextPaint.getFontMetrics().top)
                + mTextMargin;


        mScroller = new OverScroller(getContext());
        mGestureDetectorCompat = new GestureDetectorCompat(getContext(), new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDown(MotionEvent e) {
                mScroller.forceFinished(true);
                return true;
            }

            @Override
            public boolean onScroll(MotionEvent motionEvent, MotionEvent motionEvent1, float distanceX, float distanceY) {
                mOffset += distanceX;
                if (mOffset < mMinOffset) {
                    mOffset = mMinOffset;
                } else if (mOffset > mMaxOffset) {
                    mOffset = mMaxOffset;
                }
                //Log.i(TAG, "onScroll distanceX: " + distanceX + ";offset:" + mOffset);
                invalidate();
                return true;
            }


            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float velocityX, float velocityY) {
                //Log.i(TAG, "onFling: " + velocityX);
                mScroller.forceFinished(true);
                mIsFling = true;
                mScroller.fling(
                        (int) mOffset,
                        0,
                        (int) -velocityX,
                        (int) velocityY,
                        0,
                        (int) mMaxOffset,
                        0,
                        0);
                // Invalidates to trigger computeScroll()
                ViewCompat.postInvalidateOnAnimation(ScaleView.this);
                return true;
            }
        });
    }

    private void initData() {
        // 转化为整数的步进
        mStep = (int) (mGraduationStep * mGraduationStepHelper);
        // 对modStep取余为0 表示需要画刻度值
        mModStep = mStep * 10;
        // 转化成整数的最小值
        mIntMinValue = (int) (mMinValue * mGraduationStepHelper);
        // 转化成整数的最大值
        mIntMaxValue = (int) (mMaxValue * mGraduationStepHelper);
        // 允许偏移(滑动)的最小值
        mMinOffset = 0;
        // 允许偏移(滑动)的最大值
        mMaxOffset = (mMaxValue - mMinValue) / mGraduationStep * mGraduationLineMargin;
        // 当前的偏移(滑动)值
        mOffset = (mInitialValue - mMinValue) / mGraduationStep * mGraduationLineMargin;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return mGestureDetectorCompat.onTouchEvent(event) || super.onTouchEvent(event);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);


        if (widthMode == MeasureSpec.AT_MOST) {
            // 计算3个刻度的宽度,取最小值
            widthSize = (int) Math.min(widthSize, getPaddingLeft() + getPaddingRight() + 3 * 9 * mGraduationLineMargin + 3 * mGraduationLineWidth + 3 * 9 * mGraduationLineWidth / 3);
        }

        if (heightMode == MeasureSpec.AT_MOST) {
            // 计算最小高度: 包括上下padding 刻度线or指示线的高度 文字的高度 文字的margin
            float minHeight = getPaddingTop() + getPaddingBottom()
                    + Math.max(mIndicatorHeight, mGraduationLineHeight)
                    + (mTextPaint.getFontMetrics().bottom - mTextPaint.getFontMetrics().top)
                    + mTextMargin;
            heightSize = (int) Math.min(heightSize, minHeight);
        }

        setMeasuredDimension(widthSize, heightSize);

    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        drawGraduation(canvas);
        mIndicator.setBounds((int) (getWidth() / 2 - mIndicatorWidth / 2),
                getPaddingTop(),
                (int) (getWidth() / 2 + mIndicatorWidth / 2),
                (int) mIndicatorHeight);
        mIndicator.draw(canvas);
    }


    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            mOffset = mScroller.getCurrX();
            ViewCompat.postInvalidateOnAnimation(this);
            //Log.i(TAG, "computeScroll: " + mOffset);
        } else {
            if (mIsFling) {
                mIsFling = false;
                if (mOffset != 0 && mOffset != mMaxOffset) {
                    findFinalGraduation();
                }
            }
        }
    }

    private void drawGraduation(Canvas canvas) {

        mStartX = getPaddingLeft() + getWidth() / 2 - mOffset;
        int index = 0;
        for (int i = mIntMinValue; i <= mIntMaxValue; i += mStep) {
            if (mStartX + index * mGraduationLineMargin > 0 && mStartX + index * mGraduationLineMargin < getWidth()) {
                if (i % mModStep == 0) {
                    mGraduationPaint.setStrokeWidth(mGraduationLineWidth);
                    canvas.drawLine(mStartX + index * mGraduationLineMargin, getPaddingTop(), mStartX + index * mGraduationLineMargin, mGraduationLineHeight, mGraduationPaint);
                    canvas.drawText(
                            String.valueOf(i / mGraduationStepHelper),
                            mStartX + index * mGraduationLineMargin,
                            mBaselineY,
                            mTextPaint);
                } else {
                    mGraduationPaint.setStrokeWidth(mGraduationLineWidth / 2);
                    canvas.drawLine(mStartX + index * mGraduationLineMargin, getPaddingTop(), mStartX + index * mGraduationLineMargin, mGraduationLineHeight / 2, mGraduationPaint);
                }
            }
            index++;
        }
    }

    /**
     * 寻找最终停留的刻度线
     */
    private void findFinalGraduation() {
        int maxTarget = 0;
        int index = 0;
        for (int i = mIntMinValue; i <= mIntMaxValue; i += mStep) {
            if (mGraduationLineMargin * index > mOffset) {
                maxTarget = index;
                break;
            }
            index++;
        }
        Log.i(TAG, "findFinalGraduation: " + maxTarget);
        float maxTargetOffset = maxTarget * mGraduationLineMargin;
        float minTargetOffset = (maxTarget - 1) * mGraduationLineMargin;
        float startX = mOffset;
        if (Math.abs(maxTargetOffset - mOffset) > Math.abs(minTargetOffset - mOffset)) {
            mOffset = minTargetOffset;
        } else {
            mOffset = maxTargetOffset;
        }
        mScroller.startScroll((int) startX, 0, (int) (mOffset - startX), 0, 200);
        ViewCompat.postInvalidateOnAnimation(this);
    }


    private float dp2px(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getContext().getResources().getDisplayMetrics());
    }

}
