package com.ivyshare.ui.main.contact;

import java.util.List;

import com.ivyshare.engin.im.Person;

public interface AdapterState {
    public static final int STATE_ACTIE = 0;
    public static final int STATE_BUSY = 1;		// entering
    public static final int STATE_DEACTIVE = 2;
    public static final int STATE_EXITING = 3;

    public void active(List<Person> persons);
    public void deactive();
    public void busy();
    public void exiting();

    public void active(List<Person> persons, String id);
    public void deactive(String id);
    public void busy(String id);
    public void exiting(String id);

    public boolean isActive();
    public boolean isDeactive();
    public boolean isBusying();
    public boolean isExiting();
    public void setData(List<Person> persons);
}
