package com.github.tablayout;

import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;


import com.github.tablayout.listener.ITab;
import com.github.tablayout.listener.OnTabSelectListener;
import com.github.tablayout.utils.DimensionUtils;
import com.github.tablayout.utils.FragmentChangeManager;
import com.github.tablayout.utils.UnreadMsgUtils;
import com.github.tablayout.widget.MsgView;

import java.util.ArrayList;

/**
 * 没有继承HorizontalScrollView不能滑动, 对于ViewPager无依赖
 */
public class CommonTabLayout extends FrameLayout implements ValueAnimator.AnimatorUpdateListener {
    private final Context mContext;
    private final ArrayList<ITab> mTabEntities = new ArrayList<>();
    private final LinearLayout mTabsContainer;
    private int mCurrentTab;
    private int mLastTab;
    private int mTabCount;
    /*-- 用于绘制显示器 -- */
    private final Rect mIndicatorRect = new Rect();
    private final GradientDrawable mIndicatorDrawable = new GradientDrawable();

    private final Paint mRectPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mDividerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mTrianglePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path mTrianglePath = new Path();
    private static final int STYLE_NORMAL = 0;
    private static final int STYLE_TRIANGLE = 1;
    private static final int STYLE_BLOCK = 2;
    private int mIndicatorStyle = STYLE_NORMAL;

    private float mTabPadding;
    private boolean mTabSpaceEqual;
    private float mTabWidth;

    /* -- indicator -- */
    private int mIndicatorColor;
    private float mIndicatorHeight;
    private float mIndicatorWidth;
    private float mIndicatorCornerRadius;
    private float mIndicatorMarginLeft;
    private float mIndicatorMarginTop;
    private float mIndicatorMarginRight;
    private float mIndicatorMarginBottom;
    private long mIndicatorAnimDuration;
    private boolean mIndicatorAnimEnable;
    private boolean mIndicatorBounceEnable;
    private int mIndicatorGravity;

    /* -- underline -- */
    private int mUnderlineColor;
    private float mUnderlineHeight;
    private int mUnderlineGravity;

    /* -- divider -- */
    private int mDividerColor;
    private float mDividerWidth;
    private float mDividerPadding;

    /* -- title -- */
    private static final int TEXT_BOLD_NONE = 0;
    private static final int TEXT_BOLD_WHEN_SELECT = 1;
    private static final int TEXT_BOLD_BOTH = 2;
    private float mTextSize;
    private float mSelectedTextSize;
    private int mTextSelectColor;
    private int mTextUnselectColor;
    private int mTextBold;
    private boolean mTextAllCaps;

    /* -- icon -- */
    private boolean mIconVisible;
    private int mIconGravity;
    private float mIconWidth;
    private float mIconHeight;
    private float mIconMargin;

    private int mHeight;

    /* -- anim -- */
    private final ValueAnimator mValueAnimator;
    private final OvershootInterpolator mInterpolator = new OvershootInterpolator(1.5f);

    private FragmentChangeManager mFragmentChangeManager;

    public CommonTabLayout(Context context) {
        this(context, null, 0);
    }

    public CommonTabLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CommonTabLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        //重写onDraw方法,需要调用这个方法来清除flag
        setWillNotDraw(false);
        setClipChildren(false);
        setClipToPadding(false);

        this.mContext = context;
        mTabsContainer = new LinearLayout(context);
        addView(mTabsContainer);

        obtainAttributes(context, attrs);

        //get layout_height
        String height = attrs.getAttributeValue("http://schemas.android.com/apk/res/android", "layout_height");

        //create ViewPager
        if (height.equals(ViewGroup.LayoutParams.MATCH_PARENT + "")) {
        } else if (height.equals(ViewGroup.LayoutParams.WRAP_CONTENT + "")) {
        } else {
            int[] systemAttrs = {android.R.attr.layout_height};
            TypedArray a = context.obtainStyledAttributes(attrs, systemAttrs);
            mHeight = a.getDimensionPixelSize(0, ViewGroup.LayoutParams.WRAP_CONTENT);
            a.recycle();
        }

