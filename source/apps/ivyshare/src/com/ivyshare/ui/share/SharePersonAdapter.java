package com.ivyshare.ui.share;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.ivy.ivyengine.control.ImManager;
import com.ivy.ivyengine.control.LocalSetting;
import com.ivy.ivyengine.control.LocalSetting.UserIconEnvironment;
import com.ivy.ivyengine.control.PersonManager;
import com.ivy.ivyengine.im.Person;
import com.ivyshare.R;
import com.ivyshare.ui.main.QuickPersonInfoActivity;
import com.ivyshare.util.CommonUtils;

public class SharePersonAdapter extends BaseAdapter {
		private static final String TAG = SharePersonAdapter.class.getSimpleName();

        private Context mContext;
        private List<Person> mListPersons;
        private List<Person> mListSelectPersons = null;
        
        private ImManager mImManager = null;

        public SharePersonAdapter(Context context, List<Person> listPersons, ImManager imManager) {
            
            Log.d(TAG, "PersonAdapter construct");
            mContext = context;
            mImManager = imManager;

            mListPersons = listPersons;
            
            
            mListSelectPersons = new ArrayList<Person>();
        }
        
        public void ChangeList(List<Person> listPersons) {
            mListPersons = listPersons;
        }

        @Override
        public int getCount() {
            return mListPersons.size();
        }

        @Override
        public Object getItem(int arg0) {
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            return arg0;    
        }
        
        private void addSelectPerson(Person person) {
            if( mListSelectPersons.contains(person) != true ){
                mListSelectPersons.add(person);
            }
        }

        private void subbSelectPerson(Person person) {
            if( mListSelectPersons.contains(person) == true ){
                mListSelectPersons.remove(person);
            }
        }
        
        private boolean checkSelectPerson(Person person) {
            if( mListSelectPersons.contains(person) == true ){
                return true;
            }else{
                return false;
            }
        }
        
        public List<Person> getSelectItem() {
            return mListSelectPersons;
        }
        
        @Override
        public View getView(final int position, View view, ViewGroup arg2) {
            if (position < 0 || position >= getCount()) {
                return null;
            }
            
            ViewClass myClass = null;
            if(view == null) {
                LayoutInflater factory = LayoutInflater.from(mContext);
                view = factory.inflate(R.layout.share_listview_persons , null);

                myClass = new ViewClass();
                myClass.image = (ImageView)view.findViewById(R.id.photo);
                myClass.name = (TextView)view.findViewById(R.id.share_persons_name);
                myClass.ip = (TextView)view.findViewById(R.id.share_persons_ip);
                myClass.checkbox = (CheckBox)view.findViewById(R.id.checkbox_person);
                myClass.layout = (LinearLayout)view.findViewById(R.id.listitem);
                view.setTag(myClass);
            } else {
                myClass = (ViewClass)view.getTag();
            }
            
            myClass.image.setOnClickListener(new MyOnclickListener(position, R.id.photo));
            myClass.layout.setOnClickListener(new MyOnclickListener(position, R.id.listitem));
            
            Person person = null;

            int pos = position;
            if (pos >=0 && pos < mListPersons.size()) {
                person = mListPersons.get(pos);
            }
            
            if(checkSelectPerson(person)){
                myClass.checkbox.setChecked(true);
            }else{
                myClass.checkbox.setChecked(false);
            }
            
            myClass.checkbox.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean checked = ((CheckBox)v).isChecked();
                    if(checked){
                        addSelectPerson(mListPersons.get(position));
                    }else{
                        subbSelectPerson(mListPersons.get(position));
                    }
                }
            });
            
            showPerson(myClass, person);

            return view;
        }
        
        private void showPerson(ViewClass myClass, Person person) {
            if (person == null) {
                return;
            }

            UserIconEnvironment userIconEnvironment = LocalSetting.getInstance().getUserIconEnvironment();
            if (person.mImage != null && userIconEnvironment.isExistHead(person.mImage, -1)) {
                String headimagepath = userIconEnvironment.getFriendHeadFullPath(person.mImage);
                // Log.d(TAG, "image name = " + person.mImage + ", nickname = " + person.mNickName + ", headimagepath = " + headimagepath);
                Bitmap bitmap = CommonUtils.DecodeBitmap(headimagepath, 256*256);
                if (bitmap != null) {
                    myClass.image.setImageBitmap(bitmap);
                }
            } else {
                myClass.image.setImageResource(R.drawable.ic_contact_picture_holo_light);
            }

            myClass.name.setText(person.mNickName);
            if (person.mIP != null) {
                myClass.ip.setText(person.mIP.getHostAddress());
            }
            
        }
        
        class ViewClass {       
            ImageView image;
            TextView name;
            TextView ip;
            CheckBox checkbox;
            LinearLayout layout;
        }
        
        private final class MyOnclickListener implements OnClickListener{
            private int position;
            private int id;
            
            public MyOnclickListener(int position, int id){
                this.position = position;
                this.id = id;
            }
            
            @Override
            public void onClick(View v) {
                onClicked(v, position, id);
            }
            
        }
        
        public void onClicked(View v, int position, int id) {
            switch (id) {
            case R.id.photo:{
                Intent intent = new Intent();
                String key = null;
                
                key = PersonManager.getPersonKey((Person)mListPersons.get(position));
                
                if (key != null && mImManager != null && mImManager.getPerson(key) != null) {
                    intent.putExtra("chatpersonKey", key);
                    intent.setClass(mContext, QuickPersonInfoActivity.class);
                    mContext.startActivity(intent);
                    ((Activity)mContext).overridePendingTransition(R.anim.zoomin, R.anim.zoomin);
                }
            }
            break;
            case R.id.listitem:{
                Log.e(TAG, "listitem");
                CheckBox checkBox = (CheckBox) v.findViewById(R.id.checkbox_person);
                if( checkBox.isChecked() == true ){
                    checkBox.setChecked(false);
                    subbSelectPerson(mListPersons.get(position));
                }else{
                    checkBox.setChecked(true);
                    addSelectPerson(mListPersons.get(position));
                }
            }
            break;
            }
        }
        
}
