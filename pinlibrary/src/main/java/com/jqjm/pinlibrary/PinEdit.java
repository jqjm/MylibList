package com.jqjm.pinlibrary;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.text.TextUtilsCompat;
import android.support.v4.view.ViewCompat;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import java.util.Locale;

/**
 * edit  密码框lib
 * 作者：JQJM_曹加启
 * 时间：2017-03-21.
 */

@SuppressLint("AppCompatCustomView")
public class PinEdit extends EditText {
    private static final String XML_NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android";

    protected String mMask = null;
    protected StringBuilder mMaskChars = null;
    protected String mSingleCharHint = null;
    protected int mAnimatedType = 0;
    protected float mSpace = 24; //24 dp ，默认行距
    protected float mCharSize;
    protected float mNumChars = 4;
    protected float mTextBottomPadding = 8; //8dp 默认, 行文本的高度
    protected int mMaxLength = 4;
    protected RectF[] mLineCoords;
    protected float[] mCharBottom;
    protected Paint mCharPaint;
    protected Paint mLastCharPaint;
    protected Paint mSingleCharPaint;
    protected Drawable mPinBackground;
    protected Rect mTextHeight = new Rect();

    protected boolean mIsDigitSquare = false;

    protected OnClickListener mClickListener;
    protected OnPinEnteredListener mOnPinEnteredListener = null;

    protected float mLineStroke = 1; //1dp by default
    protected float mLineStrokeSelected = 2; //2dp by default
    protected Paint mLinesPaint;
    protected boolean mAnimate = false;
    protected boolean mHasError = false;
    protected ColorStateList mOriginalTextColors;
    protected int[][] mStates = new int[][]{
            new int[]{android.R.attr.state_selected}, // 选中
            new int[]{android.R.attr.state_active}, // 错误
            new int[]{android.R.attr.state_focused}, // 聚焦
            new int[]{-android.R.attr.state_focused}, // 未聚焦
    };

    protected int[] mColors = new int[]{
            Color.GREEN,
            Color.RED,
            Color.BLACK,
            Color.GRAY
    };

    protected ColorStateList mColorStates = new ColorStateList(mStates, mColors);


    public PinEdit(Context context) {
        super(context);
    }

    public PinEdit(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PinEdit(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }


    public void setMaxLength(final int maxLength) {
        mMaxLength = maxLength;
        mNumChars = maxLength;

        setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});

