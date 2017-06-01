package com.axiheyhey.android.celllayout;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.DimenRes;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Created by minxizhang on 2017/5/18.
 */

public class CellLayout extends ViewGroup {

    private static final int MIN_COLUMN_COUNT = 2;

    public static final int DEFAULT_COLUMN_COUNT = MIN_COLUMN_COUNT;

    public static final int SHOW_DIVIDER_NONE = 0;
    public static final int SHOW_DIVIDER_START = 1;
    public static final int SHOW_DIVIDER_MIDDLE = 1 << 1;
    public static final int SHOW_DIVIDER_END = 1 << 2;
    public static final int SHOW_DIVIDER_MASK = SHOW_DIVIDER_START | SHOW_DIVIDER_MIDDLE | SHOW_DIVIDER_END;

    @IntDef(flag = true,
            value = {
                    SHOW_DIVIDER_NONE,
                    SHOW_DIVIDER_START,
                    SHOW_DIVIDER_MIDDLE,
                    SHOW_DIVIDER_END
            })
    @Retention(RetentionPolicy.SOURCE)
    @interface DividerMode {
    }

    @IntRange(from = MIN_COLUMN_COUNT)
    private int mColumnCount;
    private int mMeasuredRowCount;

    private int mDividerModeX;
    @ColorInt
    private int mDividerColorX;
    private int mDividerSizeX;

    private int mDividerModeY;
    @ColorInt
    private int mDividerColorY;
    private int mDividerSizeY;

    @NonNull
    private Axis mHorizontalAxis = new Axis();

    @NonNull
    private Axis mVerticalAxis = new Axis();

    @NonNull
    private Paint mPaintX = new Paint();

    @NonNull
    private Paint mPaintY = new Paint();

