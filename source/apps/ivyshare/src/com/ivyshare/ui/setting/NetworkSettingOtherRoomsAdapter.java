package com.ivyshare.ui.setting;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.ivyshare.MyApplication;
import com.ivyshare.R;

public class NetworkSettingOtherRoomsAdapter extends BaseAdapter {
    public class Room {
        public String name;
        public String ssid;
    }

    private Context mContext;
    private List<Room> mListRooms;
    
    public NetworkSettingOtherRoomsAdapter(Context context) {
        mContext = context;
        mListRooms = new ArrayList<Room>();
        Room room;
        
        room = new Room();
        room.name = "张志强";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "王亮";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "毕松";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "毕松2";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "毕松3";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "毕松4";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "毕松5";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "李海峰";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮2";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮3";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮4";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮5";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮6";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮7";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮8";
        mListRooms.add(room);
        
        room = new Room();
        room.name = "黄长亮9黄长亮9黄长亮9黄长亮9黄长亮9";
        mListRooms.add(room);
    }

    @Override
    public int getCount() {
        return mListRooms.size();
    }

    @Override
    public Object getItem(int arg0) {
        return mListRooms.get(arg0);
    }

    @Override
    public long getItemId(int arg0) {
        return arg0;
    }

    @Override
    public View getView(int position, View convertview, ViewGroup arg2) {
        View view = null;
        ViewHolder holder = null;
        if (convertview == null || convertview.getTag() == null) {
            view =  LayoutInflater.from(mContext).inflate(R.layout.list_app_item, null);
            holder = new ViewHolder(view);
            view.setTag(holder);
        } 
        else{
            view = convertview ;
            holder = (ViewHolder) convertview.getTag() ;
        }

        Room room = (Room) getItem(position);
        holder.ivIcon.setImageResource(R.drawable.room);
        holder.tvName.setText(MyApplication.getInstance().getString(R.string.network_setting_roomname, room.name));
        return view;
    }

    private class ViewHolder {
        ImageView ivIcon;
        TextView tvName;

        public ViewHolder(View view) {
            this.ivIcon = (ImageView) view.findViewById(R.id.imgApp);
            this.tvName = (TextView) view.findViewById(R.id.tvAppLabel);
        }
    }
}
