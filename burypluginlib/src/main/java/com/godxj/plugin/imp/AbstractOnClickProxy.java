package com.godxj.plugin.imp;

import android.content.DialogInterface;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 点击事件同步代理方法
 * 当点击某个空间触发了相应的事件，将同步调用此接口相同的方法
 * 并且同时调用clickProxy 该方法并将父级路径作为参数传回
 */
public abstract class AbstractOnClickProxy implements
        View.OnTouchListener,
        View.OnClickListener,
        View.OnLongClickListener,
        DialogInterface.OnClickListener,
        DialogInterface.OnCancelListener,
        DialogInterface.OnShowListener,
        DialogInterface.OnDismissListener,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener,
        AdapterView.OnItemSelectedListener,
        ExpandableListView.OnGroupClickListener,
        ExpandableListView.OnGroupExpandListener,
        ExpandableListView.OnChildClickListener,
        RatingBar.OnRatingBarChangeListener,
        RadioGroup.OnCheckedChangeListener,
        CompoundButton.OnCheckedChangeListener {


    /**
     * 被点击的后的代理方法，该方法作为统一处理方法，也可以单独对以上的方法做处理
     * 该方法将一些路径信息作为参数返回给使用者方便做路径统计
     * 该路径规范如下：
     *
     * @param viewPath 点击的view的父级路径
     */
    public abstract void clickProxy(String viewPath);

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP){
            clickProxy(getViewPath(v));
        }
        return false;
    }

    @Override
    public void onCancel(DialogInterface dialog) {

    }

    @Override
    public void onClick(DialogInterface dialog, int which) {

    }

    @Override
    public void onDismiss(DialogInterface dialog) {

    }

    @Override
    public void onShow(DialogInterface dialog) {
    }

    @Override
    public void onClick(View v) {
        clickProxy(getViewPath(v));
    }

    @Override
    public boolean onLongClick(View v) {
        clickProxy(getViewPath(v));
        return false;
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        clickProxy(getViewPath(view));
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        clickProxy(getViewPath(view));
        return false;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (isChecked){
            clickProxy(getViewPath(buttonView));
        }
    }

    @Override
    public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
        clickProxy(getViewPath(v));
        return false;
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        clickProxy(getViewPath(v));
        return false;
    }

    @Override
    public void onGroupExpand(int groupPosition) {

    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        clickProxy(getViewPath(group));
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        clickProxy(getViewPath(ratingBar));
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        clickProxy(getViewPath(view));
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    /**
     * 获取view的层级
     * @param v
     */
    private String getViewPath(View v){
        StringBuilder stringBuilder = new StringBuilder();
        if (v.getId() != View.NO_ID){
            stringBuilder.append(v.getContext().getResources().getResourceEntryName(v.getId()));
            stringBuilder.insert(0, "#");
        }
        List<String> text = getText(v);
        if (text.size() == 1){
            stringBuilder.append("[");
            stringBuilder.append(text.get(0));
            stringBuilder.append("]");
        }
        stringBuilder.insert(0,  getName(v)).insert(0, "/");
        do{
            v = (View) v.getParent();
            if(android.R.id.content == v.getId()){
                break;
            }else {
                stringBuilder.insert(0, getName(v)).insert(0, "/");
            }
        }while (v.getParent() instanceof View);
        stringBuilder.replace(0,1,"");
        String viewPath = stringBuilder.toString();
        viewPath = viewPath.replaceAll("/\\{","{");
        return viewPath;
    }

    private String getName(View v){
        String name = v.getClass().getSimpleName();
        if (v.getParent() instanceof ViewGroup){
           int position = ((ViewGroup) v.getParent()).indexOfChild(v);
            name+="["+position+"]";
        }
        if (v instanceof ViewPager){
            name += "[" + ((ViewPager) v).getCurrentItem() + "]";
        }else if(v.getParent() instanceof AdapterView ){
            name = "{Item["+((AdapterView) v.getParent()).getPositionForView(v)+"]}";
        }
        return name;
    }

    /**
     * 获取当前类所有父类
     *
     * 从android.view.View 该出算起
     * @param clazz
     * @return
     */
    private List<Class<View>> getSuperClass(Class<View> clazz){
        List<Class<View>> listSuperClass = new ArrayList<>();
        listSuperClass.add(clazz);
        Class superclass = clazz.getSuperclass();
        while (superclass != null) {
            if(superclass.getName().equals("android.view.View")){
                break;
            }
            listSuperClass.add(superclass);
            superclass = superclass.getSuperclass();
        }
        return listSuperClass;
    }


    /**
     * 获取文字
     * 布局中只有一个是TextView情况，不论布局复杂度，只要点击布局只包含一个TextView就当可以获取文字
     * @param v
     * @return
     */
    private ArrayList getText(View v){
        ArrayList text= new ArrayList();
        //如果直接从TextView继承过来的 可以直接获取到这个文本
        try {
            TextView tv = (TextView) v;
            text.add(tv.getText().toString());
            return text;
        }catch (Exception e){
        }
        //某些布局是针对父布局进行点击操作，而子布局只有一个TextView的情况
        if (v instanceof ViewGroup){
            ViewGroup viewGroup = (ViewGroup) v;
            int count = viewGroup.getChildCount();
            for (int i = 0; i < count; i++){
               View view = viewGroup.getChildAt(i);
               text.addAll(getText(view));
            }
        }
        return text;
    }
}
