package com.github.kevin.library;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

public class RecyclerView extends ViewGroup {
    //y偏移量 距离屏幕上边界的距离
    private int scrollY;
    //当前显示的View
    private List<View> viewList;
    //当前滑动的Y值
    private int currentY;
    //行数
    private int rowCount;
    //初始化 第一屏最慢 onLayout()一次
    private boolean needRelayout = true;
    //当前RecyclerView的宽高
    private int width, height;
    //item高度
    private int[] heights;
    //View的第一行占内容的第几行
    private int firstRow;
    //最小滑动距离
    private int touchSlop;
    //持有一个回收池的引用
    private Recycler recycler;
    private Adapter adapter;

    public void setAdapter(Adapter adapter) {
        this.adapter = adapter;
        if (adapter != null) {
            recycler = new Recycler(adapter.getViewTypeCount());
        }
        scrollY = 0;
        firstRow = 0;
        needRelayout = true;
        requestLayout();//1、onMeasure 2、onLayout
    }

    //适配器
    public interface Adapter {
        //item的类型
        int getItemViewType(int row);

        //item的类型数量
        int getViewTypeCount();

        View onCreateViewHolder(int position, ViewGroup parent);

        View onBinderViewHolder(int position, View convertView, ViewGroup parent);

        int getCount();

        int getHeight(int index);
    }


    public RecyclerView(Context context) {
        super(context);
        init(context);
    }

    public RecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.viewList = new ArrayList<>();
        this.needRelayout = true;
        // 点击 28-40   滑动 40+
        this.touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int h = 0; //高度 item
        if (adapter != null) {
            rowCount = adapter.getCount();
            heights = new int[rowCount];
            for (int i = 0; i < heights.length; i++) {
                heights[i] = adapter.getHeight(i);
            }
        }
        // RecyclerView的高度
        int tmpH = sumArray(heights, 0, heights.length);
        h = Math.min(heightSize, tmpH);
        setMeasuredDimension(widthSize, h);
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if (needRelayout || changed) {
            needRelayout = false;
            viewList.clear();
            removeAllViews();
            if (adapter != null) {
                width = r - l;
                height = b - t;
                int left, top = 0, right, bottom;
                for (int i = 0; i < rowCount && top < height; i++) {
                    bottom = top + heights[i];
                    View view = makeAndStep(i, 0, top, width, bottom);
                    viewList.add(view);
                    top = bottom;//循环摆放
                }
            }
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        boolean intercept = false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                currentY = (int) ev.getRawY();
                break;
            case MotionEvent.ACTION_MOVE:
                int y2 = Math.abs(currentY - (int) ev.getRawY());
                //只拦截滑动，不拦截点击
                if (y2 > touchSlop) {
                    intercept = true;
                }
                break;
        }
        return intercept;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                int y2 = (int) event.getRawY();
                //diff为正数表示上滑，反之表示下滑
                int diff = currentY - y2;
                scrollBy(0, diff);
                break;
        }
        return super.onTouchEvent(event);
    }


    @Override
    public void removeView(View view) {
        int type = (int) view.getTag(R.id.tag_type_view);
        recycler.put(view, type);
    }

    private int scrollBounds(int scrollY, int firstRow, int sizes[], int viewSize) {
        if (scrollY > 0) { //上滑
            scrollY = Math.min(scrollY, sumArray(sizes, firstRow, sizes.length - firstRow) - viewSize);
        } else {//下滑
            scrollY = Math.max(scrollY, -sumArray(sizes, 0, firstRow));
        }
        return scrollY;
    }

    @Override
    public void scrollBy(int x, int y) {
        //累计滑动距离
        scrollY += y;
        //修正上下两个边界的极限值
        scrollY = scrollBounds(scrollY, firstRow, heights, height);
        if (scrollY > 0) { //上滑
            // 移除
            while (scrollY > heights[firstRow]) { //滑出屏幕上边界
                if (!viewList.isEmpty()) {
                    removeView(viewList.remove(0));
                }
                //为了兼容快速上滑多个item，如果只上滑一个item，则相当于scrollY = 0
                scrollY -= heights[firstRow];
                firstRow++;
            }
            //添加
            while (getFilledHeight() < height) {
                int addLast = firstRow + viewList.size();
                View view = obtainView(addLast, width, heights[addLast]);
                viewList.add(viewList.size(), view);
            }
        } else if (scrollY < 0) { //下滑
            while (!viewList.isEmpty() && getFilledHeight() - heights[firstRow + viewList.size()] > 0) {
                if (!viewList.isEmpty()) {
                    removeView(viewList.remove(viewList.size() - 1));
                }
            }
            while (scrollY < 0) {
                int firstAddRow = firstRow - 1;
                View view = obtainView(firstAddRow, width, heights[firstAddRow]);
                viewList.add(0, view);
                firstRow--;
                scrollY += heights[firstRow + 1];
            }
        }
        //重新对一个子View进行摆放
        repositionViews();
        //显示ScrollBar
        awakenScrollBars();
    }

    private void repositionViews() {
        int left, top, right, bottom, i;
        top = -scrollY;
        i = firstRow;
        for (View view : viewList) {
            bottom = top + heights[i++];
            view.layout(0, top, width, bottom);
            top = bottom;
        }
    }


    /**
     * 可见元素加起来的总高度
     * @return
     */
    public int getFilledHeight() {
        return sumArray(heights, firstRow, viewList.size()) - scrollY;
    }

    private View makeAndStep(int row, int left, int top, int right, int bottom) {
        //生成View
        View view = obtainView(row, right - left, bottom - top);
        //摆放View
        view.layout(left, top, right, bottom);

        return view;
    }

    /**
     * 从回收池里面获取View
     *
     * @param row
     * @param width
     * @param height
     * @return
     */
    private View obtainView(int row, int width, int height) {
        int itemType = adapter.getItemViewType(row);
        View recyView = recycler.get(itemType);
        View view = null;
        if (recyView == null) {
            recyView = adapter.onCreateViewHolder(row, this);
            if (recyView == null) {
                throw new RuntimeException("onCreateViewHolder 必须填充布局");
            }
        }
        view = adapter.onBinderViewHolder(row, recyView, this);
        view.setTag(R.id.tag_type_view, itemType);
        view.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));

        addView(view, 0);

        return view;
    }

    private int sumArray(int[] heights, int firstIndex, int count) {
        int sum = 0;
        count += firstIndex;
        for (int i = firstIndex; i < count; i++) {
            sum += heights[i];
        }
        return sum;
    }

}
