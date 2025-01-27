package com.hjj.tvalist.presenter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.hjj.tvalist.R;
import com.hjj.tvalist.model.AlistResponse.Content;
import com.hjj.tvalist.util.FileUtils;

public class FileItemPresenter extends Presenter {
    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Content content);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_file, parent, false);
        
        // 添加点击监听
        view.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                Content content = (Content) v.getTag();
                onItemClickListener.onItemClick(content);
            }
        });

        // 添加焦点变化监听
        view.setOnFocusChangeListener((v, hasFocus) -> {
            View cardView = v.findViewById(R.id.card_view);
            if (hasFocus) {
                // 放大效果
                cardView.animate()
                    .scaleX(1.05f)
                    .scaleY(1.05f)
                    .setDuration(150)
                    .start();
            } else {
                // 恢复原始大小
                cardView.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(150)
                    .start();
            }
        });
        
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object item) {
        Content content = (Content) item;
        viewHolder.view.setTag(content);  // 保存内容到tag
        TextView titleView = viewHolder.view.findViewById(R.id.title);
        TextView infoView = viewHolder.view.findViewById(R.id.info);
        ImageView imageView = viewHolder.view.findViewById(R.id.image);
        View cardView = viewHolder.view.findViewById(R.id.card_view);

        titleView.setText(content.name);
        
        // 设置文件信息
        String info = content.is_dir ? "文件夹" : FileUtils.formatSize(Long.parseLong(content.size));
        infoView.setText(info);
        
        // 设置图标或缩略图
        if (content.is_dir) {
            imageView.setImageResource(R.drawable.ic_folder);
        } else if (FileUtils.isVideoFile(content.name)) {
            if (content.thumb != null && !content.thumb.isEmpty()) {
                Glide.with(viewHolder.view.getContext())
                    .load(content.thumb)
                    .placeholder(R.drawable.ic_video)
                    .error(R.drawable.ic_video)
                    .into(imageView);
            } else {
                imageView.setImageResource(R.drawable.ic_video);
            }
        } else {
            imageView.setImageResource(R.drawable.ic_file);
        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        ImageView imageView = viewHolder.view.findViewById(R.id.image);
        Glide.with(imageView.getContext()).clear(imageView);
    }
} 