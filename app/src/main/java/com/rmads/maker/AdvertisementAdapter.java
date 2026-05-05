package com.rmads.maker;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.cache.CacheDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.io.File;
import java.util.ArrayList;

@UnstableApi
public class AdvertisementAdapter
        extends RecyclerView.Adapter<AdvertisementAdapter.VH> {

    ArrayList<AdvertisementItem> list;
    int highlightPos = -1;
    private static boolean isMuted = true; // Default to muted
    private final ArrayList<ExoPlayer> activePlayers = new ArrayList<>();
    private int currentActivePos = -1;

    public interface OnMediaStateListener {
        void onVideoStarted();
        void onVideoEnded();
        void onVideoError();
    }

    private OnMediaStateListener listener;

    public void setListener(OnMediaStateListener l) {
        this.listener = l;
    }

    public void setHighlightPos(int pos) {
        this.highlightPos = pos;
        notifyDataSetChanged();
    }

    public void updateActivePosition(int pos) {
        int oldPos = currentActivePos;
        currentActivePos = pos;
        if (oldPos != -1) notifyItemChanged(oldPos);
        if (currentActivePos != -1) notifyItemChanged(currentActivePos);
    }

    public void onPause() {
        for (ExoPlayer p : activePlayers) {
            if (p != null) p.pause();
        }
    }

    public void onResume() {
        // Players will resume via onBind or if they were already playing
    }

    public void releaseAll() {
        for (ExoPlayer p : activePlayers) {
            if (p != null) p.release();
        }
        activePlayers.clear();
    }

    public AdvertisementAdapter(ArrayList<AdvertisementItem> list) {
        this.list = list;
    }

    @Override
    public VH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_trending_hotstar, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(VH h, int position) {
        AdvertisementItem item = list.get(position);

        boolean isVideo = item.type != null ? item.type.equalsIgnoreCase("video") : (item.imagePath != null && (
                item.imagePath.toLowerCase().contains(".mp4") ||
                item.imagePath.toLowerCase().contains(".mkv") ||
                item.imagePath.toLowerCase().contains(".webm") ||
                item.imagePath.toLowerCase().contains(".mov") ||
                item.imagePath.toLowerCase().contains(".3gp")
        ));

        if (isVideo) {
            h.img.setVisibility(View.VISIBLE); // Show thumbnail while loading
            h.videoPlayer.setVisibility(View.VISIBLE);
            h.btnVolume.setVisibility(View.VISIBLE);
            
            // 🚀 Optimized ExoPlayer Setup for fast start (Reduced buffering)
            if (h.exoPlayer == null) {
                androidx.media3.exoplayer.DefaultLoadControl loadControl = new androidx.media3.exoplayer.DefaultLoadControl.Builder()
                        .setBufferDurationsMs(
                                1500, // minBufferMs
                                5000, // maxBufferMs
                                500,  // bufferForPlaybackMs
                                1000  // bufferForPlaybackAfterRebufferMs
                        ).build();
                h.exoPlayer = new ExoPlayer.Builder(h.itemView.getContext())
                        .setLoadControl(loadControl)
                        .build();
                h.exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF); // Play once then slide
                activePlayers.add(h.exoPlayer);
                
                h.exoPlayer.addListener(new Player.Listener() {
                    @Override
                    public void onPlaybackStateChanged(int state) {
                        if (state == Player.STATE_BUFFERING) {
                            h.pbLoading.setVisibility(View.VISIBLE);
                        } else if (state == Player.STATE_READY) {
                            h.pbLoading.setVisibility(View.GONE);
                            h.img.setVisibility(View.GONE); // Hide thumbnail when video is ready
                            if (listener != null && h.getBindingAdapterPosition() == currentActivePos) {
                                listener.onVideoStarted();
                            }
                        } else if (state == Player.STATE_ENDED) {
                            if (listener != null && h.getBindingAdapterPosition() == currentActivePos) {
                                listener.onVideoEnded();
                            }
                        }
                    }

                    @Override
                    public void onPlayerError(@NonNull androidx.media3.common.PlaybackException error) {
                        h.pbLoading.setVisibility(View.GONE);
                        if (listener != null && h.getBindingAdapterPosition() == currentActivePos) {
                            listener.onVideoError();
                        }
                    }
                });
            }
            h.videoPlayer.setPlayer(h.exoPlayer);

            String currentPath = (String) h.videoPlayer.getTag();
            if (item.imagePath != null && (!item.imagePath.equals(currentPath) || h.exoPlayer.getPlaybackState() == Player.STATE_IDLE)) {
                MediaItem mediaItem = MediaItem.fromUri(item.imagePath);
                h.exoPlayer.setMediaItem(mediaItem);
                h.exoPlayer.prepare();
                h.videoPlayer.setTag(item.imagePath);
            }
            
            h.btnVolume.setImageResource(isMuted ? R.drawable.ic_volume_off : R.drawable.ic_volume_up);
            
            // 🖼️ Load thumbnail for video immediately
            Glide.with(h.img.getContext())
                    .load(item.imagePath)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(h.img);
            
            // 🔊 Volume & Playback Control: Only play and unmute if this is the front (active) item
            if (position == currentActivePos) {
                h.exoPlayer.setVolume(isMuted ? 0f : 1f);
                h.exoPlayer.play();
                h.btnVolume.setVisibility(View.VISIBLE);
                
                // If the video was already prepared while off-screen, it won't fire STATE_READY again.
                // So we manually notify that it started playing now that it's on-screen.
                if (h.exoPlayer.getPlaybackState() == Player.STATE_READY) {
                    if (listener != null) listener.onVideoStarted();
                }
            } else {
                h.exoPlayer.setVolume(0f);
                h.exoPlayer.pause();
                h.btnVolume.setVisibility(View.GONE); // Hide volume button for background ads
            }

            h.btnVolume.setOnClickListener(v -> {
                isMuted = !isMuted;
                // Update all visible players' volume preference
                for (ExoPlayer p : activePlayers) {
                    if (p != null) {
                        // Only the active one should actually have volume
                        if (activePlayers.indexOf(p) == currentActivePos % list.size()) { // Approximate check
                             // This is tricky because activePlayers is just a list of players, not indexed by adapter position.
                             // But since we call notifyItemChanged, it will re-bind and set the right volume.
                        }
                    }
                }
                notifyDataSetChanged(); // Refresh to apply volume change to all
            });

        } else {
            h.img.setVisibility(View.VISIBLE);
            h.videoPlayer.setVisibility(View.GONE);
            h.btnVolume.setVisibility(View.GONE);
            h.pbLoading.setVisibility(View.GONE);
            if (h.exoPlayer != null) {
                h.exoPlayer.stop();
            }
            
            // 🚀 Performance: Use centerCrop and smaller thumbnail for carousel
            Glide.with(h.img.getContext())
                    .load(item.imagePath)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_report_image)
                    .centerCrop()
                    .override(600, 300) // Lower resolution for carousel items to save memory
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(h.img);
        }

        h.itemView.setOnClickListener(v -> {
            Context ctx = v.getContext();
            
            // 🌐 If it has a link, open external browser
            if (item.link != null && !item.link.isEmpty()) {
                String finalLink = item.link.trim();
                if (!finalLink.startsWith("http://") && !finalLink.startsWith("https://")) {
                    finalLink = "https://" + finalLink;
                }
                try {
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(finalLink));
                    ctx.startActivity(i);
                } catch (Exception ignored) {}
            }
            // 🎬 NO FALLBACK: Ads on home page are no longer clickable for preview
        });

        if (h.itemView instanceof com.google.android.material.card.MaterialCardView) {
            com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) h.itemView;
            if (highlightPos != -1 && highlightPos == position) {
                card.setStrokeColor(android.graphics.Color.parseColor("#4A6CF7"));
                card.setStrokeWidth(12);
                card.setCardElevation(20);
            } else {
                card.setStrokeWidth(0);
                card.setCardElevation(10);
            }
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VH holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.exoPlayer != null) {
            holder.exoPlayer.pause();
        }
    }

    @Override
    public void onViewRecycled(@NonNull VH holder) {
        super.onViewRecycled(holder);
        if (holder.exoPlayer != null) {
            activePlayers.remove(holder.exoPlayer);
            holder.exoPlayer.release();
            holder.exoPlayer = null;
        }
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView img, btnVolume;
        PlayerView videoPlayer;
        ExoPlayer exoPlayer;
        ProgressBar pbLoading;

        VH(View v) {
            super(v);
            img = v.findViewById(R.id.img);
            videoPlayer = v.findViewById(R.id.videoPlayer);
            btnVolume = v.findViewById(R.id.btnVolume);
            pbLoading = v.findViewById(R.id.pbLoading);
        }
    }
}
