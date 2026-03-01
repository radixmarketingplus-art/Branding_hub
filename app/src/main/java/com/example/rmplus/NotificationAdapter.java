package com.example.rmplus;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class NotificationAdapter extends BaseAdapter {

    Context context;
    ArrayList<NotificationModel> data;

    public NotificationAdapter(Context context, ArrayList<NotificationModel> data) {
        this.context = context;
        this.data = data;
    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int i) {
        return data.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup parent) {

        if (view == null) {
            view = LayoutInflater.from(context)
                    .inflate(R.layout.item_notification, parent, false);
        }

        TextView title = view.findViewById(R.id.txtTitle);
        TextView message = view.findViewById(R.id.txtMessage);
        TextView time = view.findViewById(R.id.txtTime);

        NotificationModel model = data.get(i);

        title.setText(model.title);
        message.setText(model.message);

        if (model.time > 0) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.getDefault());
            time.setText(sdf.format(new java.util.Date(model.time)));
            time.setVisibility(View.VISIBLE);
        } else {
            time.setVisibility(View.GONE);
        }

        return view;
    }
}
