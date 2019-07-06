package zhet.youplay.adapter;

import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.palashbansal.musicalyoutube.VideoItem;
import com.squareup.picasso.Picasso;

import zhet.youplay.controller.PlaybackController;

public class PlaybackQueueAdapter extends RecyclerView.Adapter<PlaybackQueueAdapter.ViewHolder> {

    public PlaybackQueueAdapter() {
    }

    @Override
    public PlaybackQueueAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(com.palashbansal.musicalyoutube.R.layout.video_item, parent, false);
        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final PlaybackController controller = PlaybackController.getInstance(holder.title.getContext());
        final VideoItem video = controller.getPlaylist().getVideos().get(position);
        Picasso.get().load(video.getThumbnailURL()).into(holder.thumbnail);
        holder.title.setText(video.getTitle());
        holder.channelName.setText(video.getChannelTitle());
        holder.optionsButton.setVisibility(View.INVISIBLE);
        if (controller.getCurrentPosition() == position) {
            holder.container.setBackgroundColor(ResourcesCompat.getColor(holder.title.getContext().getResources(), com.palashbansal.musicalyoutube.R.color.colorPrimaryDark, null));
        } else {
            holder.container.setBackgroundColor(ResourcesCompat.getColor(holder.title.getContext().getResources(), com.palashbansal.musicalyoutube.R.color.colorPrimary, null));
        }
        holder.container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.playAtPosition(holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        PlaybackController controller = PlaybackController.getInstance(null);
        if (controller == null || controller.getPlaylist() == null) return 0;
        return controller.getPlaylist().getVideos().size();
    }

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public ImageView thumbnail;
        public TextView title;
        public TextView channelName;
        public ImageView optionsButton;
        public FrameLayout container;

        //		public ProgressBar progressBar;
        public ViewHolder(View v) {
            super(v);
            thumbnail = v.findViewById(com.palashbansal.musicalyoutube.R.id.video_thumbnail);
            title = v.findViewById(com.palashbansal.musicalyoutube.R.id.video_title);
            channelName = v.findViewById(com.palashbansal.musicalyoutube.R.id.video_channel);
            optionsButton = v.findViewById(com.palashbansal.musicalyoutube.R.id.video_options);
            container = v.findViewById(com.palashbansal.musicalyoutube.R.id.main_video_container);
        }
    }
}