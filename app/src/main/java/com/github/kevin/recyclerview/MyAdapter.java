package com.github.kevin.recyclerview;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.kevin.library.RecyclerView;

public class MyAdapter implements RecyclerView.Adapter {
    private LayoutInflater inflater;
    private int count, height;
    private Context context;

    public MyAdapter(Context context, int count) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.count = count;
    }

    @Override
    public int getItemViewType(int row) {
        return 0;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public View onCreateViewHolder(int position, ViewGroup parent) {
        return inflater.inflate(R.layout.item_table, parent, false);
    }

    /**
     * 做绘制操作
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View onBinderViewHolder(int position, View convertView, ViewGroup parent) {
        TextView textView = convertView.findViewById(R.id.text);
        textView.setText("第" + position + "行");
        return convertView;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public int getHeight(int index) {
        return 400;
    }
}