        mValueAnimator = ValueAnimator.ofObject(new PointEvaluator(), mLastP, mCurrentP);
        mValueAnimator.addUpdateListener(this);
    }

    private void obtainAttributes(Context context, AttributeSet attrs) {
        TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.CommonTabLayout);

        mIndicatorStyle = ta.getInt(R.styleable.CommonTabLayout_tl_indicator_style, 0);
        mIndicatorColor = ta.getColor(R.styleable.CommonTabLayout_tl_indicator_color, Color.parseColor(mIndicatorStyle == STYLE_BLOCK ? "#4B6A87" : "#ffffff"));
        mIndicatorHeight = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_height,
                DimensionUtils.dp2px(context,(mIndicatorStyle == STYLE_TRIANGLE ? 4 : (mIndicatorStyle == STYLE_BLOCK ? -1 : 2))));
        mIndicatorWidth = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_width, DimensionUtils.dp2px(context,(mIndicatorStyle == STYLE_TRIANGLE ? 10 : -1)));
        mIndicatorCornerRadius = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_corner_radius, DimensionUtils.dp2px(context,(mIndicatorStyle == STYLE_BLOCK ? -1 : 0)));
        mIndicatorMarginLeft = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_margin_left, DimensionUtils.dp2px(context,0));
        mIndicatorMarginTop = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_margin_top, DimensionUtils.dp2px(context,(mIndicatorStyle == STYLE_BLOCK ? 7 : 0)));
        mIndicatorMarginRight = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_margin_right, DimensionUtils.dp2px(context,0));
        mIndicatorMarginBottom = ta.getDimension(R.styleable.CommonTabLayout_tl_indicator_margin_bottom, DimensionUtils.dp2px(context,(mIndicatorStyle == STYLE_BLOCK ? 7 : 0)));
        mIndicatorAnimEnable = ta.getBoolean(R.styleable.CommonTabLayout_tl_indicator_anim_enable, true);
        mIndicatorBounceEnable = ta.getBoolean(R.styleable.CommonTabLayout_tl_indicator_bounce_enable, true);
        mIndicatorAnimDuration = ta.getInt(R.styleable.CommonTabLayout_tl_indicator_anim_duration, -1);
        mIndicatorGravity = ta.getInt(R.styleable.CommonTabLayout_tl_indicator_gravity, Gravity.BOTTOM);

        mUnderlineColor = ta.getColor(R.styleable.CommonTabLayout_tl_underline_color, Color.parseColor("#ffffff"));
        mUnderlineHeight = ta.getDimension(R.styleable.CommonTabLayout_tl_underline_height, DimensionUtils.dp2px(context,0));
        mUnderlineGravity = ta.getInt(R.styleable.CommonTabLayout_tl_underline_gravity, Gravity.BOTTOM);

        mDividerColor = ta.getColor(R.styleable.CommonTabLayout_tl_divider_color, Color.parseColor("#ffffff"));
        mDividerWidth = ta.getDimension(R.styleable.CommonTabLayout_tl_divider_width, DimensionUtils.dp2px(context,0));
        mDividerPadding = ta.getDimension(R.styleable.CommonTabLayout_tl_divider_padding, DimensionUtils.dp2px(context,12));

        mTextSize = ta.getDimension(R.styleable.CommonTabLayout_tl_textSize, DimensionUtils.sp2px(context,13f));
        mSelectedTextSize = ta.getDimension(R.styleable.CommonTabLayout_tl_textSelectSize, mTextSize);
        mTextSelectColor = ta.getColor(R.styleable.CommonTabLayout_tl_textSelectColor, Color.parseColor("#ffffff"));
        mTextUnselectColor = ta.getColor(R.styleable.CommonTabLayout_tl_textUnselectColor, Color.parseColor("#AAffffff"));
        mTextBold = ta.getInt(R.styleable.CommonTabLayout_tl_textBold, TEXT_BOLD_NONE);
        mTextAllCaps = ta.getBoolean(R.styleable.CommonTabLayout_tl_textAllCaps, false);

        mIconVisible = ta.getBoolean(R.styleable.CommonTabLayout_tl_iconVisible, true);
        mIconGravity = ta.getInt(R.styleable.CommonTabLayout_tl_iconGravity, Gravity.TOP);
        mIconWidth = ta.getDimension(R.styleable.CommonTabLayout_tl_iconWidth, DimensionUtils.dp2px(context,0));
        mIconHeight = ta.getDimension(R.styleable.CommonTabLayout_tl_iconHeight, DimensionUtils.dp2px(context,0));
        mIconMargin = ta.getDimension(R.styleable.CommonTabLayout_tl_iconMargin, DimensionUtils.dp2px(context,2.5f));

        mTabSpaceEqual = ta.getBoolean(R.styleable.CommonTabLayout_tl_tab_space_equal, true);
        mTabWidth = ta.getDimension(R.styleable.CommonTabLayout_tl_tab_width, DimensionUtils.dp2px(context,-1));
        mTabPadding = ta.getDimension(R.styleable.CommonTabLayout_tl_tab_padding, mTabSpaceEqual || mTabWidth > 0 ? DimensionUtils.dp2px(context,0) : DimensionUtils.dp2px(context,10));

        ta.recycle();
    }

    /**
     * 设置tab数据
     *
     * @param tabEntities
     */
    public void setTabData(ArrayList<ITab> tabEntities) {
        if (tabEntities == null || tabEntities.size() == 0) {
            throw new IllegalStateException("TabEntities can not be NULL or EMPTY !");
        }

        this.mTabEntities.clear();
        this.mTabEntities.addAll(tabEntities);

        notifyDataSetChanged();
    }

    /**
     * 关联数据支持同时切换fragments
     *
     * @param tabEntities
     * @param fa
     * @param containerViewId
     * @param fragments
     */
    public void setTabData(ArrayList<ITab> tabEntities, FragmentActivity fa, int containerViewId, ArrayList<Fragment> fragments) {
        mFragmentChangeManager = new FragmentChangeManager(fa.getSupportFragmentManager(), containerViewId, fragments);
        setTabData(tabEntities);
    }

    /**
     * 更新数据
     */
    public void notifyDataSetChanged() {
        mTabsContainer.removeAllViews();
        this.mTabCount = mTabEntities.size();
        View tabView;
        for (int i = 0; i < mTabCount; i++) {
            if (mIconGravity == Gravity.LEFT) {
                tabView = View.inflate(mContext, R.layout.layout_tab_left, null);
            } else if (mIconGravity == Gravity.RIGHT) {
                tabView = View.inflate(mContext, R.layout.layout_tab_right, null);
            } else if (mIconGravity == Gravity.BOTTOM) {
                tabView = View.inflate(mContext, R.layout.layout_tab_bottom, null);
            } else {
                tabView = View.inflate(mContext, R.layout.layout_tab_top, null);
            }

            tabView.setTag(i);
            addTab(i, tabView);
        }

        updateTabStyles();
    }

    /**
     * 创建并添加tab
     *
     * @param position
     * @param tabView
     */
    private void addTab(final int position, View tabView) {
        TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
        tabTitleView.setText(mTabEntities.get(position).getTabTitle());
        ImageView tabIconView = (ImageView) tabView.findViewById(R.id.iv_tab_icon);
        tabIconView.setImageResource(mTabEntities.get(position).getTabUnselectedIcon());

        tabView.setOnClickListener(v -> {
            int childPosition = (Integer) v.getTag();
            if (mCurrentTab != childPosition) {
                setCurrentTab(childPosition);
                if (mListener != null) {
                    mListener.onTabSelect(childPosition);
                }
            } else {
                if (mListener != null) {
                    mListener.onTabReselect(childPosition);
                }
            }
        });

        // 每一个Tab的布局参数
        LinearLayout.LayoutParams itemTabLp = mTabSpaceEqual ?
                new LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT, 1.0f) :
                new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT);
        if (mTabWidth > 0) {
            itemTabLp = new LinearLayout.LayoutParams((int) mTabWidth, LayoutParams.MATCH_PARENT);
        }
        mTabsContainer.addView(tabView, position, itemTabLp);
    }

    /**
     * 更新Tab样式
     */
    private void updateTabStyles() {
        for (int i = 0; i < mTabCount; i++) {
            View tabView = mTabsContainer.getChildAt(i);
            tabView.setPadding((int) mTabPadding, 0, (int) mTabPadding, 0);
            TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
            tabTitleView.setTextColor(i == mCurrentTab ? mTextSelectColor : mTextUnselectColor);
            tabTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, i == mCurrentTab ? mSelectedTextSize : mTextSize);
//            tabTitleView.setPadding((int) mTabPadding, 0, (int) mTabPadding, 0);
            if (mTextAllCaps) {
                tabTitleView.setText(tabTitleView.getText().toString().toUpperCase());
            }

            if (mTextBold == TEXT_BOLD_BOTH) {
                tabTitleView.getPaint().setFakeBoldText(true);
            } else if (mTextBold == TEXT_BOLD_NONE) {
                tabTitleView.getPaint().setFakeBoldText(false);
            } else if (mTextBold ==TEXT_BOLD_WHEN_SELECT){
                tabTitleView.getPaint().setFakeBoldText(i == mCurrentTab);
            }

            ImageView tabIconView = (ImageView) tabView.findViewById(R.id.iv_tab_icon);
            if (mIconVisible) {
                tabIconView.setVisibility(View.VISIBLE);
                ITab tabEntity = mTabEntities.get(i);
                tabIconView.setImageResource(i == mCurrentTab ? tabEntity.getTabSelectedIcon() : tabEntity.getTabUnselectedIcon());
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        mIconWidth <= 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : (int) mIconWidth,
                        mIconHeight <= 0 ? LinearLayout.LayoutParams.WRAP_CONTENT : (int) mIconHeight);
                if (mIconGravity == Gravity.LEFT) {
                    lp.rightMargin = (int) mIconMargin;
                } else if (mIconGravity == Gravity.RIGHT) {
                    lp.leftMargin = (int) mIconMargin;
                } else if (mIconGravity == Gravity.BOTTOM) {
                    lp.topMargin = (int) mIconMargin;
                } else {
                    lp.bottomMargin = (int) mIconMargin;
                }

                tabIconView.setLayoutParams(lp);
            } else {
                tabIconView.setVisibility(View.GONE);
            }
        }
    }

    private void updateTabSelection(int position) {
        for (int i = 0; i < mTabCount; ++i) {
            View tabView = mTabsContainer.getChildAt(i);
            final boolean isSelect = i == position;
            TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
            tabTitleView.setTextColor(isSelect ? mTextSelectColor : mTextUnselectColor);
            tabTitleView.setTextSize(TypedValue.COMPLEX_UNIT_PX, isSelect ? mSelectedTextSize : mTextSize);
            ImageView tabIconView = (ImageView) tabView.findViewById(R.id.iv_tab_icon);
            ITab tabEntity = mTabEntities.get(i);
            tabIconView.setImageResource(isSelect ? tabEntity.getTabSelectedIcon() : tabEntity.getTabUnselectedIcon());
            if (mTextBold == TEXT_BOLD_WHEN_SELECT) {
                tabTitleView.getPaint().setFakeBoldText(isSelect);
            }
        }
    }

    private void calcOffset() {
        final View currentTabView = mTabsContainer.getChildAt(this.mCurrentTab);
        mCurrentP.left = currentTabView.getLeft();
        mCurrentP.right = currentTabView.getRight();

        final View lastTabView = mTabsContainer.getChildAt(this.mLastTab);
        mLastP.left = lastTabView.getLeft();
        mLastP.right = lastTabView.getRight();

        if (mLastP.left == mCurrentP.left && mLastP.right == mCurrentP.right) {
            invalidate();
        } else {
            mValueAnimator.setObjectValues(mLastP, mCurrentP);
            if (mIndicatorBounceEnable) {
                mValueAnimator.setInterpolator(mInterpolator);
            }

            if (mIndicatorAnimDuration < 0) {
                mIndicatorAnimDuration = mIndicatorBounceEnable ? 500 : 250;
            }
            mValueAnimator.setDuration(mIndicatorAnimDuration);
            mValueAnimator.start();
        }
    }

    private void calcIndicatorRect() {
        View currentTabView = mTabsContainer.getChildAt(this.mCurrentTab);
        float left = currentTabView.getLeft();
        float right = currentTabView.getRight();

        mIndicatorRect.left = (int) left;
        mIndicatorRect.right = (int) right;

        if (mIndicatorWidth < 0) {
            //indicatorWidth小于0时,原jpardogo's PagerSlidingTabStrip

        } else {
            //indicatorWidth大于0时,圆角矩形以及三角形
            float indicatorLeft = currentTabView.getLeft() + (currentTabView.getWidth() - mIndicatorWidth) / 2;

            mIndicatorRect.left = (int) indicatorLeft;
            mIndicatorRect.right = (int) (mIndicatorRect.left + mIndicatorWidth);
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        View currentTabView = mTabsContainer.getChildAt(this.mCurrentTab);
        IndicatorPoint p = (IndicatorPoint) animation.getAnimatedValue();
        mIndicatorRect.left = (int) p.left;
        mIndicatorRect.right = (int) p.right;

        if (mIndicatorWidth < 0) {
            //indicatorWidth小于0时,原jpardogo's PagerSlidingTabStrip

        } else {
            //indicatorWidth大于0时,圆角矩形以及三角形
            float indicatorLeft = p.left + (currentTabView.getWidth() - mIndicatorWidth) / 2;

            mIndicatorRect.left = (int) indicatorLeft;
            mIndicatorRect.right = (int) (mIndicatorRect.left + mIndicatorWidth);
        }
        invalidate();
    }

    private boolean mIsFirstDraw = true;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (isInEditMode() || mTabCount <= 0) {
            return;
        }

        int height = getHeight();
        int paddingLeft = getPaddingLeft();
        // draw divider
        if (mDividerWidth > 0) {
            mDividerPaint.setStrokeWidth(mDividerWidth);
            mDividerPaint.setColor(mDividerColor);
            for (int i = 0; i < mTabCount - 1; i++) {
                View tab = mTabsContainer.getChildAt(i);
                canvas.drawLine(paddingLeft + tab.getRight(), mDividerPadding, paddingLeft + tab.getRight(), height - mDividerPadding, mDividerPaint);
            }
        }

        // draw underline
        if (mUnderlineHeight > 0) {
            mRectPaint.setColor(mUnderlineColor);
            if (mUnderlineGravity == Gravity.BOTTOM) {
                canvas.drawRect(paddingLeft, height - mUnderlineHeight, mTabsContainer.getWidth() + paddingLeft, height, mRectPaint);
            } else {
                canvas.drawRect(paddingLeft, 0, mTabsContainer.getWidth() + paddingLeft, mUnderlineHeight, mRectPaint);
            }
        }

        //draw indicator line
        if (mIndicatorAnimEnable) {
            if (mIsFirstDraw) {
                mIsFirstDraw = false;
                calcIndicatorRect();
            }
        } else {
            calcIndicatorRect();
        }


        if (mIndicatorStyle == STYLE_TRIANGLE) {
            if (mIndicatorHeight > 0) {
                mTrianglePaint.setColor(mIndicatorColor);
                mTrianglePath.reset();
                mTrianglePath.moveTo(paddingLeft + mIndicatorRect.left, height);
                mTrianglePath.lineTo(paddingLeft + mIndicatorRect.left / 2f + mIndicatorRect.right / 2f, height - mIndicatorHeight);
                mTrianglePath.lineTo(paddingLeft + mIndicatorRect.right, height);
                mTrianglePath.close();
                canvas.drawPath(mTrianglePath, mTrianglePaint);
            }
        } else if (mIndicatorStyle == STYLE_BLOCK) {
            if (mIndicatorHeight < 0) {
                mIndicatorHeight = height - mIndicatorMarginTop - mIndicatorMarginBottom;
            } else {

            }

            if (mIndicatorHeight > 0) {
                if (mIndicatorCornerRadius < 0 || mIndicatorCornerRadius > mIndicatorHeight / 2) {
                    mIndicatorCornerRadius = mIndicatorHeight / 2;
                }

                mIndicatorDrawable.setColor(mIndicatorColor);
                mIndicatorDrawable.setBounds(paddingLeft + (int) mIndicatorMarginLeft + mIndicatorRect.left,
                        (int) mIndicatorMarginTop, (int) (paddingLeft + mIndicatorRect.right - mIndicatorMarginRight),
                        (int) (mIndicatorMarginTop + mIndicatorHeight));
                mIndicatorDrawable.setCornerRadius(mIndicatorCornerRadius);
                mIndicatorDrawable.draw(canvas);
            }
        } else {
               /* mRectPaint.setColor(mIndicatorColor);
                calcIndicatorRect();
                canvas.drawRect(getPaddingLeft() + mIndicatorRect.left, getHeight() - mIndicatorHeight,
                        mIndicatorRect.right + getPaddingLeft(), getHeight(), mRectPaint);*/

            if (mIndicatorHeight > 0) {
                mIndicatorDrawable.setColor(mIndicatorColor);
                if (mIndicatorGravity == Gravity.BOTTOM) {
                    mIndicatorDrawable.setBounds(paddingLeft + (int) mIndicatorMarginLeft + mIndicatorRect.left,
                            height - (int) mIndicatorHeight - (int) mIndicatorMarginBottom,
                            paddingLeft + mIndicatorRect.right - (int) mIndicatorMarginRight,
                            height - (int) mIndicatorMarginBottom);
                } else {
                    mIndicatorDrawable.setBounds(paddingLeft + (int) mIndicatorMarginLeft + mIndicatorRect.left,
                            (int) mIndicatorMarginTop,
                            paddingLeft + mIndicatorRect.right - (int) mIndicatorMarginRight,
                            (int) mIndicatorHeight + (int) mIndicatorMarginTop);
                }
                mIndicatorDrawable.setCornerRadius(mIndicatorCornerRadius);
                mIndicatorDrawable.draw(canvas);
            }
        }
    }

    /* -- setter and getter -- **/
    public void setCurrentTab(int currentTab) {
        mLastTab = this.mCurrentTab;
        this.mCurrentTab = currentTab;
        updateTabSelection(currentTab);
        if (mFragmentChangeManager != null) {
            mFragmentChangeManager.setFragments(currentTab);
        }
        if (mIndicatorAnimEnable) {
            calcOffset();
        } else {
            invalidate();
        }
    }

    public void setIndicatorStyle(int indicatorStyle) {
        this.mIndicatorStyle = indicatorStyle;
        invalidate();
    }

    public void setTabPadding(float tabPadding) {
        this.mTabPadding = DimensionUtils.dp2px(getContext(), tabPadding);
        updateTabStyles();
    }

    public void setTabSpaceEqual(boolean tabSpaceEqual) {
        this.mTabSpaceEqual = tabSpaceEqual;
        updateTabStyles();
    }

    public void setTabWidth(float tabWidth) {
        this.mTabWidth = DimensionUtils.dp2px(getContext(), tabWidth);
        updateTabStyles();
    }

    public void setIndicatorColor(int indicatorColor) {
        this.mIndicatorColor = indicatorColor;
        invalidate();
    }

    public void setIndicatorHeight(float indicatorHeight) {
        this.mIndicatorHeight = DimensionUtils.dp2px(getContext(), indicatorHeight);
        invalidate();
    }

    public void setIndicatorWidth(float indicatorWidth) {
        this.mIndicatorWidth = DimensionUtils.dp2px(getContext(), indicatorWidth);
        invalidate();
    }

    public void setIndicatorCornerRadius(float indicatorCornerRadius) {
        this.mIndicatorCornerRadius = DimensionUtils.dp2px(getContext(), indicatorCornerRadius);
        invalidate();
    }

    public void setIndicatorGravity(int indicatorGravity) {
        this.mIndicatorGravity = indicatorGravity;
        invalidate();
    }

    public void setIndicatorMargin(float indicatorMarginLeft, float indicatorMarginTop,
                                   float indicatorMarginRight, float indicatorMarginBottom) {
        this.mIndicatorMarginLeft = DimensionUtils.dp2px(getContext(), indicatorMarginLeft);
        this.mIndicatorMarginTop = DimensionUtils.dp2px(getContext(), indicatorMarginTop);
        this.mIndicatorMarginRight = DimensionUtils.dp2px(getContext(), indicatorMarginRight);
        this.mIndicatorMarginBottom = DimensionUtils.dp2px(getContext(), indicatorMarginBottom);
        invalidate();
    }

    public void setIndicatorAnimDuration(long indicatorAnimDuration) {
        this.mIndicatorAnimDuration = indicatorAnimDuration;
    }

    public void setIndicatorAnimEnable(boolean indicatorAnimEnable) {
        this.mIndicatorAnimEnable = indicatorAnimEnable;
    }

    public void setIndicatorBounceEnable(boolean indicatorBounceEnable) {
        this.mIndicatorBounceEnable = indicatorBounceEnable;
    }

    public void setUnderlineColor(int underlineColor) {
        this.mUnderlineColor = underlineColor;
        invalidate();
    }

    public void setUnderlineHeight(float underlineHeight) {
        this.mUnderlineHeight = DimensionUtils.dp2px(getContext(), underlineHeight);
        invalidate();
    }

    public void setUnderlineGravity(int underlineGravity) {
        this.mUnderlineGravity = underlineGravity;
        invalidate();
    }

    public void setDividerColor(int dividerColor) {
        this.mDividerColor = dividerColor;
        invalidate();
    }

    public void setDividerWidth(float dividerWidth) {
        this.mDividerWidth = DimensionUtils.dp2px(getContext(), dividerWidth);
        invalidate();
    }

    public void setDividerPadding(float dividerPadding) {
        this.mDividerPadding = DimensionUtils.dp2px(getContext(), dividerPadding);
        invalidate();
    }

    public void setTextSize(float textSize) {
        this.mTextSize = DimensionUtils.sp2px(getContext(), textSize);
        updateTabStyles();
    }

    public void setTextSelectedSize(float textSize) {
        this.mSelectedTextSize = DimensionUtils.sp2px(getContext(), textSize);
        updateTabStyles();
    }

    public void setTextSelectColor(int textSelectColor) {
        this.mTextSelectColor = textSelectColor;
        updateTabStyles();
    }

    public void setTextUnselectColor(int textUnselectColor) {
        this.mTextUnselectColor = textUnselectColor;
        updateTabStyles();
    }

    public void setTextBold(int textBold) {
        this.mTextBold = textBold;
        updateTabStyles();
    }

    public void setIconVisible(boolean iconVisible) {
        this.mIconVisible = iconVisible;
        updateTabStyles();
    }

    public void setIconGravity(int iconGravity) {
        this.mIconGravity = iconGravity;
        notifyDataSetChanged();
    }

    public void setIconWidth(float iconWidth) {
        this.mIconWidth = DimensionUtils.dp2px(getContext(), iconWidth);
        updateTabStyles();
    }

    public void setIconHeight(float iconHeight) {
        this.mIconHeight = DimensionUtils.dp2px(getContext(), iconHeight);
        updateTabStyles();
    }

    public void setIconMargin(float iconMargin) {
        this.mIconMargin = DimensionUtils.dp2px(getContext(), iconMargin);
        updateTabStyles();
    }

    public void setTextAllCaps(boolean textAllCaps) {
        this.mTextAllCaps = textAllCaps;
        updateTabStyles();
    }


    public int getTabCount() {
        return mTabCount;
    }

    public int getCurrentTab() {
        return mCurrentTab;
    }

    public int getIndicatorStyle() {
        return mIndicatorStyle;
    }

    public float getTabPadding() {
        return mTabPadding;
    }

    public boolean isTabSpaceEqual() {
        return mTabSpaceEqual;
    }

    public float getTabWidth() {
        return mTabWidth;
    }

    public int getIndicatorColor() {
        return mIndicatorColor;
    }

    public float getIndicatorHeight() {
        return mIndicatorHeight;
    }

    public float getIndicatorWidth() {
        return mIndicatorWidth;
    }

    public float getIndicatorCornerRadius() {
        return mIndicatorCornerRadius;
    }

    public float getIndicatorMarginLeft() {
        return mIndicatorMarginLeft;
    }

    public float getIndicatorMarginTop() {
        return mIndicatorMarginTop;
    }

    public float getIndicatorMarginRight() {
        return mIndicatorMarginRight;
    }

    public float getIndicatorMarginBottom() {
        return mIndicatorMarginBottom;
    }

    public long getIndicatorAnimDuration() {
        return mIndicatorAnimDuration;
    }

    public boolean isIndicatorAnimEnable() {
        return mIndicatorAnimEnable;
    }

    public boolean isIndicatorBounceEnable() {
        return mIndicatorBounceEnable;
    }

    public int getUnderlineColor() {
        return mUnderlineColor;
    }

    public float getUnderlineHeight() {
        return mUnderlineHeight;
    }

    public int getDividerColor() {
        return mDividerColor;
    }

    public float getDividerWidth() {
        return mDividerWidth;
    }

    public float getDividerPadding() {
        return mDividerPadding;
    }

    public float getTextSize() {
        return mTextSize;
    }

    public int getTextSelectColor() {
        return mTextSelectColor;
    }

    public int getTextUnselectColor() {
        return mTextUnselectColor;
    }

    public int getTextBold() {
        return mTextBold;
    }

    public boolean isTextAllCaps() {
        return mTextAllCaps;
    }

    public int getIconGravity() {
        return mIconGravity;
    }

    public float getIconWidth() {
        return mIconWidth;
    }

    public float getIconHeight() {
        return mIconHeight;
    }

    public float getIconMargin() {
        return mIconMargin;
    }

    public boolean isIconVisible() {
        return mIconVisible;
    }


    public ImageView getIconView(int tab) {
        View tabView = mTabsContainer.getChildAt(tab);
        return (ImageView) tabView.findViewById(R.id.iv_tab_icon);
    }

    public TextView getTitleView(int tab) {
        View tabView = mTabsContainer.getChildAt(tab);
        return (TextView) tabView.findViewById(R.id.tv_tab_title);
    }

    // show MsgTipView
    private final Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SparseBooleanArray mInitSetMap = new SparseBooleanArray();

    /**
     * 显示未读消息
     *
     * @param position 显示tab位置
     * @param num      num小于等于0显示红点,num大于0显示数字
     */
    public void showMsg(int position, int num) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }

        View tabView = mTabsContainer.getChildAt(position);
        MsgView tipView = (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
        if (tipView != null) {
            UnreadMsgUtils.show(tipView, num);

            if (mInitSetMap.get(position)) {
                return;
            }

            if (!mIconVisible) {
                setMsgMargin(position, 2, 2);
            } else {
                setMsgMargin(position, 0,
                        mIconGravity == Gravity.LEFT || mIconGravity == Gravity.RIGHT ? 4 : 0);
            }

            mInitSetMap.put(position, true);
        }
    }

    /**
     * 显示未读红点
     *
     * @param position 显示tab位置
     */
    public void showDot(int position) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }
        showMsg(position, 0);
    }

    /**
     * 隐藏消息
     *
     * @param position
     */
    public void hideMsg(int position) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }

        View tabView = mTabsContainer.getChildAt(position);
        MsgView tipView = (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
        if (tipView != null) {
            tipView.setVisibility(View.GONE);
        }
    }

    /**
     * 设置提示红点偏移,注意
     * 1.控件为固定高度:参照点为tab内容的右上角
     * 2.控件高度不固定(WRAP_CONTENT):参照点为tab内容的右上角,此时高度已是红点的最高显示范围,所以这时bottomPadding其实就是topPadding
     */
    public void setMsgMargin(int position, float leftPadding, float bottomPadding) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }
        View tabView = mTabsContainer.getChildAt(position);
        MsgView tipView = (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
        if (tipView != null) {
            TextView tabTitleView = (TextView) tabView.findViewById(R.id.tv_tab_title);
            mTextPaint.setTextSize(mTextSize);
            float textWidth = mTextPaint.measureText(tabTitleView.getText().toString());
            float textHeight = mTextPaint.descent() - mTextPaint.ascent();
            MarginLayoutParams lp = (MarginLayoutParams) tipView.getLayoutParams();

            float iconH = mIconHeight;
            float margin = 0;
            if (mIconVisible) {
                if (iconH <= 0) {
                    iconH = ContextCompat.getDrawable(getContext(), mTabEntities.get(position).getTabSelectedIcon()).getIntrinsicHeight();
                }
                margin = mIconMargin;
            }

            if (mIconGravity == Gravity.TOP || mIconGravity == Gravity.BOTTOM) {
                lp.leftMargin = DimensionUtils.dp2px(getContext(), leftPadding);
                lp.topMargin = mHeight > 0 ? (int) (mHeight - textHeight - iconH - margin) / 2 - DimensionUtils.dp2px(getContext(), bottomPadding) : DimensionUtils.dp2px(getContext(), bottomPadding);
            } else {
                lp.leftMargin = DimensionUtils.dp2px(getContext(), leftPadding);
                lp.topMargin = mHeight > 0 ? (int) (mHeight - Math.max(textHeight, iconH)) / 2 - DimensionUtils.dp2px(getContext(), bottomPadding) : DimensionUtils.dp2px(getContext(), bottomPadding);
            }

            tipView.setLayoutParams(lp);
        }
    }

    /**
     * 当前类只提供了少许设置未读消息属性的方法,可以通过该方法获取MsgView对象从而各种设置
     * @param position
     * @return
     */
    public MsgView getMsgView(int position) {
        if (position >= mTabCount) {
            position = mTabCount - 1;
        }
        View tabView = mTabsContainer.getChildAt(position);
        return (MsgView) tabView.findViewById(R.id.rtv_msg_tip);
    }

    private OnTabSelectListener mListener;

    public void setOnTabSelectListener(OnTabSelectListener listener) {
        this.mListener = listener;
    }


    @Override
    protected Parcelable onSaveInstanceState() {
        Bundle bundle = new Bundle();
        bundle.putParcelable("instanceState", super.onSaveInstanceState());
        bundle.putInt("mCurrentTab", mCurrentTab);
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof Bundle) {
            Bundle bundle = (Bundle) state;
            mCurrentTab = bundle.getInt("mCurrentTab");
            state = bundle.getParcelable("instanceState");
            if (mCurrentTab != 0 && mTabsContainer.getChildCount() > 0) {
                updateTabSelection(mCurrentTab);
            }
        }
        super.onRestoreInstanceState(state);
    }

    static class IndicatorPoint {
        public float left;
        public float right;
    }

    private final IndicatorPoint mCurrentP = new IndicatorPoint();
    private final IndicatorPoint mLastP = new IndicatorPoint();

    static class PointEvaluator implements TypeEvaluator<IndicatorPoint> {
        @Override
        public IndicatorPoint evaluate(float fraction, IndicatorPoint startValue, IndicatorPoint endValue) {
            float left = startValue.left + fraction * (endValue.left - startValue.left);
            float right = startValue.right + fraction * (endValue.right - startValue.right);
            IndicatorPoint point = new IndicatorPoint();
            point.left = left;
            point.right = right;
            return point;
        }
    }
}
