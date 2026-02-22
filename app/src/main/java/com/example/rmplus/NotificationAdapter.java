package com.example.rmplus;

import android.content.Context;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

public class NotificationAdapter extends BaseAdapter {

    Context context;
    ArrayList<String[]> data;

    public NotificationAdapter(Context context, ArrayList<String[]> data) {
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

        title.setText(data.get(i)[0]);
        message.setText(data.get(i)[1]);
        time.setText(data.get(i)[2]);

        if (data.get(i)[2].isEmpty()) {
            time.setVisibility(View.GONE);
        } else {
            time.setVisibility(View.VISIBLE);
        }

        return view;
    }
}
