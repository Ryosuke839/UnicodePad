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

import android.app.Activity;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;

public class TabsActivity extends Activity
{
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		int[] themelist =
				{
						androidx.appcompat.R.style.Theme_AppCompat,
						androidx.appcompat.R.style.Theme_AppCompat_Light,
						androidx.appcompat.R.style.Theme_AppCompat_Light_DarkActionBar,
				};
		setTheme(themelist[Integer.valueOf(PreferenceManager.getDefaultSharedPreferences(this).getString("theme", "2131492983")) - 2131492983]);
		super.onCreate(savedInstanceState);

		DragSortListView view = new DragSortListView(this, null);
		DragSortController controller = new DragSortController(view, R.id.HANDLE_ID, DragSortController.ON_DRAG, DragSortController.FLING_REMOVE);
		controller.setSortEnabled(true);
		view.setFloatViewManager(controller);
		view.setOnTouchListener(controller);
		view.setAdapter(new TabsAdapter(this, view));
		setContentView(view);
	}
}
