package com.ivyshare.ui.main;


import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.HorizontalScrollView;

public class MenuHorizontalScrollView extends HorizontalScrollView {

	/* 当前控件 */
	private MenuHorizontalScrollView me;

	/* 菜单 */
	private View menu;

	/* 菜单状态 */
	private boolean menuOut;

	/* 扩展宽度 */
	public static final int ENLARGE_WIDTH = 280;

	/* 菜单的宽度 */
	private int menuWidth;

	/* 按钮 */
	// private Button menuBtn;

	/* 当前滑动的位置 */
	private int current;

	private int scrollToViewPos;

	// private GestureDetector mGestureDetector;
	View.OnTouchListener mGestureListener;

	private boolean mIsFirst = false;

	public void setIsFirst(boolean isFirst) {
		mIsFirst = isFirst;
	}

	public MenuHorizontalScrollView(Context context) {
		super(context);
		// TODO Auto-generated constructor stub
		init();
	}

	public MenuHorizontalScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO Auto-generated constructor stub
		init();
	}

	public MenuHorizontalScrollView(Context context, AttributeSet attrs,
			int defStyle) {
		super(context, attrs, defStyle);
		// TODO Auto-generated constructor stub
		init();
	}

	private void init() {
		// mGestureDetector = new GestureDetector(new YScrollDetector());

		setFadingEdgeLength(0);

		this.setHorizontalFadingEdgeEnabled(false);
		this.setVerticalFadingEdgeEnabled(false);
		this.me = this;
		this.me.setVisibility(View.INVISIBLE);
		this.menuOut = false;
	}

	/**
	 * Init the scroll view. add a TRANSPARENT view to scrollview, to show the
	 * under layer menuView
	 * 
	 * @param context
	 * @param contentView
	 * @param menuView
	 * @param sizeCallBack
	 */
	public void initViews(Context context, View contentView, View menuView,
			SizeCallBack sizeCallBack) {
		this.menu = menuView;
		View sideView = new View(context);
		sideView.setBackgroundColor(Color.TRANSPARENT);
		final View[] children = new View[] { sideView, contentView };

		ViewGroup parent = (ViewGroup) getChildAt(0);
		// parent.removeAllViews();

		for (int i = 0; i < children.length; i++) {
			children[i].setVisibility(View.INVISIBLE);
			parent.addView(children[i]);
		}
		OnGlobalLayoutListener onGlLayoutistener = new MenuOnGlobalLayoutListener(
				parent, children, sizeCallBack);
		getViewTreeObserver().addOnGlobalLayoutListener(onGlLayoutistener);

	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent ev) {
		if (menuOut)
			return true;

		return super.onInterceptTouchEvent(ev) && mIsFirst;
		// && mGestureDetector.onTouchEvent(ev);
	}

	/**
	 * when press back key, if menu showing, close it and intercept this key
	 * event.
	 */
	public boolean onBackPressed() {
		if (menuOut) {
			interactMenu();
			return true;
		}

		return false;
	}

	/**
	 * Show or display side menu
	 */
	public void interactMenu() {

		if (!this.menuOut) {
			this.menuWidth = 0;
		} else {
			this.menuWidth = this.menu.getMeasuredWidth() - ENLARGE_WIDTH;
		}
		menuSlide();
	}

	/**
	 * 滑动出菜单
	 */
	private void menuSlide() {

		if (this.menuWidth == 0) {
			this.menuOut = true;
			requestDisallowInterceptTouchEvent(false);
		} else {
			this.menuOut = false;
			requestDisallowInterceptTouchEvent(true);
		}
		me.scrollTo(this.menuWidth, 0);
		// if(this.menuOut == true)
		// this.menuBtn.setBackgroundResource(R.drawable.menu_fold);
		// else
		// this.menuBtn.setBackgroundResource(R.drawable.menu_unfold);
	}

	@Override
	protected void onScrollChanged(int l, int t, int oldl, int oldt) {
		// TODO Auto-generated method stub
		super.onScrollChanged(l, t, oldl, oldt);
		if (l < (this.menu.getMeasuredWidth() - ENLARGE_WIDTH) / 2) {
			this.menuWidth = 0;
		} else {
			this.menuWidth = this.menu.getWidth() - ENLARGE_WIDTH;
		}
		this.current = l;
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev) {
		// TODO Auto-generated method stub
		int x = (int) ev.getRawX();
		if ((this.current == 0 && x < this.scrollToViewPos)
				|| (this.current == this.scrollToViewPos * 2 && x > ENLARGE_WIDTH)) {
			return false;
		} else {
			if (ev.getAction() == MotionEvent.ACTION_UP) {
				menuSlide();
				return false;
			}
		}
		return super.onTouchEvent(ev);
	}

	/****************************************************/
	/*-												   -*/
	/*-			Class 			Area				   -*/
	/*-												   -*/
	/****************************************************/

	public class MenuOnGlobalLayoutListener implements OnGlobalLayoutListener {

		private ViewGroup parent;
		private View[] children;
		// private int scrollToViewIndex = 0;
		private SizeCallBack sizeCallBack;

		public MenuOnGlobalLayoutListener(ViewGroup parent, View[] children,
				SizeCallBack sizeCallBack) {

			this.parent = parent;
			this.children = children;
			this.sizeCallBack = sizeCallBack;
		}

		@Override
		public void onGlobalLayout() {
			// TODO Auto-generated method stub
			me.getViewTreeObserver().removeGlobalOnLayoutListener(this);
			this.sizeCallBack.onGlobalLayout();
			this.parent.removeViewsInLayout(0, children.length);
			int width = me.getMeasuredWidth();
			int height = me.getMeasuredHeight();

			int[] dims = new int[2];
			scrollToViewPos = 0;

			for (int i = 0; i < children.length; i++) {
				this.sizeCallBack.getViewSize(i, width, height, dims);
				children[i].setVisibility(View.VISIBLE);
				parent.addView(children[i], dims[0], dims[1]);
				if (i == 0) {
					scrollToViewPos += dims[0];
				}
			}

			new Handler().post(new Runnable() {
				@Override
				public void run() {
					me.scrollBy(scrollToViewPos, 0);

					/* 视图不是中间视图 */
					me.setVisibility(View.VISIBLE);
					menu.setVisibility(View.VISIBLE);
				}
			});
		}
	}

	// class YScrollDetector extends SimpleOnGestureListener {
	// @Override
	// public boolean onScroll(MotionEvent e1, MotionEvent e2,
	// float distanceX, float distanceY) {
	// if (distanceY != 0 && distanceX != 0) {
	//
	// }
	// if (Math.abs(distanceY) >= Math.abs(distanceX)) {
	// return true;
	// }
	// return false;
	// }
	// }
}
