package jp.ddo.hotmist.unicodepad;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;

class RecentAdapter extends UnicodeAdapter {
    private ArrayList<Integer> list;
    private ArrayList<Integer> temp;
    boolean sorted;
    static int maxitems = 16;

    RecentAdapter(SharedPreferences pref, NameDatabase db, boolean single) {
        super(db, single);
	    sorted = pref.getBoolean("sort_recents", true);

	    list = new ArrayList<>();
        temp = list;
        String rec = pref.getString("rec", "");
        int num = 0;
        for (int i = 0; i < rec.length(); ++i) {
	        if (rec.codePointAt(i) > 0xFFFF) {
		        ++i;
	        }
            ++num;
        }
        for (int i = 0; i < rec.length(); ++i) {
            int code = rec.codePointAt(i);
	        if (code > 0xFFFF) {
		        ++i;
	        }
	        if (--num < maxitems) {
		        list.add(code);
	        }
        }
    }

    @Override
    int name() {
        return R.string.recent;
    }

    @Override
    void show() {
        trunc();
	    if (grid != null) {
		    grid.invalidateViews();
	    }
    }

    @Override
    void leave() {
        commit();
	    if (grid != null) {
		    grid.invalidateViews();
	    }
    }

    @Override
    public int getCount() {
        return temp.size();
    }

    @Override
    public long getItemId(int arg0) {
        return temp.get(temp.size() - arg0 - 1);
    }

    void add(int code) {
        list.remove(Integer.valueOf(code));
        list.add(code);
	    if (sorted) {
            Collections.sort(list);
            Collections.reverse(list);
        }
	    if (list.size() >= maxitems) {
		    list.remove(0);
	    }
    }

    void remove(int code) {
        list.remove(Integer.valueOf(code));
	    if (list != temp) {
		    temp.remove(Integer.valueOf(code));
	    }
	    if (grid != null) {
		    grid.invalidateViews();
	    }
    }

    void clear() {
        temp.clear();
        list.clear();
        if (grid != null) {
            grid.invalidateViews();
        }
    }

    private void commit() {
	    if (list != temp) {
		    temp = list;
	    }
    }

    private void trunc() {
	    if (list == temp) {
		    temp = new ArrayList<>(list);
	    }
    }

    @Override
    void save(SharedPreferences.Editor edit) {
        StringBuilder str = new StringBuilder();
	    for (Integer i : list) {
		    str.append(String.valueOf(Character.toChars(i)));
	    }
        edit.putString("rec", str.toString());
    }

}
