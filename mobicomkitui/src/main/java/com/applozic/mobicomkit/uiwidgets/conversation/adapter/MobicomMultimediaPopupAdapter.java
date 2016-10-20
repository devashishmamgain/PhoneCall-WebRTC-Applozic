package com.applozic.mobicomkit.uiwidgets.conversation.adapter;

import android.content.Context;
import android.graphics.Typeface;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.applozic.mobicomkit.uiwidgets.ApplozicSetting;
import com.applozic.mobicomkit.uiwidgets.R;

/**
 * Created by reytum on 18/3/16.
 */
public class MobicomMultimediaPopupAdapter extends BaseAdapter {
    Context context;
    String[] multimediaIcons;
    String[] multimediaText;

    public MobicomMultimediaPopupAdapter(Context context, String[] multimediaIcons, String[] multimediaText) {
        this.context = context;
        this.multimediaIcons = multimediaIcons;
        this.multimediaText = multimediaText;
    }

    @Override
    public int getCount() {
        return multimediaText.length;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.mobicom_individual_multimedia_option_item, null);

        TextView icon = (TextView) convertView.findViewById(R.id.mobicom_multimedia_icon);
        Typeface iconTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/fontawesome-webfont.ttf");
        icon.setTypeface(iconTypeface);
        TextView text = (TextView) convertView.findViewById(R.id.mobicom_multimedia_text);
        icon.setTextColor(ContextCompat.getColor(context, ApplozicSetting.getInstance(context).getAttachmentIconsBackgroundColor()));
        icon.setText(multimediaIcons[position]);
        text.setText(multimediaText[position]);
        return convertView;
    }

}
