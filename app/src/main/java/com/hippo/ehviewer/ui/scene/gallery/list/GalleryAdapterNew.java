/*
 * Copyright 2021 xiaojieonly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hippo.ehviewer.ui.scene.gallery.list;
import okhttp3.*;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import org.json.JSONArray;
import org.json.JSONObject;
import com.hippo.drawable.TriangleDrawable;
import com.hippo.easyrecyclerview.MarginItemDecoration;
import com.hippo.ehviewer.EhApplication;
import com.hippo.ehviewer.R;
import com.hippo.ehviewer.Settings;
import com.hippo.ehviewer.client.EhCacheKeyFactory;
import com.hippo.ehviewer.client.EhUtils;
import com.hippo.ehviewer.client.data.GalleryInfo;
import com.hippo.ehviewer.download.DownloadManager;
import com.hippo.ehviewer.ui.scene.TransitionNameFactory;
import com.hippo.ehviewer.widget.SimpleRatingView;
import com.hippo.ehviewer.widget.TileThumbNew;
import com.hippo.widget.LoadImageViewNew;
import com.hippo.widget.recyclerview.AutoStaggeredGridLayoutManager;
import com.hippo.yorozuya.ViewUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.content.Context;

import com.hippo.ehviewer.spider.SpiderQueen;

abstract class GalleryAdapterNew extends RecyclerView.Adapter<GalleryAdapterNew.GalleryHolder> {

    @IntDef({TYPE_LIST, TYPE_GRID})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Type {
    }

    public static final int TYPE_INVALID = -1;
    public static final int TYPE_LIST = 0;
    public static final int TYPE_GRID = 1;

    private final LayoutInflater mInflater;
    private final Resources mResources;
    private final RecyclerView mRecyclerView;
    private final AutoStaggeredGridLayoutManager mLayoutManager;
    private final int mPaddingTopSB;
    private MarginItemDecoration mListDecoration;
    private MarginItemDecoration mGirdDecoration;
    private final int mListThumbWidth;
    private final int mListThumbHeight;
    private int mType = TYPE_INVALID;
    private boolean mShowFavourite;
    private OnThumbItemClickListener myOnThumbItemClickListener;

    private DownloadManager mDownloadManager;

    private final ExecutorService executor;
    private final Handler handler = new Handler(Looper.getMainLooper());


    public GalleryAdapterNew(@NonNull LayoutInflater inflater, @NonNull Resources resources,
                             @NonNull RecyclerView recyclerView, int type, boolean showFavourited, ExecutorService executor) {
        this.executor = executor;
        mInflater = inflater;
        mResources = resources;
        mRecyclerView = recyclerView;
        mLayoutManager = new AutoStaggeredGridLayoutManager(0, StaggeredGridLayoutManager.VERTICAL);
        mPaddingTopSB = resources.getDimensionPixelOffset(R.dimen.gallery_padding_top_search_bar);
        mShowFavourite = showFavourited;

        mRecyclerView.setAdapter(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        View calculator = inflater.inflate(R.layout.item_gallery_list_thumb_height, null);
        ViewUtils.measureView(calculator, 1024, ViewGroup.LayoutParams.WRAP_CONTENT);
        mListThumbHeight = calculator.getMeasuredHeight();
        mListThumbWidth = mListThumbHeight * 2 / 3;

        setType(type);

        mDownloadManager = EhApplication.getDownloadManager(inflater.getContext());
    }

    private void adjustPadding() {
        RecyclerView recyclerView = mRecyclerView;
        recyclerView.setPadding(recyclerView.getPaddingLeft(), recyclerView.getPaddingTop() + mPaddingTopSB,
                recyclerView.getPaddingRight(), recyclerView.getPaddingBottom());
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        if (type == mType) {
            return;
        }
        mType = type;

        RecyclerView recyclerView = mRecyclerView;
        switch (type) {
            default:
            case GalleryAdapterNew.TYPE_LIST: {
                int columnWidth = mResources.getDimensionPixelOffset(Settings.getDetailSizeResId());
                mLayoutManager.setColumnSize(columnWidth);
                mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_MIN_SIZE);
                if (null != mGirdDecoration) {
                    recyclerView.removeItemDecoration(mGirdDecoration);
                }
                if (null == mListDecoration) {
                    int interval = mResources.getDimensionPixelOffset(R.dimen.gallery_list_interval);
                    int paddingH = mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_h);
                    int paddingV = mResources.getDimensionPixelOffset(R.dimen.gallery_list_margin_v);
                    mListDecoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
                }
                recyclerView.addItemDecoration(mListDecoration);
                mListDecoration.applyPaddings(recyclerView);
                adjustPadding();
                notifyDataSetChanged();
                break;
            }
            case GalleryAdapterNew.TYPE_GRID: {
                int columnWidth = mResources.getDimensionPixelOffset(Settings.getThumbSizeResId());
                mLayoutManager.setColumnSize(columnWidth);
                mLayoutManager.setStrategy(AutoStaggeredGridLayoutManager.STRATEGY_SUITABLE_SIZE);
                if (null != mListDecoration) {
                    recyclerView.removeItemDecoration(mListDecoration);
                }
                if (null == mGirdDecoration) {
                    int interval = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_interval);
                    int paddingH = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_h);
                    int paddingV = mResources.getDimensionPixelOffset(R.dimen.gallery_grid_margin_v);
                    mGirdDecoration = new MarginItemDecoration(interval, paddingH, paddingV, paddingH, paddingV);
                }
                recyclerView.addItemDecoration(mGirdDecoration);
                mGirdDecoration.applyPaddings(recyclerView);
                adjustPadding();
                notifyDataSetChanged();
                break;
            }
        }
    }

    @Override
    public GalleryAdapterNew.GalleryHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int layoutId;
        switch (viewType) {
            default:
            case TYPE_LIST:
                layoutId = R.layout.item_gallery_list_new;
                break;
            case TYPE_GRID:
                layoutId = R.layout.item_gallery_grid_new;
                break;
        }

        GalleryAdapterNew.GalleryHolder holder = new GalleryAdapterNew.GalleryHolder(mInflater.inflate(layoutId, parent, false), myOnThumbItemClickListener, viewType);

        if (viewType == TYPE_LIST) {
            ViewGroup.LayoutParams lp = holder.thumb.getLayoutParams();
            lp.width = mListThumbWidth;
            lp.height = mListThumbHeight;
            holder.thumb.setLayoutParams(lp);
        }

        return holder;
    }

    @Override
    public int getItemViewType(int position) {
        return mType;
    }

    @Nullable
    public GalleryInfo getDataAt(int position) {
        return null;
    }

    private String commandResponse(String text) {
        String server_ip = "118.31.66.122";
        int port = 8189;
        String urlStr = "http://" + server_ip + ":" + port + "/taskflow/ehcls";

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");

        // 创建JSON对象
        JSONObject requestData = new JSONObject();
        try {
            JSONObject dataObject = new JSONObject();
            dataObject.put("text", text);
            requestData.put("data", dataObject);

            // 创建请求体
            RequestBody requestBody = RequestBody.create(mediaType, requestData.toString());

            // 创建请求
            Request request = new Request.Builder()
                    .url(urlStr)
                    .post(requestBody)
                    .build();

            // 发送请求
            Response response = client.newCall(request).execute();

            // 获取响应体的字符串
            String responseBody = response.body().string();

            // 解析返回的JSON
            JSONObject responseObject = new JSONObject(responseBody);
            JSONArray resultArray = responseObject.getJSONArray("result");
            JSONObject predictionsObject = resultArray.getJSONObject(0).getJSONArray("predictions").getJSONObject(0);

            // 获取label字段值
            String label = predictionsObject.getString("label");

            return label;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    @Override
    public void onBindViewHolder(GalleryAdapterNew.GalleryHolder holder, int position) {
        GalleryInfo gi = getDataAt(position);
        if (null == gi) {
            return;
        }
        final String misCommand = gi.title + "," + gi.titleJpn + "," + Arrays.toString(gi.simpleTags);


//        if(misCommand == null) {
//            misCommand = "null";
//        }

        switch (mType) {
            default:
            case TYPE_LIST: {
                // 使用Handler在新线程执行完后通知主线程
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        // 在线程中执行请求
                        String updatedMisCommand = commandResponse(misCommand);

                        // 使用Handler发送消息通知主线程
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                String finalMisCommand = (updatedMisCommand != null) ? updatedMisCommand : "null";
                                // 在这里执行主线程的操作，例如更新UI，使用finalMisCommand
                                Log.d("commandResponse: ", finalMisCommand);
                                holder.isCommand.setTextColor(Color.RED); // 设置字体颜色为红色
                                holder.isCommand.setText(finalMisCommand);
                                holder.isCommand.setVisibility(View.VISIBLE);

                            }
                        });
                    }
                }).start();


                holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
                holder.title.setText(EhUtils.getSuitableTitle(gi));
                holder.uploader.setText(gi.uploader);
                holder.rating.setRating(gi.rating);
                TextView category = holder.category;
                String newCategoryText = EhUtils.getCategory(gi.category);
                if (!newCategoryText.equals(category.getText().toString())) {
                    category.setText(newCategoryText);
                    category.setBackgroundColor(EhUtils.getCategoryColor(gi.category));
                }
                holder.posted.setText(gi.posted);
                if (gi.pages == 0 || !Settings.getShowGalleryPages()) {
                    holder.pages.setText(null);
                    holder.pages.setVisibility(View.GONE);
                } else {
                    executor.submit(()->{
                        int startPage = SpiderQueen.findStartPage(mInflater.getContext(), gi);
                        handler.post(()->{
                            String text;
                            if (startPage > 0) {
                                text = startPage + 1 + "/" + gi.pages + "P";
                                holder.pages.setText(text);
                            } else {
                                text = "0/" + gi.pages + "P";
                                holder.pages.setText(text);
                            }
                        });
                    });
                    holder.pages.setText(new StringBuffer(gi.pages + "P"));
                    holder.pages.setVisibility(View.VISIBLE);
                }
                if (TextUtils.isEmpty(gi.simpleLanguage)) {
                    holder.simpleLanguage.setText(null);
                    holder.simpleLanguage.setVisibility(View.GONE);
                } else {
                    holder.simpleLanguage.setText(gi.simpleLanguage);
                    holder.simpleLanguage.setVisibility(View.VISIBLE);
                }
                holder.favourite.setVisibility((mShowFavourite && gi.favoriteSlot >= -1 && gi.favoriteSlot <= 10) ? View.VISIBLE : View.GONE);
                holder.downloaded.setVisibility(mDownloadManager.containDownloadInfo(gi.gid) ? View.VISIBLE : View.GONE);
                break;
            }
            case TYPE_GRID: {
                ((TileThumbNew) holder.thumb).setThumbSize(gi.thumbWidth, gi.thumbHeight);
                holder.thumb.load(EhCacheKeyFactory.getThumbKey(gi.gid), gi.thumb);
                View category = holder.category;
                Drawable drawable = category.getBackground();
                int color = EhUtils.getCategoryColor(gi.category);
                if (!(drawable instanceof TriangleDrawable)) {
                    drawable = new TriangleDrawable(color);
                    category.setBackground(drawable);
                } else {
                    ((TriangleDrawable) drawable).setColor(color);
                }
                holder.simpleLanguage.setText(gi.simpleLanguage);
                break;
            }
        }
        ViewCompat.setTransitionName(holder.thumb, TransitionNameFactory.getThumbTransitionName(gi.gid));
    }

    public void setThumbItemClickListener(OnThumbItemClickListener listener) {
        myOnThumbItemClickListener = listener;
    }

    public interface OnThumbItemClickListener {
        void onThumbItemClick(int position, View view, GalleryInfo gi);
    }

    public class GalleryHolder extends RecyclerView.ViewHolder {

        public final LoadImageViewNew thumb;
        public TextView title;
        public final TextView uploader;
        public final SimpleRatingView rating;
        public final TextView category;
        public final TextView posted;
        public final TextView pages;
        public final TextView isCommand;

        public final TextView simpleLanguage;
        public final ImageView favourite;
        public final ImageView downloaded;

        public GalleryHolder(View itemView, final OnThumbItemClickListener onThumbItemClickListener, int mType) {
            super(itemView);
            thumb = itemView.findViewById(R.id.thumb_new);
            title = itemView.findViewById(R.id.title);
            uploader = itemView.findViewById(R.id.uploader);
            rating = itemView.findViewById(R.id.rating);
            category = itemView.findViewById(R.id.category);
            posted = itemView.findViewById(R.id.posted);
            pages = itemView.findViewById(R.id.pages);

            isCommand = itemView.findViewById(R.id.is_command);

            simpleLanguage = itemView.findViewById(R.id.simple_language);
            favourite = itemView.findViewById(R.id.favourited);
            downloaded = itemView.findViewById(R.id.downloaded);
            if (mType == 0) {
                thumb.setOnClickListener(v -> {
                    if (onThumbItemClickListener != null) {
                        int position = getBindingAdapterPosition();
                        onThumbItemClickListener.onThumbItemClick(position, itemView, getDataAt(position));
                    }
                });
            }
        }

    }

}
