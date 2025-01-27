package com.hjj.tvalist.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.leanback.widget.Presenter;
import com.hjj.tvalist.R;
import com.hjj.tvalist.model.MenuItem;

public class MenuItemPresenter extends Presenter {
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_menu, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        MenuItem menuItem = (MenuItem) item;
        View itemView = viewHolder.view;

        ImageView iconView = itemView.findViewById(R.id.menu_icon);
        TextView titleView = itemView.findViewById(R.id.menu_title);

        iconView.setImageResource(menuItem.getIconResId());
        titleView.setText(menuItem.getTitle());

        itemView.setOnClickListener(v -> menuItem.performAction());
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
    }
} 