    public CellLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public CellLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public CellLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.CellLayout, defStyleAttr, 0);

        final int columnCount = typedArray.getInt(R.styleable.CellLayout_cellColumnCount, DEFAULT_COLUMN_COUNT);
        checkArgument(columnCount >= MIN_COLUMN_COUNT);
        mColumnCount = columnCount;

        int dividerMode = typedArray.getInt(R.styleable.CellLayout_showCellDividers, SHOW_DIVIDER_NONE);
        int dividerColor = typedArray.getColor(R.styleable.CellLayout_cellDividerColor, Color.TRANSPARENT);
        int dividerSize = typedArray.getDimensionPixelSize(R.styleable.CellLayout_cellDividerSize, 0);

        mDividerModeX = typedArray.getInt(R.styleable.CellLayout_showCellHorizontalDividers, dividerMode);
        mDividerColorX = typedArray.getColor(R.styleable.CellLayout_cellHorizontalDividerColor, dividerColor);
        mDividerSizeX = typedArray.getDimensionPixelSize(R.styleable.CellLayout_cellHorizontalDividerWidth, dividerSize);
        mPaintX.setColor(mDividerColorX);

        mDividerModeY = typedArray.getInt(R.styleable.CellLayout_showCellVerticalDividers, dividerMode);
        mDividerColorY = typedArray.getColor(R.styleable.CellLayout_cellVerticalDividerColor, dividerColor);
        mDividerSizeY = typedArray.getDimensionPixelSize(R.styleable.CellLayout_cellVerticalDividerHeight, dividerSize);
        mPaintY.setColor(mDividerColorY);

        typedArray.recycle();

        detectWhetherDrawDividers();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mMeasuredRowCount = calculateRowCount();
        if (mMeasuredRowCount < 1) {
            setMeasuredDimension(resolveSizeAndState(getSuggestedMinimumWidth(), widthMeasureSpec, 0),
                    resolveSizeAndState(getSuggestedMinimumHeight(), heightMeasureSpec, 0));
            return;
        }

        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        final int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        final int paddingX = getPaddingLeft() + getPaddingRight();
        final int paddingY = getPaddingTop() + getPaddingBottom();
        final int childCount = getChildCount();
        final int columnCount = mColumnCount;
        final int rowCount = mMeasuredRowCount;

        if (widthMode == MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(widthSize, heightSize);
            mHorizontalAxis.set(getMeasuredWidth(), paddingX, columnCount, mDividerModeX, mDividerSizeX, true);
            mVerticalAxis.set(getMeasuredHeight(), paddingY, rowCount, mDividerModeY, mDividerSizeY, true);
        } else {
            mHorizontalAxis.set(widthSize, paddingX, columnCount, mDividerModeX, mDividerSizeX, false);
            mVerticalAxis.set(heightSize, paddingY, rowCount, mDividerModeY, mDividerSizeY, false);
            int maxChildWidth = 0;
            int maxChildHeight = 0;
            for (int index = 0; index < childCount; index++) {
                View child = getChildAt(index);
                if (child.getVisibility() == View.GONE) {
                    continue;
                }
                ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
                int childWidthMeasureSpec = makeChildMeasureSpec(mHorizontalAxis.getSegmentSizeAt(index),
                        widthMode, layoutParams.width);
                int childHeightMeasureSpec = makeChildMeasureSpec(mVerticalAxis.getSegmentSizeAt(index),
                        heightMode, layoutParams.height);
                child.measure(childWidthMeasureSpec, childHeightMeasureSpec);

                maxChildWidth = Math.max(maxChildWidth, child.getMeasuredWidth());
                maxChildHeight = Math.max(maxChildHeight, child.getMeasuredHeight());
            }
            int width = Math.max(getSuggestedMinimumWidth(), maxChildWidth * columnCount + paddingX);
            int height = Math.max(getSuggestedMinimumHeight(), maxChildHeight * rowCount + paddingY);
            setMeasuredDimension(resolveSizeAndState(width, widthMeasureSpec, 0),
                    resolveSizeAndState(height, heightMeasureSpec, 0));
            mHorizontalAxis.set(getMeasuredWidth(), paddingX, columnCount, mDividerModeX, mDividerSizeX, true);
            mVerticalAxis.set(getMeasuredHeight(), paddingY, rowCount, mDividerModeY, mDividerSizeY, true);
        }

        for (int index = 0, columnIndex = 0, rowIndex = 0; index < childCount; index++, columnIndex++) {
            View child = getChildAt(index);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            if (columnIndex == columnCount) {
                columnIndex = 0;
                rowIndex++;
            }
            int cellWidth = mHorizontalAxis.getSegmentSizeAt(columnIndex);
            int cellHeight = mVerticalAxis.getSegmentSizeAt(rowIndex);
            child.measure(MeasureSpec.makeMeasureSpec(cellWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(cellHeight, MeasureSpec.EXACTLY));
        }
    }

    private int calculateRowCount() {
        int visibleChildCount = 0;
        for (int index = 0, childCount = getChildCount(); index < childCount; index++) {
            View child = getChildAt(index);
            if (child.getVisibility() == View.GONE) {
                continue;
            }
            visibleChildCount++;
        }
        return (int) Math.ceil((float) visibleChildCount / mColumnCount);
    }

    private int makeChildMeasureSpec(int cellSize, int cellMode, int childDimension) {
        int resultSize = 0;
        int resultMode = MeasureSpec.UNSPECIFIED;

        switch (cellMode) {
            case MeasureSpec.EXACTLY:
                resultSize = cellSize;
                resultMode = MeasureSpec.EXACTLY;
                break;
            case MeasureSpec.AT_MOST:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = cellSize;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = cellSize;
                    resultMode = MeasureSpec.AT_MOST;
                }
                break;
            case MeasureSpec.UNSPECIFIED:
                if (childDimension >= 0) {
                    resultSize = childDimension;
                    resultMode = MeasureSpec.EXACTLY;
                } else if (childDimension == LayoutParams.MATCH_PARENT) {
                    resultSize = cellSize;
                    resultMode = MeasureSpec.UNSPECIFIED;
                } else if (childDimension == LayoutParams.WRAP_CONTENT) {
                    resultSize = cellSize;
                    resultMode = MeasureSpec.UNSPECIFIED;
                }
                break;
            default:
        }
        return MeasureSpec.makeMeasureSpec(resultSize, resultMode);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        final int parentLeft = getPaddingLeft();
        final int parentTop = getPaddingTop();
        final int columnCount = mColumnCount;

        int columnIndex = 0;
        int rowIndex = 0;
        int childLeft = parentLeft;
        int childTop = parentTop;

        for (int index = 0, count = getChildCount(); index < count; index++, columnIndex++) {
            final View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }

            final int width = child.getMeasuredWidth();
            final int height = child.getMeasuredHeight();

            if (columnIndex == columnCount) {
                childLeft = parentLeft;
                columnIndex = 0;

                childTop += height;
                if (mVerticalAxis.hasDividerAfter(rowIndex)) {
                    childTop += mDividerSizeY;
                }
                rowIndex++;
            }

            if (columnIndex == 0 && mHorizontalAxis.hasStartedDivider()) {
                childLeft += mDividerSizeX;
            }

            if (columnIndex == 0 && rowIndex == 0 && mVerticalAxis.hasStartedDivider()) {
                childTop += mDividerSizeY;
            }

            child.layout(childLeft, childTop, childLeft + width, childTop + height);
            childLeft += width;
            if (mHorizontalAxis.hasDividerAfter(columnIndex)) {
                childLeft += mDividerSizeX;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (shouldNotDrawDividers()) {
            return;
        }
        final int columnCount = mColumnCount;
        for (int index = 0, count = getChildCount(), columnIndex = 0, rowIndex = 0; index < count;
             index++, columnIndex++) {
            final View child = getChildAt(index);
            if (child.getVisibility() == GONE) {
                continue;
            }
            if (columnIndex == columnCount) {
                columnIndex = 0;
                rowIndex++;
            }

            int left = child.getLeft();
            int top = child.getTop();
            int right = child.getRight();
            int bottom = child.getBottom();

            if (columnIndex == 0 && mHorizontalAxis.hasStartedDivider()) {
                canvas.drawRect(left - mDividerSizeX, top, left, bottom, mPaintX);
                left = left - mDividerSizeX;
            }

            if (mHorizontalAxis.hasDividerAfter(columnIndex)) {
                canvas.drawRect(right, top, right + mDividerSizeX, bottom, mPaintX);
                right = right + mDividerSizeX;
            }

            if (rowIndex == 0 && mVerticalAxis.hasStartedDivider()) {
                canvas.drawRect(left, top - mDividerSizeY, right, top, mPaintY);
            }

            if (mVerticalAxis.hasDividerAfter(rowIndex)) {
                canvas.drawRect(left, bottom, right, bottom + mDividerSizeY, mPaintY);
            }
        }
    }

    public int getColumnCount() {
        return mColumnCount;
    }

    public void setColumnCount(@IntRange(from = 2) int columnCount) {
        if (mColumnCount != columnCount) {
            mColumnCount = columnCount;
            requestLayout();
        }
    }

    public int getMeasuredRowCount() {
        return mMeasuredRowCount;
    }

    @DividerMode
    public int getShowDividerX() {
        return mDividerModeX;
    }

    @DividerMode
    public int getShowDividerY() {
        return mDividerModeY;
    }

    public void setShowDividers(@DividerMode int mode) {
        setShowDividers(mode, mode);
    }

    public void setShowDividers(@DividerMode int modeX, @DividerMode int modeY) {
        boolean isChanged = false;
        if (mDividerModeX != modeX) {
            mDividerModeX = modeX;
            isChanged = true;
        }
        if (mDividerModeY != modeY) {
            mDividerModeY = modeY;
            isChanged = true;
        }
        if (isChanged) {
            detectWhetherDrawDividers();
            requestLayout();
        }
    }

    public void setDividerColorResource(@ColorRes int colorRes) {
        int color = ResourcesCompat.getColor(getResources(), colorRes, null);
        setDividerColor(color);
    }

    public void setDividerColorResource(@ColorRes int colorResX, @ColorRes int colorResY) {
        int colorX = ResourcesCompat.getColor(getResources(), colorResX, null);
        int colorY = ResourcesCompat.getColor(getResources(), colorResY, null);
        setDividerColor(colorX, colorY);
    }

    @ColorInt
    public int getDividerColorX() {
        return mDividerColorX;
    }

    @ColorInt
    public int getDividerColorY() {
        return mDividerColorY;
    }

    public void setDividerColor(@ColorInt int color) {
        setDividerColor(color, color);
    }

    public void setDividerColor(@ColorInt int colorX, @ColorInt int colorY) {
        boolean isChanged = false;
        if (mDividerColorX != colorX) {
            mDividerColorX = colorX;
            mPaintX.setColor(mDividerColorX);
            isChanged = true;
        }
        if (mDividerColorY != colorY) {
            mDividerColorY = colorY;
            mPaintY.setColor(mDividerColorY);
            isChanged = true;
        }
        if (isChanged) {
            detectWhetherDrawDividers();
            invalidate();
        }
    }

    public int getDividerSizeX() {
        return mDividerSizeX;
    }

    public int getDividerSizeY() {
        return mDividerSizeY;
    }

    public void setDividerSize(@DimenRes int size) {
        setDividerSize(size, size);
    }

    public void setDividerSize(@DimenRes int sizeResX, @DimenRes int sizeResY) {
        int sizeX = getResources().getDimensionPixelSize(sizeResX);
        int sizeY = getResources().getDimensionPixelSize(sizeResY);
        boolean isChanged = false;
        if (mDividerSizeX != sizeX) {
            mDividerSizeX = sizeX;
            isChanged = true;
        }
        if (mDividerSizeY != sizeY) {
            mDividerSizeY = sizeY;
            isChanged = true;
        }
        if (isChanged) {
            detectWhetherDrawDividers();
            requestLayout();
        }
    }

    private void detectWhetherDrawDividers() {
        setWillNotDraw(shouldNotDrawDividers());
    }

    private boolean shouldNotDrawDividers() {
        return shouldNotDrawHorizontalDividers() && shouldNotDrawVerticalDividers();
    }

    private boolean shouldNotDrawHorizontalDividers() {
        return mDividerSizeX <= 0 || mDividerColorX == Color.TRANSPARENT
                || (mDividerModeX & SHOW_DIVIDER_MASK) == SHOW_DIVIDER_NONE;
    }

    private boolean shouldNotDrawVerticalDividers() {
        return mDividerSizeY <= 0 || mDividerColorY == Color.TRANSPARENT
                || (mDividerModeY & SHOW_DIVIDER_MASK) == SHOW_DIVIDER_NONE;
    }

    private final static class Axis {

        @IntRange(from = 1)
        private int mCount;
        @DividerMode
        private int mDividerMode;
        private int mDividerSize;

        private int mGeneralSegmentSize;
        private int mLastSegmentSize;

        void set(@IntRange(from = 1) int space,
                 @IntRange(from = 0) int padding,
                 @IntRange(from = 1) int count,
                 @DividerMode int dividerMode,
                 @IntRange(from = 0) int dividerSize,
                 boolean isExact) {
            mCount = count;
            mDividerMode = dividerMode;
            mDividerSize = dividerSize;

            int spaceUsed = 0;
            if (dividerSize > 0 && (mDividerMode & SHOW_DIVIDER_MASK) != SHOW_DIVIDER_NONE) {
                int dividerCount = 0;
                if ((dividerMode & SHOW_DIVIDER_START) == SHOW_DIVIDER_START) {
                    dividerCount += 1;
                }
                if ((dividerMode & SHOW_DIVIDER_MIDDLE) == SHOW_DIVIDER_MIDDLE) {
                    dividerCount += count - 1;
                }
                if ((dividerMode & SHOW_DIVIDER_END) == SHOW_DIVIDER_END) {
                    dividerCount += 1;
                }
                spaceUsed = dividerSize * dividerCount;
            }

            final int spaceSansDividers = space - padding - spaceUsed;
            if (spaceSansDividers > 0) {
                mGeneralSegmentSize = Math.round((float) spaceSansDividers / count);
                if (isExact) {
                    mLastSegmentSize = spaceSansDividers - mGeneralSegmentSize * (count - 1);
                } else {
                    mLastSegmentSize = mGeneralSegmentSize;
                }
            } else {
                mGeneralSegmentSize = 0;
                mLastSegmentSize = 0;
            }
        }

        int getSegmentSizeAt(@IntRange(from = 0) int index) {
            return (index < mCount - 1) ? mGeneralSegmentSize : mLastSegmentSize;
        }

        private boolean hasStartedDivider() {
            return mDividerSize > 0 && (mDividerMode & SHOW_DIVIDER_START) == SHOW_DIVIDER_START;
        }

        private boolean hasDividerAfter(@IntRange(from = 0) int index) {
            if (mDividerSize <= 0) {
                return false;
            }
            if (index < mCount - 1) {
                return (mDividerMode & SHOW_DIVIDER_MIDDLE) == SHOW_DIVIDER_MIDDLE;
            } else {
                return index == mCount - 1 && (mDividerMode & SHOW_DIVIDER_END) == SHOW_DIVIDER_END;
            }
        }
    }
}
