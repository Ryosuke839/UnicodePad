package jp.ddo.hotmist.unicodepad;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;

class FavoriteAdapter extends UnicodeAdapter {
    private ArrayList<Integer> list;
    private ArrayList<Integer> temp;
    boolean sorted;

    FavoriteAdapter(SharedPreferences pref, NameDatabase db, boolean single) {
        super(db, single);
        sorted = pref.getBoolean("sort_favorites", true);

        list = new ArrayList<>();
        temp = list;
        String fav = pref.getString("fav", "");
        for (int i = 0; i < fav.length(); ++i) {
            int code = fav.codePointAt(i);
	        if (code > 0xFFFF) {
		        ++i;
	        }
            list.add(code);
        }
    }

    @Override
    int name() {
        return R.string.favorite;
    }

    @Override
    void show() {
        truncate();
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
        return temp.get(arg0);
    }

    void add(int code) {
        list.remove(Integer.valueOf(code));
        list.add(code);
        if (sorted) {
            Collections.sort(list);
            // Collections.reverse(list);
        }
    }

    void remove(int code) {
        list.remove(Integer.valueOf(code));
    }

    void clear() {
        list.clear();
    }

    private void commit() {
        if (list != temp) {
            temp = list;
        }
    }

    private void truncate() {
	    if (list == temp) {
		    temp = new ArrayList<>(list);
	    }
    }

    boolean isFavorited(int code) {
        return list.contains(code);
    }

    @Override
    void save(SharedPreferences.Editor editor) {
        StringBuilder str = new StringBuilder();
	    for (Integer i : list) {
		    str.append(String.valueOf(Character.toChars(i)));
	    }
        editor.putString("fav", str.toString());
    }

}
