package jp.ddo.hotmist.unicodepad;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

public class LockableViewPager extends ViewPager
{
	private int slop;
	private float x;
	private float y;

	public LockableViewPager(Context context)
	{
		super(context);
		slop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	public LockableViewPager(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		slop = ViewConfiguration.get(context).getScaledTouchSlop();
	}

	@Override
	public boolean onTouchEvent(MotionEvent ev)
	{
		try
		{
			return super.onTouchEvent(ev);
		}
		catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
		}
		return false;
	}

	@Override
	public boolean onInterceptTouchEvent(MotionEvent event)
	{
		try
		{
			boolean inmove = super.onInterceptTouchEvent(event);
			switch (event.getActionMasked())
			{
			case MotionEvent.ACTION_DOWN:
				x = event.getX();
				y = event.getY();
				break;
			case MotionEvent.ACTION_MOVE:
				float dx = Math.abs(event.getX() - x);
				float dy = Math.abs(event.getY() - y);
				if (dx > slop && dx * .5 > dy)
					return true;
				break;
			}
			return inmove;
		}
		catch (IllegalArgumentException ex)
		{
			ex.printStackTrace();
		}
		return false;
	}
}
