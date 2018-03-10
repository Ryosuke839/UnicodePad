/*
   Copyright 2018 Ryosuke839

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

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