        setText(null);
        invalidate();
    }

    public void setPinBackgroundDrawable(Drawable drawable) {
        mPinBackground = drawable;

    }

    private void init(Context context, AttributeSet attrs) {
        float multi = context.getResources().getDisplayMetrics().density;
        mLineStroke = multi * mLineStroke;
        mLineStrokeSelected = multi * mLineStrokeSelected;
        mSpace = multi * mSpace; //convert to pixels for our density
        mTextBottomPadding = multi * mTextBottomPadding; //convert to pixels for our density

        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.PinEdit, 0, 0);
        try {
            TypedValue outValue = new TypedValue();
            ta.getValue(R.styleable.PinEdit_pinAnimationType, outValue);
            mAnimatedType = outValue.data;//动画类型 从style中获取后赋值到outvalue中，拿出data 就是客户写的选中值了
            mMask = ta.getString(R.styleable.PinEdit_pinCharacterMask);//面具，就像是密码框中 隐藏的字符
            mSingleCharHint = ta.getString(R.styleable.PinEdit_pinRepeatedHint);
            mLineStroke = ta.getDimension(R.styleable.PinEdit_pinLineStroke, mLineStroke);
            mLineStrokeSelected = ta.getDimension(R.styleable.PinEdit_pinLineStrokeSelected, mLineStrokeSelected);
            mSpace = ta.getDimension(R.styleable.PinEdit_pinCharacterSpacing, mSpace);
            mTextBottomPadding = ta.getDimension(R.styleable.PinEdit_pinTextBottomPadding, mTextBottomPadding);
            mIsDigitSquare = ta.getBoolean(R.styleable.PinEdit_pinBackgroundIsSquare, mIsDigitSquare);
            mPinBackground = ta.getDrawable(R.styleable.PinEdit_pinBackgroundDrawable);
            ColorStateList colors = ta.getColorStateList(R.styleable.PinEdit_pinLineColors);
            if (colors != null) {
                mColorStates = colors;
            }
        } finally {
            ta.recycle();
        }

        mCharPaint = new Paint(getPaint());
        mLastCharPaint = new Paint(getPaint());
        mSingleCharPaint = new Paint(getPaint());
        mLinesPaint = new Paint(getPaint());
        mLinesPaint.setStrokeWidth(mLineStroke);

        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(R.attr.colorControlActivated,
                outValue, true);
        int colorSelected = outValue.data;
        mColors[0] = colorSelected;

        int colorFocused = isInEditMode() ? Color.GRAY : ContextCompat.getColor(context, R.color.pin_normal);
        mColors[1] = colorFocused;

        int colorUnfocused = isInEditMode() ? Color.GRAY : ContextCompat.getColor(context, R.color.pin_normal);
        mColors[2] = colorUnfocused;

        setBackgroundResource(0);

        mMaxLength = attrs.getAttributeIntValue(XML_NAMESPACE_ANDROID, "maxLength", 4);
        mNumChars = mMaxLength;

        //Disable copy paste
        super.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public void onDestroyActionMode(ActionMode mode) {
            }

            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                return false;
            }
        });
        // When tapped, move cursor to end of text.
        super.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelection(getText().length());
                if (mClickListener != null) {
                    mClickListener.onClick(v);
                }
            }
        });

        super.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                setSelection(getText().length());
                return true;
            }
        });

        //如果输入类型是密码设置,没有面具(Mask),使用默认的面具
        if ((getInputType() & InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD && TextUtils.isEmpty(mMask)) {
            mMask = "\u25CF";
        } else if ((getInputType() & InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD && TextUtils.isEmpty(mMask)) {
            mMask = "\u25CF";
        }

        if (!TextUtils.isEmpty(mMask)) {
            mMaskChars = getMaskChars();
        }

        //高度使用的字符,如果有一个背景可拉的
        getPaint().getTextBounds("|", 0, 1, mTextHeight);

        mAnimate = mAnimatedType > -1;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mOriginalTextColors = getTextColors();
        if (mOriginalTextColors != null) {
            mLastCharPaint.setColor(mOriginalTextColors.getDefaultColor());
            mCharPaint.setColor(mOriginalTextColors.getDefaultColor());
            mSingleCharPaint.setColor(getCurrentHintTextColor());
        }
        int availableWidth = getWidth() - ViewCompat.getPaddingEnd(this) - ViewCompat.getPaddingStart(this);
        if (mSpace < 0) {
            mCharSize = (availableWidth / (mNumChars * 2 - 1));
        } else {
            mCharSize = (availableWidth - (mSpace * (mNumChars - 1))) / mNumChars;
        }
        mLineCoords = new RectF[(int) mNumChars];
        mCharBottom = new float[(int) mNumChars];
        int startX;
        int bottom = getHeight() - getPaddingBottom();
        int rtlFlag;
        final boolean isLayoutRtl = TextUtilsCompat.getLayoutDirectionFromLocale(Locale.getDefault()) == ViewCompat.LAYOUT_DIRECTION_RTL;
        if (isLayoutRtl) {
            rtlFlag = -1;
            startX = (int) (getWidth() - ViewCompat.getPaddingStart(this) - mCharSize);
        } else {
            rtlFlag = 1;
            startX = ViewCompat.getPaddingStart(this);
        }
        for (int i = 0; i < mNumChars; i++) {
            mLineCoords[i] = new RectF(startX, bottom, startX + mCharSize, bottom);
            if (mPinBackground != null) {
                if (mIsDigitSquare) {
                    mLineCoords[i].top = getPaddingTop();
                    mLineCoords[i].right = startX + mLineCoords[i].height();
                } else {
                    mLineCoords[i].top -= mTextHeight.height() + mTextBottomPadding * 2;
                }
            }

            if (mSpace < 0) {
                startX += rtlFlag * mCharSize * 2;
            } else {
                startX += rtlFlag * (mCharSize + mSpace);
            }
            mCharBottom[i] = mLineCoords[i].bottom - mTextBottomPadding;
        }
    }

    @Override
    public void setOnClickListener(OnClickListener l) {
        mClickListener = l;
    }

    @Override
    public void setCustomSelectionActionModeCallback(ActionMode.Callback actionModeCallback) {
        throw new RuntimeException("不支持设置setCustomSelectionActionModeCallback()");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        //super.onDraw(canvas);
        CharSequence text = getFullText();
        int textLength = text.length();
        float[] textWidths = new float[textLength];
        getPaint().getTextWidths(text, 0, textLength, textWidths);

        float hintWidth = 0;
        if (mSingleCharHint != null) {
            float[] hintWidths = new float[mSingleCharHint.length()];
            getPaint().getTextWidths(mSingleCharHint, hintWidths);
            for (float i : hintWidths) {
                hintWidth += i;
            }
        }
        for (int i = 0; i < mNumChars; i++) {
            //If a background for the pin characters is specified, it should be behind the characters.
            // 如果指定pin的背景人物,应该是后面的字符。
            if (mPinBackground != null) {
                updateDrawableState(i < textLength, i == textLength);
                mPinBackground.setBounds((int) mLineCoords[i].left, (int) mLineCoords[i].top, (int) mLineCoords[i].right, (int) mLineCoords[i].bottom);
                mPinBackground.draw(canvas);
            }
            float middle = mLineCoords[i].left + mCharSize / 2;
            if (textLength > i) {
                if (!mAnimate || i != textLength - 1) {
                    canvas.drawText(text, i, i + 1, middle - textWidths[i] / 2, mCharBottom[i], mCharPaint);
                } else {
                    canvas.drawText(text, i, i + 1, middle - textWidths[i] / 2, mCharBottom[i], mLastCharPaint);
                }
            } else if (mSingleCharHint != null) {
                canvas.drawText(mSingleCharHint, middle - hintWidth / 2, mCharBottom[i], mSingleCharPaint);
            }
            //线应该是前面的文本(因为这就是我想要)。
            if (mPinBackground == null) {
                updateColorForLines(i <= textLength);
                canvas.drawLine(mLineCoords[i].left, mLineCoords[i].top, mLineCoords[i].right, mLineCoords[i].bottom, mLinesPaint);
            }
        }
    }

    private CharSequence getFullText() {
        if (mMask == null) {
            return getText();
        } else {
            return getMaskChars();
        }
    }

    private StringBuilder getMaskChars() {
        if (mMaskChars == null) {
            mMaskChars = new StringBuilder();
        }
        int textLength = getText().length();
        while (mMaskChars.length() != textLength) {
            if (mMaskChars.length() < textLength) {
                mMaskChars.append(mMask);
            } else {
                mMaskChars.deleteCharAt(mMaskChars.length() - 1);
            }
        }
        return mMaskChars;
    }


    private int getColorForState(int... states) {
        return mColorStates.getColorForState(states, Color.GRAY);
    }

    /**
     * @param hasTextOrIsNext Is the color for a character that has been typed or is
     *                        the next character to be typed?
     */
    protected void updateColorForLines(boolean hasTextOrIsNext) {
        if (mHasError) {
            mLinesPaint.setColor(getColorForState(android.R.attr.state_active));
        } else if (isFocused()) {
            mLinesPaint.setStrokeWidth(mLineStrokeSelected);
            mLinesPaint.setColor(getColorForState(android.R.attr.state_focused));
            if (hasTextOrIsNext) {
                mLinesPaint.setColor(getColorForState(android.R.attr.state_selected));
            }
        } else {
            mLinesPaint.setStrokeWidth(mLineStroke);
            mLinesPaint.setColor(getColorForState(-android.R.attr.state_focused));
        }
    }

    protected void updateDrawableState(boolean hasText, boolean isNext) {
        if (mHasError) {
            mPinBackground.setState(new int[]{android.R.attr.state_active});
        } else if (isFocused()) {
            mPinBackground.setState(new int[]{android.R.attr.state_focused});
            if (isNext) {
                mPinBackground.setState(new int[]{android.R.attr.state_focused, android.R.attr.state_selected});
            } else if (hasText) {
                mPinBackground.setState(new int[]{android.R.attr.state_focused, android.R.attr.state_checked});
            }
        } else {
            mPinBackground.setState(new int[]{-android.R.attr.state_focused});
        }
    }

    public void setError(boolean hasError) {
        mHasError = hasError;
    }

    public boolean isError() {
        return mHasError;
    }

    /**
     * Request focus on this PinEditText
     */
    public void focus() {
        requestFocus();

        // Show keyboard
        InputMethodManager inputMethodManager = (InputMethodManager) getContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.showSoftInput(this, 0);
    }

    @Override
    protected void onTextChanged(CharSequence text, final int start, int lengthBefore, final int lengthAfter) {
        setError(false);
        if (mLineCoords == null || !mAnimate) {
            if (mOnPinEnteredListener != null && text.length() == mMaxLength) {
                mOnPinEnteredListener.onPinEntered(text);
            }
            return;
        }

        if (mAnimatedType == -1) {
            invalidate();
            return;
        }

        if (lengthAfter > lengthBefore) {
            if (mAnimatedType == 0) {
                animatePopIn();
            } else {
                animateBottomUp(text, start);
            }
        }
    }

    private void animatePopIn() {
        ValueAnimator va = ValueAnimator.ofFloat(1, getPaint().getTextSize());
        va.setDuration(200);
        va.setInterpolator(new OvershootInterpolator());
        va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mLastCharPaint.setTextSize((Float) animation.getAnimatedValue());
                PinEdit.this.invalidate();
            }
        });
        if (getText().length() == mMaxLength && mOnPinEnteredListener != null) {
            va.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mOnPinEnteredListener.onPinEntered(getText());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {
                }
            });
        }
        va.start();
    }

    private void animateBottomUp(CharSequence text, final int start) {
        mCharBottom[start] = mLineCoords[start].bottom - mTextBottomPadding;
        ValueAnimator animUp = ValueAnimator.ofFloat(mCharBottom[start] + getPaint().getTextSize(), mCharBottom[start]);
        animUp.setDuration(300);
        animUp.setInterpolator(new OvershootInterpolator());
        animUp.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Float value = (Float) animation.getAnimatedValue();
                mCharBottom[start] = value;
                PinEdit.this.invalidate();
            }
        });

        mLastCharPaint.setAlpha(255);
        ValueAnimator animAlpha = ValueAnimator.ofInt(0, 255);
        animAlpha.setDuration(300);
        animAlpha.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer value = (Integer) animation.getAnimatedValue();
                mLastCharPaint.setAlpha(value);
            }
        });

        AnimatorSet set = new AnimatorSet();
        if (text.length() == mMaxLength && mOnPinEnteredListener != null) {
            set.addListener(new Animator.AnimatorListener() {

                @Override
                public void onAnimationStart(Animator animation) {
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mOnPinEnteredListener.onPinEntered(getText());
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
        }
        set.playTogether(animUp, animAlpha);
        set.start();
    }

    public void setAnimateText(boolean animate) {
        mAnimate = animate;
    }

    public void setOnPinEnteredListener(OnPinEnteredListener l) {
        mOnPinEnteredListener = l;
    }

    public interface OnPinEnteredListener {
        void onPinEntered(CharSequence str);
    }


}
