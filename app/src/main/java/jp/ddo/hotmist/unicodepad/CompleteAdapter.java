package jp.ddo.hotmist.unicodepad;


import java.util.ArrayList;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;


class CompleteAdapter extends BaseAdapter implements Filterable
{
	private final Object lock = new Object();
	private CompleteFilter filter = null;
	private ArrayList<String> list;
	private ArrayList<String> temp;
	private LayoutInflater inflater;
	private String current = "";

	CompleteAdapter(Context context, SharedPreferences pref)
	{
		list = new ArrayList<>();
		for (String s : pref.getString("comp", "").split("\n"))
			if (s.length() > 0)
				list.add(s);
		inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public void update(String str)
	{
		synchronized (lock)
		{
			for (String s : str.split(" "))
				if (s.length() > 0)
				{
					(temp == null ? list : temp).remove(s);
					if ((temp == null ? list : temp).size() == 255)
						(temp == null ? list : temp).remove(254);
					(temp == null ? list : temp).add(0, s);
				}
		}
		notifyDataSetChanged();
	}

	void save(SharedPreferences.Editor edit)
	{
		String str = "";
		for (String s : list)
			str += s + "\n";
		edit.putString("comp", str);
	}

	@Override
	public int getCount()
	{
		return list.size();
	}

	@Override
	public Object getItem(int position)
	{
		return current + list.get(position) + " ";
	}

	@Override
	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		View view = convertView == null ?
				inflater.inflate(R.layout.spinner_drop_down_item, parent, false) :
				convertView;

		((TextView)view).setText(list.get(position));

		return view;
	}

	@Override
	public Filter getFilter()
	{
		if (filter == null)
			filter = new CompleteFilter();
		return filter;
	}

	private class CompleteFilter extends Filter
	{
		@Override
		protected FilterResults performFiltering(CharSequence prefix)
		{
			FilterResults results = new FilterResults();

			if (temp == null)
			{
				synchronized (lock)
				{
					temp = new ArrayList<>(list);
				}
			}

			int idx = prefix == null ? -1 : prefix.toString().lastIndexOf(' ');

			if (prefix == null || prefix.length() == idx + 1)
			{
				ArrayList<String> res;
				synchronized (lock)
				{
					res = new ArrayList<>(temp);
				}
				results.values = res;
				results.count = res.size();
			}
			else
			{
				String prefixString = prefix.toString().toUpperCase().substring(idx + 1);

				current = idx == -1 ? "" : prefix.toString().substring(0, idx + 1);

				ArrayList<String> values;
				synchronized (lock)
				{
					values = new ArrayList<>(temp);
				}

				final int count = values.size();
				final ArrayList<String> newValues = new ArrayList<>();

				for (int i = 0; i < count; i++)
				{
					final String value = values.get(i);
					final String valueText = value.toUpperCase();

					if (valueText.startsWith(prefixString))
						newValues.add(value);
				}

				results.values = newValues;
				results.count = newValues.size();
			}

			return results;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results)
		{
			list = (ArrayList<String>)results.values;
			if (results.count > 0)
				notifyDataSetChanged();
			else
				notifyDataSetInvalidated();
		}
	}
}
