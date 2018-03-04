package jp.ddo.hotmist.unicodepad;

import android.content.Context;
import android.preference.ListPreference;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

class ButtonListPreference extends ListPreference implements OnClickListener
{
	private View buttonup;
	private View buttondown;
	private OnClickListener listener;
	private boolean eup = true;
	private boolean edown = true;
	private Object tag;

	public ButtonListPreference(Context context)
	{
		super(context);

		setWidgetLayoutResource(R.layout.spinwidget);
		listener = null;
	}

	public ButtonListPreference(Context context, AttributeSet attrs)
	{
		super(context, attrs);

		setWidgetLayoutResource(R.layout.spinwidget);
		listener = null;
	}

	@Override
	protected View onCreateView(ViewGroup parent)
	{
		View layout = super.onCreateView(parent);
		layout.findViewById(R.id.buttonlist).setOnClickListener(this);
		buttonup = layout.findViewById(R.id.buttonup);
		buttonup.setOnClickListener(listener);
		buttonup.setEnabled(eup);
		buttonup.setClickable(eup);
		buttonup.setTag(tag);
		buttondown = layout.findViewById(R.id.buttondown);
		buttondown.setOnClickListener(listener);
		buttondown.setEnabled(edown);
		buttondown.setClickable(edown);
		buttondown.setTag(tag);
		return layout;
	}

	public void setOnClickListener(OnClickListener obj)
	{
		if (buttonup != null)
			buttonup.setOnClickListener(obj);
		if (buttondown != null)
			buttondown.setOnClickListener(obj);
		listener = obj;
	}

	public void setEnabled(boolean up, boolean down)
	{
		if (buttonup != null)
		{
			buttonup.setEnabled(up);
			buttonup.setClickable(up);
		}
		if (buttondown != null)
		{
			buttondown.setEnabled(down);
			buttondown.setClickable(down);
		}
		eup = up;
		edown = down;
	}

	public void setTag(Object t)
	{
		if (buttonup != null)
			buttonup.setTag(t);
		if (buttondown != null)
			buttondown.setTag(t);
		tag = t;
	}

	@Override
	public void onClick(View arg0)
	{
		super.onClick();
	}
}
